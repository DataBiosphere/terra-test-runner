package bio.terra.testrunner.collector.config;

import bio.terra.testrunner.collector.MeasurementCollectionScript;
import bio.terra.testrunner.runner.config.SpecificationInterface;
import java.util.List;

public class MeasurementCollectionScriptSpecification implements SpecificationInterface {
  public String name;
  public String description;
  public List<String> parameters;
  public boolean saveRawDataPoints;

  private MeasurementCollectionScript scriptClassInstance;

  public static final String scriptsPackage = "scripts.measurementcollectionscripts";

  MeasurementCollectionScriptSpecification() {}

  public MeasurementCollectionScript scriptClassInstance() {
    return scriptClassInstance;
  }

  /**
   * Validate the measurement collection script specification read in from the JSON file. The name
   * is converted into a Java class reference.
   */
  public void validate() {
    try {
      Class<?> scriptClassGeneric = Class.forName(scriptsPackage + "." + name);
      Class<? extends MeasurementCollectionScript> scriptClass =
          (Class<? extends MeasurementCollectionScript>) scriptClassGeneric;
      scriptClassInstance = scriptClass.newInstance();
    } catch (ClassNotFoundException | ClassCastException classEx) {
      throw new IllegalArgumentException(
          "Measurement collection script class not found: " + name, classEx);
    } catch (IllegalAccessException | InstantiationException niEx) {
      throw new IllegalArgumentException(
          "Error calling constructor of MeasurementCollectionScript class: " + name, niEx);
    }
  }
}
