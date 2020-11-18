package bio.terra.testrunner.common.commands;

import bio.terra.testrunner.collector.config.MeasurementList;
import bio.terra.testrunner.common.utils.FileUtils;
import bio.terra.testrunner.runner.config.TestConfiguration;
import bio.terra.testrunner.runner.config.TestSuite;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PrintHelp {
  public static void main(String[] args) throws IOException {
    System.out.println(
        ANSI_PURPLE
            + "Usage (PrintHelp): ./gradlew printHelp --args=\"parentDirectory\""
            + ANSI_RESET
            + System.lineSeparator()
            + "  parentDirectory = parent directory to look for test configuration or suite JSON file"
            + System.lineSeparator());
    File file;
    if (args.length >= 1) {
      file = new File(args[0]);
      if (!file.isAbsolute()) {
        String cwd = System.getProperty("user.dir");
        file = new File(cwd, args[0]);
      }
      if (file.exists()) parentDirectory = file.toPath();
      else {
        System.out.println(
            String.format("Directory %s does not exist ", parentDirectory.toString()));
      }
    }
    printHelp();
  }

  private static Path parentDirectory = null;
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_PURPLE = "\u001B[35m";

  public static void printHelp() throws IOException {
    // upload results and measurements for a test run
    System.out.println(ANSI_PURPLE + "Usage (0): ./gradlew printHelp" + ANSI_RESET);

    // execute a test configuration or suite
    System.out.println(
        ANSI_PURPLE
            + "Usage (1): ./gradlew runTest --args=\"configOrSuiteFileName outputDirectoryName\""
            + ANSI_RESET
            + System.lineSeparator()
            + "  configOrSuiteFileName = file name of the test configuration or suite JSON file"
            + System.lineSeparator()
            + "  outputDirectoryName = name of the directory where the results will be written"
            + System.lineSeparator());
    // print out the available test configurations and suites found in the resources directory
    System.out.println("  The following test configuration files were found:");
    printAvailableFiles(TestConfiguration.resourceDirectory, parentDirectory);
    System.out.println("  The following test suite files were found:");
    printAvailableFiles(TestSuite.resourceDirectory, parentDirectory);
    // execute a test configuration or suite while locking server
    System.out.println(
        ANSI_PURPLE
            + "Usage (2):\n"
            + "export TEST_RUNNER_SERVER_SPECIFICATION_FILE=\"mmdev.json\"\n"
            + "./gradlew lockAndRunTest --args=\"configOrSuiteFileName outputDirectoryName\""
            + ANSI_RESET
            + System.lineSeparator()
            + "Same as runTest, but locking the designated server before the tests run\n"
            + "and unlocking the server after the tests run"
            + System.lineSeparator()
            + "REQUIRES setting the TEST_RUNNER_SERVER_SPECIFICATION_FILE environment variable"
            + System.lineSeparator()
            + ANSI_PURPLE
            + "Can use manual lock/unlock gradle commands to reset the locks:\n"
            + "export TEST_RUNNER_SERVER_SPECIFICATION_FILE=\"mmdev.json\"\n"
            + "./gradlew lockNamespace\n"
            + "./gradlew unlockNamespace"
            + ANSI_RESET
            + System.lineSeparator());
    // collect measurements for a test run
    System.out.println(
        ANSI_PURPLE
            + "Usage (3): ./gradlew collectMeasurements --args=\"measurementListFileName outputDirectoryName\""
            + ANSI_RESET
            + System.lineSeparator()
            + "  measurementListFileName = file name of the measurement list JSON file"
            + System.lineSeparator()
            + "  outputDirectoryName = name of the same directory that contains the Test Runner results"
            + System.lineSeparator());

    // collect measurements for a time interval
    System.out.println(
        ANSI_PURPLE
            + "Usage (4): ./gradlew collectMeasurements --args=\"measurementListFileName outputDirectoryName serverFileName startTimestamp endTimestamp\""
            + ANSI_RESET
            + System.lineSeparator()
            + "  measurementListFileName = file name of the measurement list JSON file"
            + System.lineSeparator()
            + "  outputDirectoryName = name of the directory where the results will be written"
            + System.lineSeparator()
            + "  serverFileName = name of the server JSON file"
            + System.lineSeparator()
            + "  startTimestamp = start of the interval; format must be yyyy-mm-dd hh:mm:ss[.fffffffff] (UTC timezone)"
            + System.lineSeparator()
            + "  endTimestamp = end of the interval; format must be yyyy-mm-dd hh:mm:ss[.fffffffff] (UTC timezone)"
            + System.lineSeparator());
    // print out the available measurement lists found in the resources directory
    System.out.println("  The following measurement lists were found:");
    printAvailableFiles(MeasurementList.resourceDirectory, parentDirectory);

    // upload results and measurements for a test run
    System.out.println(
        ANSI_PURPLE
            + "Usage (5): ./gradlew uploadResults --args=\"uploadListFileName outputDirectoryName\""
            + ANSI_RESET
            + System.lineSeparator()
            + "  uploadListFileName = file name of the upload list JSON file"
            + System.lineSeparator()
            + "  outputDirectoryName = name of the same directory that contains the Test Runner and measurement results"
            + System.lineSeparator());

    // example workflows
    System.out.println(
        ANSI_PURPLE
            + "Example Workflow (6): Execute a test configuration, collect the measurements generated by the server during the run, and upload the results"
            + ANSI_RESET
            + System.lineSeparator()
            + "  ./gradlew runTest --args=\"configs/basicexamples/BasicUnauthenticated.json /tmp/TestRunnerResults\""
            + System.lineSeparator()
            + "  ./gradlew collectMeasurements --args=\"BasicKubernetes.json /tmp/TestRunnerResults\""
            + System.lineSeparator()
            + "  ./gradlew uploadResults --args=\"BroadJadeDev.json /tmp/TestRunnerResults\""
            + System.lineSeparator());
    System.out.println(
        ANSI_PURPLE
            + "Example Workflow (7): Execute a test configuration while locking the server, collect the measurements generated by the server during the run, and upload the results"
            + ANSI_RESET
            + System.lineSeparator()
            + "export TEST_RUNNER_SERVER_SPECIFICATION_FILE=\"mmdev.json\"\n"
            + System.lineSeparator()
            + "  ./gradlew lockAndRunTest --args=\"configs/basicexamples/BasicUnauthenticated.json /tmp/TestRunnerResults\""
            + System.lineSeparator()
            + "  ./gradlew collectMeasurements --args=\"BasicKubernetes.json /tmp/TestRunnerResults\""
            + System.lineSeparator()
            + "  ./gradlew uploadResults --args=\"BroadJadeDev.json /tmp/TestRunnerResults\""
            + System.lineSeparator());
    System.out.println(
        ANSI_PURPLE
            + "Example Workflow (8): Collect the measurements generated by the server for a particular time interval"
            + ANSI_RESET
            + System.lineSeparator()
            + "  ./gradlew collectMeasurements --args=\"AllMeasurements.json /tmp/TestRunnerResults mmdev.json '2020-08-20 13:18:34' '2020-08-20 13:18:35.615628881'\""
            + System.lineSeparator());
    System.out.println(
        ANSI_PURPLE
            + "Example Workflow (9): Execute a test suite"
            + ANSI_RESET
            + System.lineSeparator()
            + "  ./gradlew runTest --args=\"suites/BasicSmoke.json /tmp/TestRunnerResults\""
            + System.lineSeparator());
  }

  public static void printAvailableFiles(String subDirectoryName, Path parentDirectory)
      throws IOException {
    // use the resources directory as the default parent directory
    File parentDirectoryFile;
    if (parentDirectory == null) {
      parentDirectoryFile =
          new File(PrintHelp.class.getClassLoader().getResource(subDirectoryName).getFile());
    } else {
      System.out.println(subDirectoryName);
      parentDirectoryFile = parentDirectory.resolve(subDirectoryName).toFile();
      System.out.println(parentDirectoryFile.toString());
    }

    List<String> availableTestConfigs = FileUtils.getFilesInDirectory(parentDirectoryFile);
    for (String testConfigFilePath : availableTestConfigs) {
      System.out.println("    " + testConfigFilePath);
    }
    System.out.println();
  }
}