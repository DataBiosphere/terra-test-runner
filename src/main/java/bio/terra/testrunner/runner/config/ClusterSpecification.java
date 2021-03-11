package bio.terra.testrunner.runner.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An instance of this class specifies a Kubernetes cluster that the Test Runner may manipulate
 * (e.g. kill pods, scale up/down). All Terra services do not need to be running in this cluster,
 * only the ones that we want to manipulate.
 */
public class ClusterSpecification implements SpecificationInterface {
  private static final Logger logger = LoggerFactory.getLogger(ClusterSpecification.class);

  public String clusterName;
  public String clusterShortName;
  public String region;
  public String zone;
  public String project;
  public String namespace;
  public String containerName;

  ClusterSpecification() {}

  /**
   * Validate the cluster specification read in from the JSON file. None of the properties should be
   * null, with the possible exception of the namespace.
   */
  public void validate() {
    if (clusterName == null || clusterName.equals("")) {
      throw new IllegalArgumentException("Server cluster name cannot be empty");
    } else if (clusterShortName == null || clusterShortName.equals("")) {
      throw new IllegalArgumentException("Server cluster short name cannot be empty");
    } else if (region == null || region.equals("")) {
      throw new IllegalArgumentException("Server cluster region cannot be empty");
    } else if (project == null || project.equals("")) {
      throw new IllegalArgumentException("Server cluster project cannot be empty");
    } else if (containerName == null || containerName.equals("")) {
      throw new IllegalArgumentException("Server cluster container name cannot be empty");
    } else if (StringUtils.isBlank(zone)) {
      logger.debug("Zone is required to obtain cluster spec for resiliency tests");
    }
  }
}
