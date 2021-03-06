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
package com.intellij.psi.util;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;

public interface PsiModificationTracker extends ModificationTracker {

  /**
   * This key can be passed as a dependency in a {@link com.intellij.psi.util.CachedValueProvider}.
   * @see #getModificationCount()
   */
  Key MODIFICATION_COUNT = Key.create("MODIFICATION_COUNT");

  /**
   * This key can be passed as a dependency in a {@link com.intellij.psi.util.CachedValueProvider}.
   * @see #getOutOfCodeBlockModificationCount()
   */
  Key OUT_OF_CODE_BLOCK_MODIFICATION_COUNT = Key.create("OUT_OF_CODE_BLOCK_MODIFICATION_COUNT");

  /**
   * This key can be passed as a dependency in a {@link com.intellij.psi.util.CachedValueProvider}.
   * @see #getJavaStructureModificationCount()
   */
  Key JAVA_STRUCTURE_MODIFICATION_COUNT = Key.create("JAVA_STRUCTURE_MODIFICATION_COUNT");

  /**
   * Tracks any PSI modification.
   * @return current counter value.
   */
  long getModificationCount();

  long getOutOfCodeBlockModificationCount();

  long getJavaStructureModificationCount();

  /**
   * Tracks modifications in Java annotations and annotated classes and methods.
   * @return current counter value.
   */
  long getAnnotationModificationCount();

  interface Listener {
    void modificationCountChanged();
  }
}
