package bio.terra.testrunner.configuration;

import java.util.HashMap;

public class LocalConfiguration {
  /** A map of valid names for data repo instances to their urls */
  private HashMap<String, String> envs;

  public HashMap<String, String> getInstances() {
    return envs;
  }

  public void setInstances(HashMap<String, String> envs) {
    this.envs = envs;
  }
}
