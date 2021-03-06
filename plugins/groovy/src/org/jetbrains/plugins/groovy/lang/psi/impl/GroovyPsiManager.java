/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.groovy.lang.psi.impl;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.intellij.ProjectTopics;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.reference.SoftReference;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.containers.ConcurrentWeakHashMap;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.stubs.GroovyShortNamesCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ven
 */
public class GroovyPsiManager {
  private static final Logger LOG = Logger.getInstance("org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiManager");
  private static final Set<String> ourPopularClasses = Sets.newHashSet(GroovyCommonClassNames.GROOVY_LANG_CLOSURE,
                                                                       GroovyCommonClassNames.DEFAULT_BASE_CLASS_NAME,
                                                                       GroovyCommonClassNames.GROOVY_OBJECT_SUPPORT,
                                                                       CommonClassNames.JAVA_UTIL_LIST,
                                                                       CommonClassNames.JAVA_UTIL_COLLECTION,
                                                                       CommonClassNames.JAVA_LANG_STRING);
  private final Project myProject;

  private GrTypeDefinition myArrayClass;

  private final ConcurrentMap<GroovyPsiElement, PsiType> myCalculatedTypes = new ConcurrentWeakHashMap<GroovyPsiElement, PsiType>();
  private final ConcurrentMap<String, SoftReference<Map<GlobalSearchScope, PsiClass>>> myClassCache = new ConcurrentHashMap<String, SoftReference<Map<GlobalSearchScope, PsiClass>>>();
  private final GroovyShortNamesCache myCache;

  private final TypeInferenceHelper myTypeInferenceHelper;

  private static final String SYNTHETIC_CLASS_TEXT = "class __ARRAY__ { public int length }";

  public GroovyPsiManager(Project project) {
    myProject = project;
    myCache = ContainerUtil.findInstance(project.getExtensions(PsiShortNamesCache.EP_NAME), GroovyShortNamesCache.class);

    ((PsiManagerEx)PsiManager.getInstance(myProject)).registerRunnableToRunOnAnyChange(new Runnable() {
      public void run() {
        dropTypesCache();
      }
    });
    ((PsiManagerEx)PsiManager.getInstance(myProject)).registerRunnableToRunOnChange(new Runnable() {
      public void run() {
        myClassCache.clear();
      }
    });

    myTypeInferenceHelper = new TypeInferenceHelper(myProject);

    myProject.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      public void beforeRootsChange(ModuleRootEvent event) {
      }

      public void rootsChanged(ModuleRootEvent event) {
        dropTypesCache();
        myClassCache.clear();
      }
    });
  }

  public TypeInferenceHelper getTypeInferenceHelper() {
    return myTypeInferenceHelper;
  }

  public void dropTypesCache() {
    myCalculatedTypes.clear();
  }

  public static boolean isInheritorCached(@Nullable PsiType type, @NotNull String baseClassName) {
    if (type instanceof PsiClassType) {
      PsiClass resolve = ((PsiClassType)type).resolve();
      if (resolve != null) {
        return InheritanceUtil.isInheritorOrSelf(resolve, getInstance(resolve.getProject()).findClassWithCache(baseClassName, resolve.getResolveScope()), true);
      }
    }
    return false;
  }

  public static GroovyPsiManager getInstance(Project project) {
    return ServiceManager.getService(project, GroovyPsiManager.class);
  }

  public PsiClassType createTypeByFQClassName(String fqName, GlobalSearchScope resolveScope) {
    if (ourPopularClasses.contains(fqName)) {
      PsiClass result = findClassWithCache(fqName, resolveScope);
      if (result != null) {
        return JavaPsiFacade.getElementFactory(myProject).createType(result);
      }
    }

    return JavaPsiFacade.getElementFactory(myProject).createTypeByFQClassName(fqName, resolveScope);
  }

  @Nullable
  public PsiClass findClassWithCache(String fqName, GlobalSearchScope resolveScope) {
    SoftReference<Map<GlobalSearchScope, PsiClass>> reference = myClassCache.get(fqName);
    Map<GlobalSearchScope, PsiClass> map = reference == null ? null : reference.get();
    if (map == null) {
      map = new ConcurrentHashMap<GlobalSearchScope, PsiClass>();
      myClassCache.put(fqName, new SoftReference<Map<GlobalSearchScope, PsiClass>>(map));
    }
    PsiClass cached = map.get(resolveScope);
    if (cached != null) {
      return cached;
    }

    PsiClass result = JavaPsiFacade.getInstance(myProject).findClass(fqName, resolveScope);
    if (result != null) {
      map.put(resolveScope, result);
    }
    return result;
  }

  private final ThreadLocal<Multiset<GroovyPsiElement>> inferring = new ThreadLocal<Multiset<GroovyPsiElement>>(){
    @Override
    protected Multiset<GroovyPsiElement> initialValue() {
      return HashMultiset.create();
    }
  };

  private boolean lock(GroovyPsiElement element) {
    final Multiset<GroovyPsiElement> set = inferring.get();
    boolean alreadyContains = set.contains(element);
    inferring.get().add(element);
    return alreadyContains;
  }

  private void unlock(GroovyPsiElement element) {
    inferring.get().remove(element);
  }

  @Nullable
  public <T extends GroovyPsiElement> PsiType getType(T element, Function<T, PsiType> calculator) {
    PsiType type = myCalculatedTypes.get(element);
    if (type == null) {
      try {
        boolean locked = lock(element);
        type = calculator.fun(element);
        if (type == null) {
          type = PsiType.NULL;
        }
        if (locked) {
          final PsiType alreadyInferred = myCalculatedTypes.get(element);
          if (alreadyInferred != null) {
            type = alreadyInferred;
          }
        }
        else {
          type = ConcurrencyUtil.cacheOrGet(myCalculatedTypes, element, type);
        }
      }
      finally {
        unlock(element);
      }
    }
    if (!type.isValid()) {
      LOG.error("Type is invalid: " + type + "; element: " + element + " of class " + element.getClass());
    }
    return PsiType.NULL.equals(type) ? null : type;
  }

  public GrTypeDefinition getArrayClass() {
    if (myArrayClass == null) {
      try {
        myArrayClass = GroovyPsiElementFactory.getInstance(myProject).createTypeDefinition(SYNTHETIC_CLASS_TEXT);
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    return myArrayClass;
  }

  private static final ThreadLocal<List<PsiElement>> myElementsWithTypesBeingInferred = new ThreadLocal<List<PsiElement>>() {
    protected List<PsiElement> initialValue() {
      return new ArrayList<PsiElement>();
    }
  };

  @Nullable
  public static PsiType inferType(PsiElement element, Computable<PsiType> computable) {
    final List<PsiElement> curr = myElementsWithTypesBeingInferred.get();
    if (curr.size() > 7) { //don't end up walking the whole project PSI
      return null;
    }

    try {
      curr.add(element);
      return computable.compute();
    }
    finally {
      curr.remove(element);
    }
  }

  public static boolean isTypeBeingInferred(PsiElement element) {
    return myElementsWithTypesBeingInferred.get().contains(element);
  }

  public GroovyShortNamesCache getNamesCache() {
    return myCache;
  }
}
