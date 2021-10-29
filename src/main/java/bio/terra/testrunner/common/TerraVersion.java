package bio.terra.testrunner.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressFBWarnings(
    value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class TerraVersion {
  private String appName;
  private String appVersion;
  private String chartVersion;
  private static final String APPVERSION = "appVersion";
  private static final String CHARTVERSION = "chartVersion";

  public TerraVersion() {}

  public TerraVersion(String appName) {
    this.appName = appName;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public String getChartVersion() {
    return chartVersion;
  }

  public void setChartVersion(String chartVersion) {
    this.chartVersion = chartVersion;
  }

  public static List<TerraVersion> loadEnvVars() {
    // Load Terra service versions from envvars into summary
    Map<String, TerraVersion> versions = new HashMap<>();
    Map<String, String> envvars = System.getenv();
    envvars.keySet().stream()
        .filter(envName -> envName.contains(APPVERSION) || envName.contains(CHARTVERSION))
        .forEach(
            envName -> {
              String terraComponent = envName.split("_")[0];
              String versionTag = envName.split("_")[1];
              if (!versions.containsKey(terraComponent)) {
                versions.put(terraComponent, new TerraVersion(terraComponent));
              }
              if (versionTag.contains("appVersion")) {
                versions.get(terraComponent).setAppVersion(envvars.get(envName));
              } else if (versionTag.contains("chartVersion")) {
                versions.get(terraComponent).setChartVersion(envvars.get(envName));
              }
            });

    return versions.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
  }

  public static List<TerraVersion> loadEnvVars(String... appName) {
    return Arrays.stream(appName)
        .filter(
            name ->
                System.getenv(name + "_" + APPVERSION) != null
                    && System.getenv(name + "_" + CHARTVERSION) != null)
        .map(
            name -> {
              String appVersion = System.getenv(name + "_" + APPVERSION);
              String chartVersion = System.getenv(name + "_" + APPVERSION);
              TerraVersion version = new TerraVersion(name);
              version.setAppVersion(appVersion);
              version.setChartVersion(chartVersion);
              return version;
            })
        .collect(Collectors.toList());
  }
}
