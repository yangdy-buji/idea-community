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

package com.intellij.util.indexing;

import com.intellij.openapi.editor.Document;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ConcurrentWeakFactoryMap;

import java.util.Map;

/**
 * @author Dmitry Avdeev
 * @author peter
 */
public abstract class PerIndexDocumentMap<T> {

  private final Map<Document, Map<ID, T>> myVersions = new ConcurrentWeakFactoryMap<Document, Map<ID, T>>() {
    protected Map<ID, T> create(final Document document) {
      return new ConcurrentFactoryMap<ID,T>() {
        protected T create(ID key) {
          return createDefault(document);
        }
      };
    }
  };

  public T get(Document document, ID indexId) {
    return myVersions.get(document).get(indexId);
  }

  public void put(Document document, ID indexId, T value) {
    myVersions.get(document).put(indexId, value);
  }

  public synchronized T getAndSet(Document document, ID indexId, T value) {
    T old = get(document, indexId);
    put(document, indexId, value);
    return old;
  }

  public void clear() {
    myVersions.clear();
  }

  protected abstract T createDefault(Document document);
}
