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
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.util.io.FileUtil;

import java.util.ArrayList;

/**
 * @author yole
 */
public class ExcludeCompilerOutputPolicy implements DirectoryIndexExcludePolicy {
  private final Project myProject;

  public ExcludeCompilerOutputPolicy(final Project project) {
    myProject = project;
  }

  public boolean isExcludeRoot(final VirtualFile f) {
    CompilerProjectExtension compilerProjectExtension = CompilerProjectExtension.getInstance(myProject);
    if (isEqualWithFileOrUrl(f, compilerProjectExtension.getCompilerOutput(), compilerProjectExtension.getCompilerOutputUrl())) return true;

    for (Module m : ModuleManager.getInstance(myProject).getModules()) {
      CompilerModuleExtension rm = CompilerModuleExtension.getInstance(m);
      if (isEqualWithFileOrUrl(f, rm.getCompilerOutputPath(), rm.getCompilerOutputUrl())) return true;
      if (isEqualWithFileOrUrl(f, rm.getCompilerOutputPathForTests(), rm.getCompilerOutputUrlForTests())) return true;
    }
    return false;
  }

  public boolean isExcludeRootForModule(final Module module, final VirtualFile excludeRoot) {
    final CompilerModuleExtension compilerModuleExtension = CompilerModuleExtension.getInstance(module);
    return compilerModuleExtension.getCompilerOutputPath() == excludeRoot || compilerModuleExtension.getCompilerOutputPathForTests() == excludeRoot;
  }

  public VirtualFile[] getExcludeRootsForProject() {
    VirtualFile outputPath = CompilerProjectExtension.getInstance(myProject).getCompilerOutput();
    if (outputPath != null) {
      return new VirtualFile[] { outputPath };
    }
    return VirtualFile.EMPTY_ARRAY;
  }

  public VirtualFilePointer[] getExcludeRootsForModule(final ModuleRootModel rootModel) {
    ArrayList<VirtualFilePointer> result = new ArrayList<VirtualFilePointer>();
    final CompilerModuleExtension extension = rootModel.getModuleExtension(CompilerModuleExtension.class);
    if (extension == null) {
      return VirtualFilePointer.EMPTY_ARRAY;
    }
    if (extension.isCompilerOutputPathInherited()) {
      result.add(CompilerProjectExtension.getInstance(myProject).getCompilerOutputPointer());
    }
    else {
      if (!extension.isExcludeOutput()) return VirtualFilePointer.EMPTY_ARRAY;
      final VirtualFilePointer outputPath = extension.getCompilerOutputPointer();
      if (outputPath != null) result.add(outputPath);
      final VirtualFilePointer outputPathForTests = extension.getCompilerOutputForTestsPointer();
      if (outputPathForTests != null) result.add(outputPathForTests);
    }
    return result.isEmpty() ? VirtualFilePointer.EMPTY_ARRAY : result.toArray(new VirtualFilePointer[result.size()]);
  }

  private static boolean isEqualWithFileOrUrl(VirtualFile f, VirtualFile fileToCompareWith, String url) {
    if (fileToCompareWith != null) {
      if (fileToCompareWith == f) return true;
    }
    else if (url != null) {
      if (FileUtil.pathsEqual(url, f.getUrl())) return true;
    }
    return false;
  }
}
