package com.github.simiacryptus.aicoder;

import com.intellij.openapi.project.Project;

public class ProjectManagerListener implements com.intellij.openapi.project.ProjectManagerListener {

  @Override
  public void projectOpened(Project project) {
    //assert System.getenv("CI") != null : "Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.";
  }
}