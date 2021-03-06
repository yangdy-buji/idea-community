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
package com.intellij.idea;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.PluginsFacade;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nullable;

public class IdeaTestApplication extends CommandLineApplication implements Disposable {
  private DataProvider myDataContext;

  private IdeaTestApplication() {
    super(true, true, true);

    PluginsFacade.INSTANCE = new PluginsFacade() {
      public IdeaPluginDescriptor getPlugin(PluginId id) {
        return PluginManager.getPlugin(id);
      }

      public IdeaPluginDescriptor[] getPlugins() {
        return PluginManager.getPlugins();
      }
    };
  }

  public void setDataProvider(DataProvider dataContext) {
    myDataContext = dataContext;
  }

  public Object getData(String dataId) {
    return myDataContext == null ? null : myDataContext.getData(dataId);
  }

  public static synchronized IdeaTestApplication getInstance(@Nullable final String configPath) {
    if (ourInstance == null) {
      new IdeaTestApplication();
      PluginsFacade.INSTANCE.getPlugins(); //initialization
      final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
      new WriteAction() {
        protected void run(Result result) throws Throwable {
          app.load(configPath);
        }
      }.execute();
    }
    return (IdeaTestApplication)ourInstance;
  }

  public static boolean isInitialized() {
    return ourInstance != null;
  }

  @Override
  public void dispose() {
    if (ourInstance == null) return;
    ApplicationEx applicationEx = ApplicationManagerEx.getApplicationEx();
    if (applicationEx != null) {
      Disposer.dispose(applicationEx);
      ApplicationManagerEx.setApplication(null);
    }
    ourInstance = null;
  }
}
