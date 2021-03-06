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
package com.intellij.execution.ui.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

public class CloseAction extends AnAction implements DumbAware {
  private RunContentDescriptor myContentDescriptor;
  private final Project myProject;
  private Executor myExecutor;

  public CloseAction(Executor executor, RunContentDescriptor contentDescriptor, Project project) {
    myExecutor = executor;
    myContentDescriptor = contentDescriptor;
    myProject = project;
    copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE));
    final Presentation templatePresentation = getTemplatePresentation();
    templatePresentation.setIcon(IconLoader.getIcon("/actions/cancel.png"));
    templatePresentation.setText(ExecutionBundle.message("close.tab.action.name"));
    templatePresentation.setDescription(null);
  }

  public void actionPerformed(AnActionEvent e) {
    if (myContentDescriptor == null) {
      return;
    }
    final boolean removedOk = ExecutionManager.getInstance(myProject).getContentManager().removeRunContent(myExecutor, myContentDescriptor);
    if (removedOk) {
      myContentDescriptor = null;
      myExecutor = null;
    }
  }

  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(myContentDescriptor != null);
  }
}