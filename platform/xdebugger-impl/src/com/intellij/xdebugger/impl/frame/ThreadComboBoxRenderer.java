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
package com.intellij.xdebugger.impl.frame;

import com.intellij.xdebugger.frame.XExecutionStack;

import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class ThreadComboBoxRenderer extends BasicComboBoxRenderer {
  public Component getListCellRendererComponent(final JList list,
                                                final Object value,
                                                final int index,
                                                final boolean isSelected,
                                                final boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value != null) {
      XExecutionStack executionStack = (XExecutionStack)value;
      setText(executionStack.getDisplayName());
      setIcon(executionStack.getIcon());
    }
    return this;
  }
}
