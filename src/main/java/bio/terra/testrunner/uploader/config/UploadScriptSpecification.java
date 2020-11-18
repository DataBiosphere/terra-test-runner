package bio.terra.testrunner.uploader.config;

import bio.terra.testrunner.runner.config.SpecificationInterface;
import bio.terra.testrunner.uploader.UploadScript;
import java.util.List;

public class UploadScriptSpecification implements SpecificationInterface {
  public String name;
  public String description;
  public List<String> parameters;

  private UploadScript scriptClassInstance;

  public static final String scriptsPackage = "scripts.uploadscripts";

  UploadScriptSpecification() {}

  public UploadScript scriptClassInstance() {
    return scriptClassInstance;
  }

  /**
   * Validate the upload script specification read in from the JSON file. The name is converted into
   * a Java class reference.
   */
  public void validate() {
    try {
      Class<?> scriptClassGeneric = Class.forName(scriptsPackage + "." + name);
      Class<? extends UploadScript> scriptClass =
          (Class<? extends UploadScript>) scriptClassGeneric;
      scriptClassInstance = scriptClass.newInstance();
    } catch (ClassNotFoundException | ClassCastException classEx) {
      throw new IllegalArgumentException("Upload script class not found: " + name, classEx);
    } catch (IllegalAccessException | InstantiationException niEx) {
      throw new IllegalArgumentException(
          "Error calling constructor of UploadScript class: " + name, niEx);
    }
  }
}
