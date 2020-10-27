package bio.terra.testrunner.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class EchoPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    EchoPluginExtension extension =
        project.getExtensions().create("echoext", EchoPluginExtension.class);

    project
        .task("echo")
        .doLast(
            task -> {
              System.out.println("ACK: " + extension.getIn());
            });
  }
}
