package bio.terra.testrunner.runner.config;

import bio.terra.testrunner.runner.VersionScript;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;

@SuppressFBWarnings(
    value = "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD",
    justification = "This POJO class is used for easy serialization to JSON using Jackson.")
public class VersionScriptSpecification implements SpecificationInterface {
  public String name = "";
  public Map<String, String> parameters = new HashMap<>();
  public String description;

  public Class<? extends VersionScript> scriptClass;

  public static final String scriptsPackage = "scripts.versionscripts";

  VersionScriptSpecification() {}

  /**
   * Validate the version script specification read in from the JSON file. The name is converted
   * into a Java class reference
   */
  public void validate() {
    try {
      Class<?> scriptClassGeneric = Class.forName(scriptsPackage + "." + name);
      scriptClass = (Class<? extends VersionScript>) scriptClassGeneric;
    } catch (ClassNotFoundException | ClassCastException classEx) {
      throw new IllegalArgumentException("Version script class not found: " + name, classEx);
    }
  }
}
