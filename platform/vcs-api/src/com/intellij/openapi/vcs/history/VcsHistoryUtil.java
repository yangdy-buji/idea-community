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
package com.intellij.openapi.vcs.history;

import com.intellij.openapi.diff.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;

import java.io.IOException;
import java.nio.charset.Charset;

public class VcsHistoryUtil {

  private VcsHistoryUtil() {
  }

  public static int compare(VcsFileRevision first, VcsFileRevision second) {
    if (first instanceof CurrentRevision && second instanceof CurrentRevision) {
      return compareNumbers(first, second);
    }
    if (second instanceof CurrentRevision) return -1 * compare(second, first);

    if (first instanceof CurrentRevision) {
      int result = compareNumbers(first, second);
      if (result == 0) {
        return 1;
      }
      else {
        return result;
      }
    }
    else {
      return compareNumbers(first, second);
    }
  }

  private static int compareNumbers(VcsFileRevision first, VcsFileRevision second) {
    return first.getRevisionNumber().compareTo(second.getRevisionNumber());
  }

  /**
   * Invokes {@link com.intellij.openapi.diff.DiffManager#getDiffTool()} to show difference between the given revisions of the given file.
   * @param project   project under vcs control.
   * @param filePath  file which revisions are compared.
   * @param revision1 first revision - 'before', to the left.
   * @param revision2 second revision - 'after', to the right.
   * @throws com.intellij.openapi.vcs.VcsException
   * @throws java.io.IOException
   */
  public static void showDiff(Project project, FilePath filePath, VcsFileRevision revision1, VcsFileRevision revision2, String title1, String title2) throws VcsException, IOException {
    revision1.loadContent();
    final byte[] content1 = revision1.getContent();
    if (content1 == null) throw new VcsException("Failed to load content for revision " + revision1.getRevisionNumber().asString());

    revision2.loadContent();
    final byte[] content2 = revision2.getContent();
    if (content2 == null) throw new VcsException("Failed to load content for revision " + revision2.getRevisionNumber().asString());


    final SimpleDiffRequest diffData = new SimpleDiffRequest(project, filePath.getPresentableUrl());
    diffData.addHint(DiffTool.HINT_SHOW_FRAME);
    final Document doc = filePath.getDocument();
    final Charset charset = filePath.getCharset();
    final FileType fileType = filePath.getFileType();
    diffData.setContentTitles(title1, title2);
    diffData.setContents(createContent(project, content1, revision1, doc, charset, fileType),
                         createContent(project, content2, revision2, doc, charset, fileType));
    DiffManager.getInstance().getDiffTool().show(diffData);
  }

  private static DiffContent createContent(Project project, byte[] content1, VcsFileRevision revision, Document doc, Charset charset, FileType fileType) {
    if (isCurrent(revision) && (doc != null)) { return new DocumentContent(project, doc); }
    return new BinaryContent(content1, charset, fileType);
  }

  private static boolean isCurrent(VcsFileRevision revision) {
    return revision instanceof CurrentRevision;
  }

}
