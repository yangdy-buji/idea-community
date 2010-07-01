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
package com.intellij.openapi.roots;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import org.jetbrains.annotations.NonNls;

import java.util.*;

/**
 * Root types that can be queried from OrderEntry.
 * @see OrderEntry
 * @author dsl
 */
public class OrderRootType {
  private final String myName;
  private static boolean ourExtensionsLoaded = false;

  public static final ExtensionPointName<OrderRootType> EP_NAME = ExtensionPointName.create("com.intellij.orderRootType");

  protected static PersistentOrderRootType[] ourPersistentOrderRootTypes = new PersistentOrderRootType[0];

  protected OrderRootType(@NonNls String name) {
    myName = name;
  }

  /**
   * Runtime classpath.
   * Includes:
   * <li>  production and test output for modules
   * <li>  classes roots for libraries and jdk unless scope is 'provided'
   * <li>  recursively for module dependencies with scope != 'provided'
   *
   * @deprecated
   * <ul>
   *   <li> for libraries and jdk use {@link #CLASSES}
   *   <li> to get module output roots use {@link CompilerModuleExtension#getOutputRoots}<code>(true)</code>
   *   <li> to recursively process module dependencies use <code>OrderEnumerator.orderEntries(module).compileOnly().recursively()</code>
   * </ul>
   */
  public static final OrderRootType CLASSES_AND_OUTPUT = new OrderRootType("CLASSES_AND_OUTPUT");

  /**
   * Classpath for compilation.
   * Includes:
   * <li>  production and test output for modules
   * <li>  classes roots for libraries and jdk
   * <li>  recursively for module dependencies: only exported items

   * @deprecated
   * <ul>
   *   <li> for libraries and jdk use {@link #CLASSES}
   *   <li> to get module output roots use {@link CompilerModuleExtension#getOutputRoots}<code>(true)</code>
   *   <li> to recursively process module dependencies use <code>OrderEnumerator.orderEntries(module).recursively().exportedOnly()</code>
   * </ul>
   */
  public static final OrderRootType COMPILATION_CLASSES = new OrderRootType("COMPILATION_CLASSES");

  /**
   * Classpath for compilation without tests.
   * Includes:
   * <li>  production output for modules
   * <li>  classes roots for libraries and jdk with scope suitable for production compile
   * <li>  recursively for module dependencies with scope suitable for production compile: only exported items

   * @deprecated
   * <ul>
   *   <li> for libraries and jdk use {@link #CLASSES}
   *   <li> to get module output root use {@link CompilerModuleExtension#getCompilerOutputPath()}
   *   <li> to recursively process module dependencies use <code>OrderEnumerator.orderEntries(module).compileOnly().productionOnly().recursively().exportedOnly()</code>
   * </ul>
   */
  public static final OrderRootType PRODUCTION_COMPILATION_CLASSES = new OrderRootType("PRODUCTION_COMPILATION_CLASSES");

  /**
   * Classpath without output directories for this module.
   * Includes:
   * <li>  classes roots for libraries and jdk
   * <li>  recursively for module dependencies: only exported items
   */
  public static final OrderRootType CLASSES = new PersistentOrderRootType("CLASSES", "classPath", null, "classPathEntry");

  /**
   * Sources.
   * Includes:
   * <li>  production and test source roots for modules
   * <li>  source roots for libraries and jdk
   * <li>  recursively for module dependencies: only exported items
   */
  public static final OrderRootType SOURCES = new PersistentOrderRootType("SOURCES", "sourcePath", null, "sourcePathEntry");

  public String name() {
    return myName;
  }

  /**
   * Whether this root type should be skipped when writing a Library if the root type doesn't contain
   * any roots.
   *
   * @return true if empty root type should be skipped, false otherwise.
   */
  public boolean skipWriteIfEmpty() {
    return false;
  }

  /**
   * Whether ModuleOrderEntry.getPaths() collects the list of roots from dependent modules.
   */
  public boolean collectFromDependentModules() {
    return false;
  }

  public static synchronized OrderRootType[] getAllTypes() {
    return getAllPersistentTypes();
  }

  public static PersistentOrderRootType[] getAllPersistentTypes() {
    if (!ourExtensionsLoaded) {
      ourExtensionsLoaded = true;
      Extensions.getExtensions(EP_NAME);
    }
    return ourPersistentOrderRootTypes;
  }

  public static List<PersistentOrderRootType> getSortedRootTypes() {
    List<PersistentOrderRootType> allTypes = new ArrayList<PersistentOrderRootType>();
    Collections.addAll(allTypes, getAllPersistentTypes());
    Collections.sort(allTypes, new Comparator<PersistentOrderRootType>() {
      public int compare(final PersistentOrderRootType o1, final PersistentOrderRootType o2) {
        return o1.getSdkRootName().compareTo(o2.getSdkRootName());
      }
    });
    return allTypes;
  }

  protected static <T> T getOrderRootType(final Class<? extends T> orderRootTypeClass) {
    OrderRootType[] rootTypes = Extensions.getExtensions(EP_NAME);
    for(OrderRootType rootType: rootTypes) {
      if (orderRootTypeClass.isInstance(rootType)) {
        //noinspection unchecked
        return (T)rootType;
      }
    }
    assert false : "Root type "+orderRootTypeClass+" not found. All roots: "+ Arrays.asList(rootTypes);
    return null;
  }

  public final int hashCode() {
    return super.hashCode();
  }

  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return "Root " + name();
  }
}
