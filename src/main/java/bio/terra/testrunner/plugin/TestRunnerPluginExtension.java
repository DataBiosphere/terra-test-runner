package bio.terra.testrunner.plugin;

public class TestRunnerPluginExtension {
  private String targetEnv;
  private String targetSvc;
  private String projectId;
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
}
