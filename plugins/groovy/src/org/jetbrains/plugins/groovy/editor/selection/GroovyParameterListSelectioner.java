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
package org.jetbrains.plugins.groovy.editor.selection;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList;

import java.util.Collections;
import java.util.List;

/**
 * @author Maxim.Medvedev
 */
public class GroovyParameterListSelectioner extends GroovyBasicSelectioner {
  @Override
  public boolean canSelect(PsiElement e) {
    return e instanceof GrParameterList || e.getParent() instanceof GrParameterList;
  }

  @Override
  public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
    if (e.getParent() instanceof GrParameterList) e = e.getParent();
    if (!(e instanceof GrParameterList)) return Collections.emptyList();
    return Collections.singletonList(e.getTextRange());
  }
}
