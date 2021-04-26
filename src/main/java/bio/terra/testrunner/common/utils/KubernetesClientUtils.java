package bio.terra.testrunner.common.utils;

import bio.terra.testrunner.runner.config.ApplicationSpecification;
import bio.terra.testrunner.runner.config.ServerSpecification;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import okhttp3.Call;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KubernetesClientUtils {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesClientUtils.class);

  private static int maximumSecondsToWaitForReplicaSetSizeChange = 500;
  private static int secondsIntervalToPollReplicaSetSizeChange = 5;

  private static String componentLabel;

  private static String apiComponentLabel;

  private static String namespace;

  private static CoreV1Api kubernetesClientCoreObject;
  private static AppsV1Api kubernetesClientAppsObject;

  private static Map<String, Map<String, String>> componentVersions =
      new HashMap<String, Map<String, String>>();

  private KubernetesClientUtils() {}

  public static CoreV1Api getKubernetesClientCoreObject() {
    if (kubernetesClientCoreObject == null) {
      throw new UnsupportedOperationException(
          "Kubernetes client core object is not setup. Check the server configuration skipKubernetes property.");
    }
    return kubernetesClientCoreObject;
  }

  public static AppsV1Api getKubernetesClientAppsObject() {
    if (kubernetesClientAppsObject == null) {
      throw new UnsupportedOperationException(
          "Kubernetes client apps object is not setup. Check the server configuration skipKubernetes property.");
    }
    return kubernetesClientAppsObject;
  }

  /**
   * This method returns the component versions of all MCTerra Services as a map of maps.
   * The Component Version ConfigMap for the target namespace is the source of truth.
   *
   * The state of a deployed MCTerra Service can be uniquely identified by the set
   * of component version keys present in the map nested inside the top-level map.
   *
   */
  public static Map<String, Map<String, String>> getComponentVersions() {
    return componentVersions;
  }

  /**
   * Build the singleton Kubernetes client objects. This method should be called once at the
   * beginning of a test run, and then all subsequent fetches should use the getter methods instead.
   *
   * @param server the server specification that points to the relevant Kubernetes cluster
   * @deprecated use {@link #buildKubernetesClientObjectWithClientKey(ServerSpecification,
   *     ApplicationSpecification)} instead.
   */
  @Deprecated
  public static void buildKubernetesClientObject(ServerSpecification server) throws Exception {
    // call the fetchGKECredentials script that uses gcloud to generate the kubeconfig file
    logger.debug(
        "Calling the fetchGKECredentials script that uses gcloud to generate the kubeconfig file");
    List<String> scriptArgs = new ArrayList<>();
    scriptArgs.add("tools/fetchGKECredentials.sh");
    scriptArgs.add(server.cluster.clusterShortName);
    scriptArgs.add(server.cluster.region);
    scriptArgs.add(server.cluster.project);
    Process fetchCredentialsProc = ProcessUtils.executeCommand("sh", scriptArgs);
    List<String> cmdOutputLines = ProcessUtils.waitForTerminateAndReadStdout(fetchCredentialsProc);
    for (String cmdOutputLine : cmdOutputLines) {
      logger.debug(cmdOutputLine);
    }

    // path to kubeconfig file, that was just created/updated by gcloud get-credentials above
    String kubeConfigPath = System.getenv("HOME") + "/.kube/config";
    logger.debug("Kube config path: {}", kubeConfigPath);

    namespace = server.cluster.namespace;
    // load the kubeconfig object from the file
    InputStreamReader filereader =
        new InputStreamReader(new FileInputStream(kubeConfigPath), StandardCharsets.UTF_8);
    KubeConfig kubeConfig = KubeConfig.loadKubeConfig(filereader);

    // get a refreshed SA access token and its expiration time
    logger.debug("Getting a refreshed service account access token and its expiration time");
    GoogleCredentials applicationDefaultCredentials =
        AuthenticationUtils.getServiceAccountCredential(
            server.testRunnerServiceAccount, AuthenticationUtils.cloudPlatformScope);
    AccessToken accessToken = AuthenticationUtils.getAccessToken(applicationDefaultCredentials);
    Instant tokenExpiration = accessToken.getExpirationTime().toInstant();
    String expiryUTC = tokenExpiration.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

    // USERS: build list of one user, the SA
    LinkedHashMap<String, Object> authConfigSA = new LinkedHashMap<>();
    authConfigSA.put("access-token", accessToken.getTokenValue());
    authConfigSA.put("expiry", expiryUTC);

    LinkedHashMap<String, Object> authProviderSA = new LinkedHashMap<>();
    authProviderSA.put("name", "gcp");
    authProviderSA.put("config", authConfigSA);

    LinkedHashMap<String, Object> userSA = new LinkedHashMap<>();
    userSA.put("auth-provider", authProviderSA);

    LinkedHashMap<String, Object> userWrapperSA = new LinkedHashMap<>();
    userWrapperSA.put("name", server.cluster.clusterName);
    userWrapperSA.put("user", userSA);

    ArrayList<Object> usersList = new ArrayList<>();
    usersList.add(userWrapperSA);

    // CONTEXTS: build list of one context, the specified cluster
    LinkedHashMap<String, Object> context = new LinkedHashMap<>();
    context.put("cluster", server.cluster.clusterName);
    context.put(
        "user",
        server.cluster.clusterName); // when is the user ever different from the cluster name?

    LinkedHashMap<String, Object> contextWrapper = new LinkedHashMap<>();
    contextWrapper.put("name", server.cluster.clusterName);
    contextWrapper.put("context", context);

    ArrayList<Object> contextsList = new ArrayList<>();
    contextsList.add(contextWrapper);

    // CLUSTERS: use the cluster list read in from the kubeconfig file, because I can't figure out
    // how to get the certificate-authority-data and server address for the cluster via the Java
    // client library, only with gcloud
    ArrayList<Object> clusters = kubeConfig.getClusters();

    // build the config object, replacing the contexts and users lists from the kubeconfig file with
    // the ones constructed programmatically above
    kubeConfig = new KubeConfig(contextsList, clusters, usersList);
    kubeConfig.setContext(server.cluster.clusterName);

    // build the client object from the config
    logger.debug("Building the client objects from the config");
    ApiClient client = ClientBuilder.kubeconfig(kubeConfig).build();

    // set the global default client to the one created above because the CoreV1Api and AppsV1Api
    // constructors get the client object from the global configuration
    Configuration.setDefaultApiClient(client);

    kubernetesClientCoreObject = new CoreV1Api();
    kubernetesClientAppsObject = new AppsV1Api();
  }

  /**
   * An alternative method to build the singleton Kubernetes client objects without .kube/config.
   *
   * <p>Requires Test Runner Service Account that meets the following standards - Kubernetes Engine
   * Viewer
   *
   * <p>For control of namespaces, a Kubernetes Service Account granted with appropriate RBAC
   * priviledges is required.
   *
   * <p>For more details, please refer to
   * https://docs.google.com/document/d/1-fGZqtwEUVRMmfeZfUVrn2V3M2D_eonNLepuX0ve6nM/edit?usp=sharing
   *
   * <p>This method should be called once at the beginning of a test run, and then all subsequent
   * fetches should use the getter methods instead.
   *
   * @param server the server specification that points to the relevant Kubernetes cluster
   */
  public static void buildKubernetesClientObjectWithClientKey(
      ServerSpecification server, ApplicationSpecification application) throws Exception {
    namespace = server.cluster.namespace;
    componentLabel = application.componentLabel;
    apiComponentLabel = application.apiComponentLabel;
    // get a refreshed SA access token and its expiration time
    logger.debug("Getting a refreshed service account access token and its expiration time");
    GoogleCredentials testRunnerServiceAccountCredentials =
        AuthenticationUtils.getServiceAccountCredential(
            server.testRunnerServiceAccount, AuthenticationUtils.cloudPlatformScope);

    // Kubernetes Service Account credentials. Currently these credentials are rendered from vault.
    String clientKey =
        FileUtils.readFileToString(
            server.testRunnerK8SServiceAccount.clientKeyFile.getParentFile().toPath(),
            server.testRunnerK8SServiceAccount.clientKeyFile.getName());

    String token =
        FileUtils.readFileToString(
            server.testRunnerK8SServiceAccount.tokenFile.getParentFile().toPath(),
            server.testRunnerK8SServiceAccount.tokenFile.getName());

    LinkedHashMap<String, Object> userSA = new LinkedHashMap<>();
    userSA.put("client-key-data", clientKey);
    userSA.put("token", token);

    // Build the User object to be bind to k8s context.
    LinkedHashMap<String, Object> userWrapperSA = new LinkedHashMap<>();
    // The "name" key is simply used by the Kubernetes Context to identify
    // which user to fetch the credentials from. As long as it is referenced
    // correctly, its value can be set to anything. Here we adopt the following
    // descriptive naming convention:
    //
    // <clusterShortName>-<namespace>-testrunner-user
    String userCtxName =
        String.format(
            "%s-%s-testrunner-user", server.cluster.clusterShortName, server.cluster.namespace);
    userWrapperSA.put("name", userCtxName);
    userWrapperSA.put("user", userSA);
    ArrayList<Object> usersList = new ArrayList<>();
    usersList.add(userWrapperSA);

    // Build the Cluster object to be bind to k8s context.
    // Please refer to the sample Java code on the Google GKE API reference page:
    // https://cloud.google.com/kubernetes-engine/docs/reference/rest/v1/projects.zones.clusters/get
    Cluster clusterSpec = getClusterSpecification(testRunnerServiceAccountCredentials, server);
    LinkedHashMap<String, Object> cluster = new LinkedHashMap();
    cluster.put("certificate-authority-data", clientKey);
    cluster.put("server", String.format("https://%s", clusterSpec.getEndpoint()));
    LinkedHashMap<String, Object> clusterWrapper = new LinkedHashMap();
    // The "name" key is simply used by k8s context to identify
    // which cluster to fetch the cluster spec. As long as it is referenced
    // correctly in the context, its value can be set to anything.
    // Here we adopt the following descriptive naming convention:
    //
    // <clusterShortName>-<namespace>
    String clusterCtxName =
        String.format("%s-%s", server.cluster.clusterShortName, server.cluster.namespace);
    clusterWrapper.put("name", clusterCtxName);
    clusterWrapper.put("cluster", cluster);
    ArrayList<Object> clustersList = new ArrayList();
    clustersList.add(clusterWrapper);

    // CONTEXTS: build list of one context, the specified user and cluster
    LinkedHashMap<String, Object> context = new LinkedHashMap<>();
    context.put("cluster", clusterCtxName);
    context.put("user", userCtxName);
    context.put("namespace", namespace);

    // Assign a name for the context object. Here we use the following naming convention
    //
    // clusterName
    LinkedHashMap<String, Object> contextWrapper = new LinkedHashMap<>();
    contextWrapper.put("name", server.cluster.clusterName);
    contextWrapper.put("context", context);

    ArrayList<Object> contextsList = new ArrayList<>();
    contextsList.add(contextWrapper);

    // build the config object, replacing the contexts and users lists in the kubeconfig object
    // with the ones constructed programmatically above.
    KubeConfig kubeConfig = new KubeConfig(contextsList, clustersList, usersList);
    kubeConfig.setContext(server.cluster.clusterName);

    // build the client object from the config
    logger.debug("Building the client objects from the config");
    ApiClient client = ClientBuilder.kubeconfig(kubeConfig).build();

    // set the global default client to the one created above because the CoreV1Api and AppsV1Api
    // constructors get the client object from the global configuration
    Configuration.setDefaultApiClient(client);

    kubernetesClientCoreObject = new CoreV1Api();
    kubernetesClientAppsObject = new AppsV1Api();

    // Import MCTerra Component versions from ConfigMap and outputs JSON-ready format
    importComponentVersions();
  }

  /**
   * This method imports MCTerra Component versions deployed to the target Kubernetes namespace.
   *
   * <p>A ConfigMap is used to store the terra-helmfile versions manifest which serve as the ground
   * truth of MCTerra Component versions for all environments.
   *
   * <p>In order to import MCTerra Component versions from the Config Map, the Test Runner
   * Kubernetes SA must be granted the following RBAC Role.
   *
   * <p>- apiGroups: [""] resources: ["configmaps"] resourceNames: ["terra-component-version"]
   * verbs: ["get", "patch", "update"] . * For more information of RBAC Roles, please refer to the
   * following documentation https://kubernetes.io/docs/reference/access-authn-authz/rbac/
   *
   * <p>Note: The name of the Component Version Identification ConfigMap: terra-component-version is
   * built in to this process.
   */
  private static void importComponentVersions() {
    try {
      // Get the Terra Component Version ConfigMap for the namespace.
      V1ConfigMap config =
          getKubernetesClientCoreObject()
              .readNamespacedConfigMap("terra-component-version", namespace, null, null, null);
      Map<String, String> configMap = config.getData();
      componentVersions =
          configMap.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      entry -> entry.getKey().replace(".properties", ""),
                      entry -> // A unique state of MCTerra deployment can employ multiline
                          // key=value properties.
                          // The purpose of this code is to collect all these properties as a map
                          // for each and every MCTerra Component and return a map of map structure.
                          Arrays.stream(entry.getValue().split("\\R"))
                              .collect(
                                  Collectors.toMap(
                                      versionCfg -> versionCfg.split("=")[0],
                                      versionCfg -> versionCfg.split("=")[1]))));
    } catch (ApiException e) {
      logger.debug(e.getResponseBody());
    }
  }

  /**
   * This method fetches cluster metadata using the project Id, zone, and cluster name
   *
   * @param credentials contains the Bearer token of the Test Runner SA (Kubernetes View role must
   *     be granted to this SA)
   * @param server ServerSpecification object that contains project Id, name, and zone of the
   *     cluster
   */
  private static Cluster getClusterSpecification(
      GoogleCredentials credentials, ServerSpecification server)
      throws IOException, GeneralSecurityException {
    // Obtain Cluster Spec using Google Container Service API
    Container containerService = createContainerService(credentials);
    Container.Projects.Zones.Clusters.Get request =
        containerService
            .projects()
            .zones()
            .clusters()
            .get(server.cluster.project, server.cluster.zone, server.cluster.clusterShortName);
    Cluster cluster = request.execute();
    return cluster;
  }

  /**
   * This method obtains a Google Container Service object that can be used to request GKE cluster
   * information. Please refer to the sample Java code on the Google GKE API reference page:
   * https://cloud.google.com/kubernetes-engine/docs/reference/rest/v1/projects.zones.clusters/get
   *
   * @param credentials contains the Bearer token of the Test Runner SA (Kubernetes View role must
   *     be granted to this SA)
   */
  private static Container createContainerService(GoogleCredentials credentials)
      throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    HttpRequestInitializer httpRequestInitializer = new HttpCredentialsAdapter(credentials);
    return new Container.Builder(httpTransport, jsonFactory, httpRequestInitializer)
        .setApplicationName("Google-ContainerSample/0.1")
        .build();
  }

  /**
   * List all the pods in namespace defined in buildKubernetesClientObject by the server
   * specification, or in the whole cluster if the namespace is not specified (i.e. null or empty
   * string).
   *
   * @return list of Kubernetes pods
   */
  public static List<V1Pod> listPods() throws ApiException {
    V1PodList list;
    if (namespace == null || namespace.isEmpty()) {
      list =
          getKubernetesClientCoreObject()
              .listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
    } else {
      list =
          getKubernetesClientCoreObject()
              .listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
    }
    return list.getItems();
  }

  /**
   * List all the deployments in namespace defined in buildKubernetesClientObject by the server
   * specification, or in the whole cluster if the namespace is not specified (i.e. null or empty
   * string).
   *
   * @return list of Kubernetes deployments
   */
  public static List<V1Deployment> listDeployments() throws ApiException {
    V1DeploymentList list;
    if (namespace == null || namespace.isEmpty()) {
      list =
          getKubernetesClientAppsObject()
              .listDeploymentForAllNamespaces(null, null, null, null, null, null, null, null, null);
    } else {
      list =
          getKubernetesClientAppsObject()
              .listNamespacedDeployment(
                  namespace, null, null, null, null, null, null, null, null, null);
    }
    return list.getItems();
  }

  /**
   * Get the API deployment in the in the namespace defined in buildKubernetesClientObject by the
   * server specification, or in the whole cluster if the namespace is not specified (i.e. null or
   * empty string). This method expects that there is a single API deployment in the namespace.
   *
   * @return the API deployment, null if not found
   */
  public static V1Deployment getApiDeployment() throws ApiException {
    // loop through the deployments in the namespace
    // find the one that matches the api component label
    return getApiDeployment(componentLabel, apiComponentLabel);
  }

  /**
   * Get the API deployment in the in the namespace defined in buildKubernetesClientObject by the
   * server specification, or in the whole cluster if the namespace is not specified (i.e. null or
   * empty string). This method expects that there is a single API deployment in the namespace.
   *
   * @param componentLabel metadata label key for identifying a k8s deployment
   * @param apiComponentLabel the value associated with the metadata label key
   * @return the API deployment, null if not found
   */
  public static V1Deployment getApiDeployment(String componentLabel, String apiComponentLabel)
      throws ApiException {
    // loop through the deployments in the namespace
    // find the one that matches the api component label
    return listDeployments().stream()
        .filter(
            deployment ->
                deployment.getMetadata().getLabels().containsKey(componentLabel)
                    && deployment
                        .getMetadata()
                        .getLabels()
                        .get(componentLabel)
                        .equals(apiComponentLabel))
        .findFirst()
        .orElse(null);
  }

  /**
   * Change the size of the replica set. Note that this just sends a request to change the size, it
   * does not wait to make sure the size is actually updated.
   *
   * @param deployment the deployment object to modify
   * @param numberOfReplicas the new size of the replica set to scale to
   */
  public static V1Deployment changeReplicaSetSize(V1Deployment deployment, int numberOfReplicas)
      throws ApiException {
    V1DeploymentSpec existingSpec = deployment.getSpec();
    deployment.setSpec(existingSpec.replicas(numberOfReplicas));
    return getKubernetesClientAppsObject()
        .replaceNamespacedDeployment(
            deployment.getMetadata().getName(),
            deployment.getMetadata().getNamespace(),
            deployment,
            null,
            null,
            null);
  }

  /** Select any pod from api pods and delete pod. */
  public static void deleteRandomPod() throws ApiException, IOException {
    V1Deployment apiDeployment = KubernetesClientUtils.getApiDeployment();
    if (apiDeployment == null) {
      throw new RuntimeException("API deployment not found.");
    }
    long podCount = getApiPodCount(apiDeployment);
    logger.debug("Pod Count: {}; Message: Before deleting pods", podCount);
    printApiPods(apiDeployment);
    String deploymentComponentLabel = apiDeployment.getMetadata().getLabels().get(componentLabel);

    // select a random pod from list of apis
    String randomPodName;
    randomPodName =
        listPods().stream()
            .filter(
                pod ->
                    deploymentComponentLabel.equals(
                        pod.getMetadata().getLabels().get(componentLabel)))
            .skip(new Random().nextInt((int) podCount))
            .findFirst()
            .get()
            .getMetadata()
            .getName();

    logger.info("delete random pod: {}", randomPodName);

    deletePod(randomPodName);
  }

  public static void deletePod(String podNameToDelete) throws ApiException, IOException {
    // known issue with java api "deleteNamespacedPod()" endpoint
    // https://github.com/kubernetes-client/java/issues/252
    // the following few lines were suggested as a workaround
    // https://github.com/kubernetes-client/java/issues/86
    Call call =
        getKubernetesClientCoreObject()
            .deleteNamespacedPodCall(
                podNameToDelete, namespace, null, null, null, null, null, null, null);
    Response response = call.execute();
    Configuration.getDefaultApiClient()
        .handleResponse(response, (new TypeToken<V1Pod>() {}).getType());
  }

  /**
   * Wait until the size of the replica set matches the specified number of running pods. Times out
   * after {@link KubernetesClientUtils#maximumSecondsToWaitForReplicaSetSizeChange} seconds. Polls
   * in intervals of {@link KubernetesClientUtils#secondsIntervalToPollReplicaSetSizeChange}
   * seconds.
   *
   * @param deployment the deployment object to poll
   * @param numberOfReplicas the eventual expected size of the replica set
   */
  public static void waitForReplicaSetSizeChange(V1Deployment deployment, int numberOfReplicas)
      throws Exception {
    // set values so that while conditions always true on first try
    // forces the first sleep statement to be hit giving the pods a chance to start any adjustments
    long numPods = -1;
    long numRunningPods = -2;
    int pollCtr =
        Math.floorDiv(
            maximumSecondsToWaitForReplicaSetSizeChange, secondsIntervalToPollReplicaSetSizeChange);

    while ((numPods != numRunningPods || numPods != numberOfReplicas) && pollCtr >= 0) {
      TimeUnit.SECONDS.sleep(secondsIntervalToPollReplicaSetSizeChange);
      // two checks to make sure we are fully back in working order
      // 1 - does the total number of pods match the replica count (for example, all terminating
      // pods have finished terminating & no longer show up in list)
      numPods = getApiPodCount(deployment);
      // 2 - does the number of pods in the "ready" state matches the replica count
      numRunningPods = getApiReadyPods(deployment);
      pollCtr--;
    }

    if (numPods != numberOfReplicas) {
      throw new RuntimeException(
          "Timed out waiting for replica set size to change. (numPods="
              + numPods
              + ", numberOfReplicas="
              + numberOfReplicas
              + ")");
    }
  }

  /**
   * Utilizing the other util functions to (1) fresh fetch of the api deployment, (2) scale the
   * replica count, (3) wait for replica count to update, and (4) print the results
   *
   * @param podCount count of pods to scale the kubernetes deployment to
   */
  public static void changeReplicaSetSizeAndWait(int podCount) throws Exception {
    changeReplicaSetSizeAndWait(podCount, componentLabel, apiComponentLabel);
  }

  /**
   * Utilizing the other util functions to (1) fresh fetch of the api deployment, (2) scale the
   * replica count, (3) wait for replica count to update, and (4) print the results
   *
   * @param podCount count of pods to scale the kubernetes deployment to
   * @param componentLabel component label key for locating the kubernetes application component
   *     associated with the deployment
   * @param apiComponentLabel the corresponding value of the component label key
   */
  public static void changeReplicaSetSizeAndWait(
      int podCount, String componentLabel, String apiComponentLabel) throws Exception {
    V1Deployment apiDeployment = getApiDeployment(componentLabel, apiComponentLabel);
    if (apiDeployment == null) {
      throw new RuntimeException("API deployment not found.");
    }

    long apiPodCount = getApiPodCount(apiDeployment, componentLabel);
    logger.debug("Pod Count: {}; Message: Before scaling pod count", apiPodCount);
    apiDeployment = changeReplicaSetSize(apiDeployment, podCount);
    waitForReplicaSetSizeChange(apiDeployment, podCount);

    // print out the current pods
    apiPodCount = getApiPodCount(apiDeployment, componentLabel);
    logger.debug("Pod Count: {}; Message: After scaling pod count", apiPodCount);
    printApiPods(apiDeployment);
  }

  private static long getApiPodCount(V1Deployment deployment) throws ApiException {
    // loop through the pods in the namespace
    // find the ones that match the deployment component label (e.g. find all the API pods)
    return getApiPodCount(deployment, componentLabel);
  }

  private static long getApiPodCount(V1Deployment deployment, String componentLabel)
      throws ApiException {
    // loop through the pods in the namespace
    // find the ones that match the deployment component label (e.g. find all the API pods)
    long apiPodCount =
        listPods().stream()
            .filter(
                pod ->
                    deployment.getMetadata().getLabels().containsKey(componentLabel)
                        && pod.getMetadata().getLabels().containsKey(componentLabel)
                        && deployment
                            .getMetadata()
                            .getLabels()
                            .get(componentLabel)
                            .equals(pod.getMetadata().getLabels().get(componentLabel)))
            .count();
    return apiPodCount;
  }

  private static long getApiReadyPods(V1Deployment deployment) throws ApiException {
    String deploymentComponentLabel = deployment.getMetadata().getLabels().get(componentLabel);
    long apiPodCount =
        listPods().stream()
            .filter(
                pod ->
                    deploymentComponentLabel.equals(
                            pod.getMetadata().getLabels().get(componentLabel))
                        && pod.getStatus().getContainerStatuses().get(0).getReady())
            .count();
    return apiPodCount;
  }

  public static void printApiPods(V1Deployment deployment) throws ApiException {
    printApiPods(deployment, componentLabel);
  }

  public static void printApiPods(V1Deployment deployment, String componentLabel)
      throws ApiException {
    String deploymentComponentLabel = deployment.getMetadata().getLabels().get(componentLabel);
    listPods().stream()
        .filter(
            pod ->
                deploymentComponentLabel.equals(pod.getMetadata().getLabels().get(componentLabel)))
        .forEach(p -> logger.debug("Pod: {}", p.getMetadata().getName()));
  }

  public static String getComponentLabel() {
    return componentLabel;
  }

  public static String getApiComponentLabel() {
    return apiComponentLabel;
  }
}
