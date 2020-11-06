package bio.terra.testrunner.plugin;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public class TestRunnerTask extends DefaultTask {

  @Input
  @Optional
  @Option(description = "Target Test Environment", option = "targetEnv")
  private String targetEnv;

  @Input
  @Optional
  @Option(description = "Target Service", option = "targetSvc")
  private String targetSvc;

  @Input
  @Optional
  @Option(description = "GCP Project ID", option = "projectId")
  private String projectId;

  @Input
  @Optional
  @Option(description = "Kubernetes Namespace", option = "namespace")
  private String namespace;

  public String getTargetEnv() {
    return targetEnv;
  }

  public void setTargetEnv(String targetEnv) {
    this.targetEnv = targetEnv;
  }

  public String getTargetSvc() {
    return targetSvc;
  }

  public void setTargetSvc(String targetSvc) {
    this.targetSvc = targetSvc;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @TaskAction
  public void action() {
    TestRunnerPluginExtension extension =
        (TestRunnerPluginExtension) getProject().getExtensions().findByName("testrunner");

    // First, try get properties from Command Line.
    // Then search for missing properties in build.gradle.
    if (!tryGetProperties()) {
      System.out.println(
          "Searching for missing properties from extension properties in build.gradle.");
      if (StringUtils.isBlank(getTargetEnv())) setTargetEnv(extension.getTargetEnv());
      if (StringUtils.isBlank(getTargetSvc())) setTargetSvc(extension.getTargetSvc());
      if (StringUtils.isBlank(getProjectId())) setProjectId(extension.getProjectId());
      if (StringUtils.isBlank(getNamespace())) setNamespace(extension.getNamespace());
      tryGetProperties();
    }
  }

  private boolean tryGetProperties() {
    if (missing().length == 0) {
      System.out.println(
          String.format(
              "Received Command Line properties: targetEnv=%s, targetSvc=%s, projectId=%s, namespace=%s",
              targetEnv, targetSvc, projectId, namespace));
      return true;
    } else {
      usage(missing());
      return false;
    }
  }

  private String[] missing() {
    List<String> list = new ArrayList<String>();
    if (StringUtils.isBlank(getTargetEnv())) list.add("targetEnv");
    if (StringUtils.isBlank(getTargetSvc())) list.add("targetSvc");
    if (StringUtils.isBlank(getProjectId())) list.add("projectId");
    if (StringUtils.isBlank(getNamespace())) list.add("namespace");
    return list.toArray(new String[list.size()]);
  }

  private void usage(String... missing) {
    System.out.println(
        String.format(
            "Missing Command Line properties: %s.\n\nFor help, please enter the following command\n\n  ./gradlew help --task testrunner\n\nCommand Line properties override build.gradle extension properties.\n\n",
            String.join(", ", missing)));
  }
}
