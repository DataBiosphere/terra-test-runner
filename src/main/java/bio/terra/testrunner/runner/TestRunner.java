package bio.terra.testrunner.runner;

import static bio.terra.testrunner.common.commands.PrintHelp.ANSI_PURPLE;
import static bio.terra.testrunner.common.commands.PrintHelp.ANSI_RESET;

import bio.terra.testrunner.common.TerraVersion;
import bio.terra.testrunner.common.utils.FileUtils;
import bio.terra.testrunner.common.utils.KubernetesClientUtils;
import bio.terra.testrunner.runner.config.TestConfiguration;
import bio.terra.testrunner.runner.config.TestScriptSpecification;
import bio.terra.testrunner.runner.config.TestSuite;
import bio.terra.testrunner.runner.config.TestUserSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner {
  private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

  private TestConfiguration config;
  private List<TestScript> scripts;
  private DeploymentScript deploymentScript;
  private List<ThreadPoolExecutor> threadPools;
  private ThreadPoolExecutor disruptionThreadPool;
  private List<List<Future<UserJourneyResult>>> userJourneyFutureLists;

  // test run outputs
  private List<TestScriptResult> testScriptResults;
  protected TestRunSummary summary;
  protected TestRunFullOutput runFullOutput;

  private static long secondsToWaitForPoolShutdown = 60;

  private static Map<String, Map<String, String>> componentVersions =
      new HashMap<String, Map<String, String>>();

  private boolean exceptionThrownInCleanup = false;

  protected TestRunner(TestConfiguration config) {
    this.config = config;
    this.scripts = new ArrayList<>();
    this.threadPools = new ArrayList<>();
    this.disruptionThreadPool = null;
    this.userJourneyFutureLists = new ArrayList<>();
    this.testScriptResults = new ArrayList<>();
    this.runFullOutput = new TestRunFullOutput(UUID.randomUUID().toString());
    runFullOutput.terraVersions = TerraVersion.loadEnvVars();
    this.summary = (TestRunSummary) this.runFullOutput;
  }

  protected void executeTestConfiguration() throws Exception {
    try {
      // set the start time for this test run
      summary.startTime = System.currentTimeMillis();

      executeTestConfigurationNoGuaranteedCleanup();

      // set the end time for this test run
      summary.endTime = System.currentTimeMillis();
    } catch (Exception originalEx) {
      // cleanup deployment (i.e. run teardown method)
      try {
        if (!config.server.skipDeployment) {
          logger.info(
              "Deployment: Calling {}.teardown() after failure",
              deploymentScript.getClass().getName());
          deploymentScript.teardown();
        }
      } catch (Exception deploymentTeardownEx) {
        logger.error(
            "Deployment: Exception during forced deployment teardown", deploymentTeardownEx);
      }

      // cleanup test scripts (i.e. run cleanup methods)
      if (exceptionThrownInCleanup) {
        logger.info(
            "Test Script: Exception thrown in cleanup methods, so not re-trying during cleanup");
      } else {
        logger.info("Test Scripts: Calling the cleanup methods after failure");
        try {
          callTestScriptCleanups();
        } catch (Exception testScriptCleanupEx) {
          logger.error(
              "Test Scripts: Exception during forced test script cleanups", testScriptCleanupEx);
        }
      }

      throw originalEx;
    }
  }

  private void executeTestConfigurationNoGuaranteedCleanup() throws Exception {
    // specify any value overrides in the Helm chart, then deploy
    if (!config.server.skipDeployment) {
      // get an instance of the deployment script class
      try {
        deploymentScript = config.server.deploymentScript.scriptClass.newInstance();
      } catch (IllegalAccessException | InstantiationException niEx) {
        logger.error(
            "Deployment: Error calling constructor of DeploymentScript class: {}",
            config.server.deploymentScript.name,
            niEx);
        throw new IllegalArgumentException(
            "Error calling constructor of DeploymentScript class: "
                + config.server.deploymentScript.name,
            niEx);
      }

      // set any parameters specified by the configuration
      deploymentScript.setParameters(config.server.deploymentScript.parameters);

      // call the deploy and waitForDeployToFinish methods to do the deployment
      logger.info("Deployment: Calling {}.deploy()", deploymentScript.getClass().getName());
      deploymentScript.deploy(config.server, config.application);

      logger.info(
          "Deployment: Calling {}.waitForDeployToFinish()", deploymentScript.getClass().getName());
      deploymentScript.waitForDeployToFinish();
    } else {
      logger.info("Deployment: Skipping deployment");
    }

    // update any Kubernetes properties specified by the test configuration
    if (!config.server.skipKubernetes) {
      KubernetesClientUtils.buildKubernetesClientObjectWithClientKey(config.server);
      componentVersions = KubernetesClientUtils.importComponentVersions();
      modifyKubernetesPostDeployment();
    } else {
      logger.info("Kubernetes: Skipping Kubernetes configuration post-deployment");
    }

    // setup the instance of each test script class
    logger.info(
        "Test Scripts: Fetching instance of each class, setting billing account and parameters");
    for (TestScriptSpecification testScriptSpecification : config.testScripts) {
      TestScript testScriptInstance = testScriptSpecification.scriptClassInstance();

      // set the billing account for the test script to use
      testScriptInstance.setBillingAccount(config.billingAccount);

      // set the server specification for the test script to run against
      testScriptInstance.setServer(config.server);

      // set any parameters specified by the configuration
      testScriptInstance.setParameters(testScriptSpecification.parameters);

      scripts.add(testScriptInstance);
    }

    // call the setup method of each test script
    logger.info("Test Scripts: Calling the setup methods");
    Throwable setupExceptionThrown = callTestScriptSetups();
    if (setupExceptionThrown != null) {
      logger.error("Test Scripts: Error calling test script setup methods", setupExceptionThrown);
      throw new RuntimeException("Error calling test script setup methods.", setupExceptionThrown);
    }

    // Disruptive Thread: fetch script specification if config is defined
    if (config.disruptiveScript != null) {
      logger.debug("Creating thread pool for disruptive script.");
      DisruptiveScript disruptiveScriptInstance =
          config.disruptiveScript.disruptiveScriptClassInstance();
      disruptiveScriptInstance.setBillingAccount(config.billingAccount);
      disruptiveScriptInstance.setServer(config.server);
      disruptiveScriptInstance.setParameters(config.disruptiveScript.parameters);

      // create a thread pool for running its disrupt method
      disruptionThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

      DisruptiveThread disruptiveThread =
          new DisruptiveThread(disruptiveScriptInstance, config.testUsers);
      disruptionThreadPool.execute(disruptiveThread);
      logger.debug("Successfully submitted disruptive thread.");
      TimeUnit.SECONDS.sleep(
          1); // give the disruptive script thread some time to start before kicking off the user
      // journeys.
    }

    // set the start time for the user journey portion this test run
    summary.startUserJourneyTime = System.currentTimeMillis();

    // for each test script
    logger.info(
        "Test Scripts: Creating a thread pool for each TestScript and kicking off the user journeys");
    for (int tsCtr = 0; tsCtr < scripts.size(); tsCtr++) {
      TestScript testScript = scripts.get(tsCtr);
      TestScriptSpecification testScriptSpecification = config.testScripts.get(tsCtr);

      // create a thread pool for running its user journeys
      ThreadPoolExecutor threadPool =
          (ThreadPoolExecutor)
              Executors.newFixedThreadPool(testScriptSpecification.userJourneyThreadPoolSize);
      threadPools.add(threadPool);

      // kick off the user journey(s), one per thread
      List<Future<UserJourneyResult>> userJourneyFutures = new ArrayList<>();
      for (int ujCtr = 0;
          ujCtr < testScriptSpecification.numberOfUserJourneyThreadsToRun;
          ujCtr++) {
        TestUserSpecification testUser =
            config.testUsers.isEmpty()
                ? null
                : config.testUsers.get(ujCtr % config.testUsers.size());
        // add a description to the user journey threads/results that includes any test script
        // parameters
        Future<UserJourneyResult> userJourneyFuture =
            threadPool.submit(
                new UserJourneyThread(testScript, testScriptSpecification.description, testUser));
        userJourneyFutures.add(userJourneyFuture);
      }

      // TODO: support different patterns of kicking off user journeys. here they're all queued at
      // once
      userJourneyFutureLists.add(userJourneyFutures);
    }

    // wait until all threads either finish or time out
    logger.info("Test Scripts: Waiting until all threads either finish or time out");
    for (int ctr = 0; ctr < scripts.size(); ctr++) {
      TestScriptSpecification testScriptSpecification = config.testScripts.get(ctr);
      ThreadPoolExecutor threadPool = threadPools.get(ctr);

      threadPool.shutdown();
      long totalTerminationTime =
          testScriptSpecification.expectedTimeForEach
              * testScriptSpecification.numberOfUserJourneyThreadsToRun;
      boolean terminatedByItself =
          threadPool.awaitTermination(
              totalTerminationTime, testScriptSpecification.expectedTimeForEachUnitObj);

      // if the threads didn't finish in the expected time, then send them interrupts
      if (!terminatedByItself) {
        threadPool.shutdownNow();
      }
      if (!threadPool.awaitTermination(secondsToWaitForPoolShutdown, TimeUnit.SECONDS)) {
        logger.error(
            "Test Scripts: Thread pool for test script failed to terminate: {}",
            testScriptSpecification.description);
      }
    }

    // set the end time for the user journey portion this test run
    summary.endUserJourneyTime = System.currentTimeMillis();

    // shutdown the disrupt thread pool
    if (disruptionThreadPool != null) {
      logger.debug("Tell the disruption thread pool to shutdown");
      disruptionThreadPool.shutdownNow();
      if (!disruptionThreadPool.awaitTermination(secondsToWaitForPoolShutdown, TimeUnit.SECONDS)) {
        logger.error("Disruption Script: Thread pool for disruption script failed to terminate");
      }
    }

    // compile the results from all thread pools
    logger.info("Test Scripts: Compiling the results from all thread pools");
    for (int ctr = 0; ctr < scripts.size(); ctr++) {
      List<Future<UserJourneyResult>> userJourneyFutureList = userJourneyFutureLists.get(ctr);
      TestScriptSpecification testScriptSpecification = config.testScripts.get(ctr);

      List<UserJourneyResult> userJourneyResults = new ArrayList<>();
      for (Future<UserJourneyResult> userJourneyFuture : userJourneyFutureList) {
        UserJourneyResult result = null;
        if (userJourneyFuture.isDone())
          try {
            // user journey thread completed and populated its own return object, which may include
            // an exception
            result = userJourneyFuture.get();
            result.completed = true;
          } catch (ExecutionException execEx) {
            // user journey thread threw an exception and didn't populate its own return object
            result = new UserJourneyResult(testScriptSpecification.name, "");
            result.completed = false;
            result.saveExceptionThrown(execEx);
          }
        else {
          // user journey either was never started or got cancelled before it finished
          result = new UserJourneyResult(testScriptSpecification.name, "");
          result.completed = false;
        }
        userJourneyResults.add(result);
      }
      testScriptResults.add(new TestScriptResult(testScriptSpecification, userJourneyResults));
    }

    // pull out the test script summary information into the summary object
    summary.testScriptResultSummaries =
        testScriptResults.stream().map(TestScriptResult::getSummary).collect(Collectors.toList());
    runFullOutput.testScriptResults = testScriptResults;

    // call the cleanup method of each test script
    logger.info("Test Scripts: Calling the cleanup methods");
    Throwable cleanupExceptionThrown = callTestScriptCleanups();
    if (cleanupExceptionThrown != null) {
      exceptionThrownInCleanup = true;
      logger.error(
          "Test Scripts: Error calling test script cleanup methods", cleanupExceptionThrown);
      throw new RuntimeException(
          "Error calling test script cleanup methods.", cleanupExceptionThrown);
    }

    // no need to restore any Kubernetes settings. they are always set again at the beginning of a
    // test run, which is more important from a reproducibility standpoint. probably more useful to
    // leave the deployment as is, for debugging after a test run
    if (!config.server.skipDeployment) {
      deploymentScript.teardown();
    } else {
      logger.info("Deployment: Skipping deployment teardown");
    }
  }

  /**
   * Call the setup() method of each TestScript class. If one of the classes throws an exception,
   * stop looping through the remaining setup methods and return the exception.
   *
   * @return the exception thrown, null if none
   */
  private Throwable callTestScriptSetups() {
    for (TestScript testScript : scripts) {
      try {
        testScript.setup(config.testUsers);
      } catch (Throwable setupEx) {
        // return the first exception thrown and stop looping through the setup methods
        return setupEx;
      }
    }
    return null;
  }

  /**
   * Call the cleanup() method of each TestScript class. If any of the classes throws an exception,
   * keep looping through the remaining cleanup methods before returning. Save the first exception
   * thrown and return it.
   *
   * @return the first exception thrown, null if none
   */
  private Throwable callTestScriptCleanups() {
    Throwable exceptionThrown = null;
    for (TestScript testScript : scripts) {
      try {
        testScript.cleanup(config.testUsers);
      } catch (Throwable cleanupEx) {
        // save the first exception thrown, keep looping through the remaining cleanup methods
        // before returning
        if (exceptionThrown == null) {
          exceptionThrown = cleanupEx;
        }
      }
    }
    return exceptionThrown;
  }

  private static class UserJourneyThread implements Callable<UserJourneyResult> {
    TestScript testScript;
    String userJourneyDescription;
    TestUserSpecification testUser;

    public UserJourneyThread(
        TestScript testScript, String userJourneyDescription, TestUserSpecification testUser) {
      this.testScript = testScript;
      this.userJourneyDescription = userJourneyDescription;
      this.testUser = testUser;
    }

    public UserJourneyResult call() {
      UserJourneyResult result =
          new UserJourneyResult(userJourneyDescription, Thread.currentThread().getName());

      long startTime = System.nanoTime();
      try {
        testScript.userJourney(testUser);
      } catch (Throwable ex) {
        result.saveExceptionThrown(ex);
      }
      result.elapsedTimeNS = System.nanoTime() - startTime;

      return result;
    }
  }

  private static class DisruptiveThread implements Runnable {
    DisruptiveScript disruptiveScript;
    List<TestUserSpecification> testUsers;

    public DisruptiveThread(
        DisruptiveScript disruptiveScript, List<TestUserSpecification> testUsers) {
      this.disruptiveScript = disruptiveScript;
      this.testUsers = testUsers;
    }

    public void run() {
      try {
        disruptiveScript.disrupt(testUsers);
      } catch (Exception ex) {
        logger.info("Disruptive thread threw exception: {}", ex.getMessage());
      }
    }
  }

  private void modifyKubernetesPostDeployment() throws Exception {
    if (config.kubernetes.numberOfInitialPods == null) {
      logger.info(
          "Kubernetes: Keeping the number of pods in the {}: {} deployment unchanged",
          config.server.cluster.componentLabel,
          config.server.cluster.apiComponentLabel);
      return;
    }
    // The default values of component label key-value pair for locating a Terra application are
    // defined in ApplicationSpecification.
    //
    // The default values are:
    //
    // { "app.kubernetes.io/component": "api" }
    //
    // To override the default values, configure the following fields:
    //   componentLabel
    //   apiComponentLabel
    //
    KubernetesClientUtils.changeReplicaSetSizeAndWait(
        config.kubernetes.numberOfInitialPods,
        config.server.cluster.componentLabel,
        config.server.cluster.apiComponentLabel);
  }

  private static final String renderedConfigFileName = "RENDERED_testConfiguration.json";
  private static final String userJourneyResultsFileName = "RAWDATA_userJourneyResults.json";
  private static final String runSummaryFileName = "SUMMARY_testRun.json";
  private static final String runConcatSummaryFileName = "SUMMARY_concat_testRun.json";
  private static final String envVersionFileName = "ENV_componentVersion.json";
  private static final String fullOutputFileName = "FULL_testRunOutput.json";

  /** Helper method to write out the results to files at the end of a test configuration run. */
  protected void writeOutResults(String outputParentDirName) throws IOException {
    // use Jackson to map the object to a JSON-formatted text block
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    ObjectWriter summaryObjectWriter =
        objectMapper.writerWithView(SummaryViews.Summary.class).withDefaultPrettyPrinter();
    ObjectWriter fullOutputObjectWriter =
        objectMapper.writerWithView(SummaryViews.FullOutput.class).withDefaultPrettyPrinter();

    // print the summary results to info
    logger.info(objectWriter.writeValueAsString(summary));

    // create the output directory if it doesn't already exist
    Path outputDirectory =
        Paths.get(outputParentDirName); // .resolve(config.name + "_" + summary.id);
    File outputDirectoryFile = outputDirectory.toFile();
    if (outputDirectoryFile.exists() && !outputDirectoryFile.isDirectory()) {
      throw new IllegalArgumentException(
          "Output directory already exists as a file: " + outputDirectoryFile.getAbsolutePath());
    }
    boolean outputDirectoryCreated = outputDirectoryFile.mkdirs();
    logger.debug(
        "outputDirectoryCreated {}: {}",
        outputDirectoryFile.getAbsolutePath(),
        outputDirectoryCreated);
    logger.info("Test run results written to directory: {}", outputDirectoryFile.getAbsolutePath());

    // create the output files if they don't already exist
    File renderedConfigFile = outputDirectory.resolve(renderedConfigFileName).toFile();
    File userJourneyResultsFile =
        FileUtils.createNewFile(outputDirectory.resolve(userJourneyResultsFileName).toFile());
    File runSummaryFile = outputDirectory.resolve(runSummaryFileName).toFile();
    File terraVersionFile = outputDirectory.resolve(envVersionFileName).toFile();
    File runFullOutputFile = outputDirectory.resolve(fullOutputFileName).toFile();

    // write the rendered test configuration that was run to a file
    objectWriter.writeValue(renderedConfigFile, config);
    logger.info("Rendered test configuration written to file: {}", renderedConfigFile.getName());

    // write the full set of user journey results to a file
    objectWriter.writeValue(userJourneyResultsFile, testScriptResults);
    logger.info("All user journey results written to file: {}", userJourneyResultsFile.getName());

    // write the test run summary to a file
    summaryObjectWriter.writeValue(runSummaryFile, runFullOutput);
    logger.info("Test run summary written to file: {}", runSummaryFile.getName());

    // Write the MCTerra Component versions of target environment to a file
    objectWriter.writeValue(terraVersionFile, componentVersions);
    logger.info("MCTerra Component versions written to file: {}", terraVersionFile.getName());

    fullOutputObjectWriter.writeValue(runFullOutputFile, runFullOutput);
    logger.info("Test run full output written to file: {}", runFullOutputFile.getName());
  }

  /** Helper method to print out the PASSED/FAILED tests at the end of a suite run. */
  protected static void printSuiteResults(Map<String, Boolean> testConfigNameToFailed) {
    System.out.println(ANSI_PURPLE + "PASSED test configurations" + ANSI_RESET);
    List<String> passedTestConfigs =
        testConfigNameToFailed.keySet().stream()
            .filter(testConfigName -> !testConfigNameToFailed.get(testConfigName))
            .collect(Collectors.toList());
    for (String passingTestConfig : passedTestConfigs) {
      System.out.println(passingTestConfig + System.lineSeparator());
    }
    System.out.println(System.lineSeparator());

    System.out.println(ANSI_PURPLE + "FAILED test configurations" + ANSI_RESET);
    List<String> failedTestConfigs =
        testConfigNameToFailed.keySet().stream()
            .filter(testConfigName -> testConfigNameToFailed.get(testConfigName))
            .collect(Collectors.toList());
    for (String failedTestConfig : failedTestConfigs) {
      System.out.println(failedTestConfig + System.lineSeparator());
    }
    System.out.println(System.lineSeparator());
  }

  /**
   * Read in the rendered test configuration from the output directory and return the
   * TestConfiguration Java object.
   */
  public static TestConfiguration getRenderedTestConfiguration(Path outputDirectory)
      throws Exception {
    return FileUtils.readOutputFileIntoJavaObject(
        outputDirectory, TestRunner.renderedConfigFileName, TestConfiguration.class);
  }

  /**
   * Read in the test run summary from the output directory and return the TestRunSummary Java
   * object.
   */
  public static TestRunSummary getTestRunSummary(Path outputDirectory) throws Exception {
    return FileUtils.readOutputFileIntoJavaObject(
        outputDirectory, TestRunner.runSummaryFileName, TestRunSummary.class);
  }

  /**
   * Build a list of output directories that contain test run results. - For a single test config,
   * this is just the provided output directory - For a test suite, this is all the immediate
   * sub-directories of the provided output directory
   *
   * @return a list of test run output directories
   */
  public static List<Path> getTestRunOutputDirectories(Path outputDirectory) throws Exception {
    // check that the output directory exists
    if (!outputDirectory.toFile().exists()) {
      throw new FileNotFoundException(
          "Output directory not found: " + outputDirectory.toAbsolutePath());
    }

    // build a list of output directories that contain test run results
    List<Path> testRunOutputDirectories = new ArrayList<>();
    TestRunSummary testRunSummary = null;
    try {
      testRunSummary = getTestRunSummary(outputDirectory);
    } catch (Exception ex) {
    }

    if (testRunSummary != null) { // single test config
      testRunOutputDirectories.add(outputDirectory);
    } else { // test suite
      File[] subdirectories = outputDirectory.toFile().listFiles(File::isDirectory);
      if (subdirectories == null) {
        throw new RuntimeException("Unexpected output directory format, no test runs found.");
      }
      for (int ctr = 0; ctr < subdirectories.length; ctr++) {
        testRunOutputDirectories.add(subdirectories[ctr].toPath());
      }
    }
    return testRunOutputDirectories;
  }

  /** Returns a boolean indicating whether any test runs failed or not. */
  public static boolean runTest(String configFileName, String outputParentDirName)
      throws Exception {
    logger.info("==== READING IN TEST SUITE/CONFIGURATION(S) ====");
    // read in test suite and validate it
    TestSuite testSuite;
    boolean isSuite = configFileName.startsWith(TestSuite.resourceDirectory + "/");
    boolean isSingleConfig = configFileName.startsWith(TestConfiguration.resourceDirectory + "/");
    if (isSuite) {
      testSuite =
          TestSuite.fromJSONFile(configFileName.split(TestSuite.resourceDirectory + "/")[1]);
      logger.info("Found a test suite: {}", testSuite.name);
    } else if (isSingleConfig) {
      TestConfiguration testConfiguration =
          TestConfiguration.fromJSONFile(
              configFileName.split(TestConfiguration.resourceDirectory + "/")[1]);
      testSuite = TestSuite.fromSingleTestConfiguration(testConfiguration);
      logger.info("Found a single test configuration: {}", testConfiguration.name);
    } else {
      throw new RuntimeException(
          "File reference "
              + configFileName
              + " is not found as a test suite (in "
              + TestSuite.resourceDirectory
              + "/) or as a test config (in "
              + TestConfiguration.resourceDirectory
              + "/)");
    }
    testSuite.validate();

    boolean isFailure = false;
    Map<String, Boolean> testConfigNameToFailed = new HashMap<>();
    for (int ctr = 0; ctr < testSuite.testConfigurations.size(); ctr++) {
      TestConfiguration testConfiguration = testSuite.testConfigurations.get(ctr);

      logger.info(
          "==== EXECUTING TEST CONFIGURATION ({}) {} ====", ctr + 1, testConfiguration.name);
      logger.info(testConfiguration.display());

      // get an instance of a runner and tell it to execute the configuration
      TestRunner runner = new TestRunner(testConfiguration);
      runner.summary.setTestSuiteName(testSuite.name);
      runner.runFullOutput.testConfiguration = testConfiguration;
      boolean testConfigFailed = false;
      try {
        runner.executeTestConfiguration();

        // even if the test configuration didn't throw an exception, it still may have failed due to
        // a timeout
        for (TestScriptResultSummary testScriptResultSummary :
            runner.summary.testScriptResultSummaries) {
          if (testScriptResultSummary.isFailure) {
            testConfigFailed = true;
            break;
          }
        }
      } catch (Exception runnerEx) {
        logger.error("Test Runner threw an exception", runnerEx);
        testConfigFailed = true;
      }

      // update the failure flag for this test config and the whole suite
      testConfigNameToFailed.put(testConfiguration.name, testConfigFailed);
      isFailure = isFailure || testConfigFailed;

      logger.info("==== TEST RUN RESULTS ({}) {} ====", ctr + 1, testConfiguration.name);
      String outputDirName =
          outputParentDirName; // if running a single config, put the results in the given directory
      if (isSuite) { // if running a suite, put each config results in a separate sub-directory
        outputDirName =
            Paths.get(outputParentDirName)
                .resolve(testConfiguration.name + "_" + runner.summary.id)
                .toAbsolutePath()
                .toString();
      }
      runner.writeOutResults(outputDirName);

      TimeUnit.SECONDS.sleep(5);
    }
    printSuiteResults(testConfigNameToFailed);

    return isFailure;
  }
}
