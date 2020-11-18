package bio.terra.testrunner.common.commands;

import bio.terra.testrunner.runner.TestRunner;

public class RunTest {
  public static void main(String[] args) throws Exception {
    if (args.length == 2) { // execute a test configuration or suite
      boolean isFailure = TestRunner.runTest(args[0], args[1]);
      if (isFailure) {
        System.exit(1);
      }
    } else { // if no args specified or invalid number of args specified, print help
      PrintHelp.printHelp();
    }
  }
}
