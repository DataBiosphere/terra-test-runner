package bio.terra.testrunner.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TestRunnerPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    TestRunnerPluginExtension extension =
        project.getExtensions().create("testrunner", TestRunnerPluginExtension.class);
    TestRunnerTask testRunnerTask = project.getTasks().create("testrunner", TestRunnerTask.class);
  }
}
