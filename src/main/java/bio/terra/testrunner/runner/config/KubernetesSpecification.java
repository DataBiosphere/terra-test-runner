package bio.terra.testrunner.runner.config;

import org.graalvm.compiler.core.common.SuppressFBWarnings;

public class KubernetesSpecification implements SpecificationInterface {
  @SuppressFBWarnings(value = "UWF_NULL_FIELD", justification = "must be >=0 or unspecified.")
  public Integer numberOfInitialPods = null;

  KubernetesSpecification() {}

  /** Validate the Kubernetes specification read in from the JSON file. */
  public void validate() {
    if (numberOfInitialPods != null && numberOfInitialPods <= 0) {
      throw new IllegalArgumentException("Number of initial Kubernetes pods must be >= 0");
    }
  }
}
