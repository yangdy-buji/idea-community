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

package com.intellij.execution.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author spleaner
 */
public class UnknownRunConfiguration implements RunConfiguration {
  private final ConfigurationFactory myFactory;
  private Element myStoredElement;
  private String myName;
  private final Project myProject;

  private static final AtomicInteger myUniqueName = new AtomicInteger(1);
  private boolean myDoNotStore;

  public UnknownRunConfiguration(@NotNull final ConfigurationFactory factory, @NotNull final Project project) {
    myFactory = factory;
    myProject = project;
  }

  public void setDoNotStore(boolean b) {
    myDoNotStore = b;
  }

  @Nullable
  public Icon getIcon() {
    return null;
  }

  public boolean isDoNotStore() {
    return myDoNotStore;
  }

  public ConfigurationFactory getFactory() {
    return myFactory;
  }

  public void setName(final String name) {
    myName = name;
  }

  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new UnknownSettingsEditor();
  }

  public Project getProject() {
    return myProject;
  }

  @NotNull
  public ConfigurationType getType() {
    return UnknownConfigurationType.INSTANCE;
  }

  public JDOMExternalizable createRunnerSettings(final ConfigurationInfoProvider provider) {
    return null;
  }

  public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(final ProgramRunner runner) {
    return null;
  }

  public RunConfiguration clone() {
    try {
      final UnknownRunConfiguration cloned = (UnknownRunConfiguration) super.clone();
      return cloned;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }


  public int getUniqueID() {
    return System.identityHashCode(this);
  }

  public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
    return null;
  }

  public String getName() {
    if (myName == null) {
      myName = String.format("Unknown%s", myUniqueName.getAndAdd(1));
    }

    return myName;
  }

  public void checkConfiguration() throws RuntimeConfigurationException {
    throw new RuntimeConfigurationException("Broken configuration due to unavailable plugin or invalid configuration data.");
  }

  public void readExternal(final Element element) throws InvalidDataException {
    myStoredElement = (Element) element.clone();
  }

  public void writeExternal(final Element element) throws WriteExternalException {
    if (myStoredElement != null) {
      final List attributeList = myStoredElement.getAttributes();
      for (Object anAttributeList : attributeList) {
        final Attribute a = (Attribute) anAttributeList;
        element.setAttribute(a.getName(), a.getValue());
      }

      final List list = myStoredElement.getChildren();
      for (Object child : list) {
        final Element c = (Element) child;
        element.addContent((Element) c.clone());
      }
    }
  }

  private static class UnknownSettingsEditor extends SettingsEditor<UnknownRunConfiguration> {
    private final JPanel myPanel;

    private UnknownSettingsEditor() {
      myPanel = new JPanel();
      myPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));

      myPanel.add(new JLabel("This configuration can not be edited", JLabel.CENTER));
    }

    protected void resetEditorFrom(final UnknownRunConfiguration s) {
    }

    protected void applyEditorTo(final UnknownRunConfiguration s) throws ConfigurationException {
    }

    @NotNull
    protected JComponent createEditor() {
      return myPanel;
    }

    protected void disposeEditor() {
    }
  }
}
