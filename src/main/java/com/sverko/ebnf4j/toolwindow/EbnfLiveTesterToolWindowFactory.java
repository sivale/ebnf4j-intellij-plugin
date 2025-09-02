package com.sverko.ebnf4j.toolwindow;

import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class EbnfLiveTesterToolWindowFactory implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    EbnfLiveTesterPanel testerPanel = new EbnfLiveTesterPanel(project);
    Content content = ContentFactory.getInstance().createContent(testerPanel.getComponent(), "", false);
    toolWindow.getContentManager().addContent(content);
  }
}
