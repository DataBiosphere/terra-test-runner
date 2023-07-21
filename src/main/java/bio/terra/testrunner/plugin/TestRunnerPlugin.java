package bio.terra.testrunner.plugin;

import bio.terra.testrunner.common.commands.CollectMeasurements;
import bio.terra.testrunner.common.commands.LockAndRunTest;
import bio.terra.testrunner.common.commands.LockNamespace;
import bio.terra.testrunner.common.commands.PrintHelp;
import bio.terra.testrunner.common.commands.RunTest;
import bio.terra.testrunner.common.commands.UnlockNamespace;
import bio.terra.testrunner.common.commands.UploadResults;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a Gradle plugin that auto-populates the Test Runner tasks. These tasks all
 * extend JavaExec, which jobs off a separate JVM to run the task, instead of running in-process
 * with Gradle.
 */
public class TestRunnerPlugin implements Plugin<Project> {
  private static final Logger logger = LoggerFactory.getLogger(TestRunnerPlugin.class);

  private static final String PROPERTY_NAME_SOURCE_SETS = "sourceSets";
  private static final String SOURCE_SET_NAME_MAIN = "main";

  /**
   * This class defines a set of properties that are relevant to >1 task. These values can be set in
   * the calling Gradle file and supplied to each task when it's called. This class is not a good
   * place to put task-specific variables (e.g. the config file for the runTest task).
   */
  public static class TestRunnerPluginExtension {
    // specifies the directory in which to look for service account credential files
    // this value overrides the default one specified in the service account specification file
    String SaKeyDirectoryPath;

    public String getSaKeyDirectoryPath() {
      return SaKeyDirectoryPath;
    }

    public void setSaKeyDirectoryPath(String SaKeyDirectoryPath) {
      this.SaKeyDirectoryPath = SaKeyDirectoryPath;
    }
  }

  @Override
  public void apply(Project project) {
    // create a default version of the extension = properties relevant to >1 task
    // these values can be overridden by defining them in the build.gradle file
    logger.debug("Adding the TestRunner plugin extension to {}", project.getName());
    project.getExtensions().create("testRunner", TestRunnerPluginExtension.class);

    // apply the Java plugin, so that the JavaExec task will work
    logger.debug("Applying the Java plugin to {}", project.getName());
    project.getPlugins().apply(JavaPlugin.class);

    // create the tasks
    logger.debug("Adding the TestRunner tasks to {}", project.getName());
    Class<?>[] commandClasses = {
      CollectMeasurements.class,
      LockAndRunTest.class,
      LockNamespace.class,
      PrintHelp.class,
      RunTest.class,
      UnlockNamespace.class,
      UploadResults.class
    };
    TaskContainer taskContainer = project.getTasks();
    for (Class<?> commandClass : commandClasses) {
      TestRunnerTask task =
          taskContainer.create(commandClass.getSimpleName(), TestRunnerTask.class);
      task.getMainClass().set(commandClass.getCanonicalName());
    }

    // set the classpath of all tasks to sourceSets.main.runtimeClasspath
    // Note: This next part took me a long time to figure out. My current
    // understanding is that there are 3 phases of the Gradle build from the
    // plugin's perspective:
    //   1. plugin adds tasks to the Gradle project definition
    //   2. Gradle "evaluates" the project, which includes populating the runtime classpath
    //   3. plugin can access any property of the project (e.g. runtime classpath) to pass to its
    // own tasks/code
    logger.debug(
        "Setting the classpath of all TestRunner tasks to sourceSets.main.runtimeClasspath");
    project.afterEvaluate(
        p -> {
          final SourceSetContainer sourceSets =
              (SourceSetContainer) project.getProperties().get(PROPERTY_NAME_SOURCE_SETS);
          project
              .getTasks()
              .withType(TestRunnerTask.class)
              .forEach(
                  testRunnerTask ->
                      testRunnerTask.setClasspath(
                          sourceSets.findByName(SOURCE_SET_NAME_MAIN).getRuntimeClasspath()));

          // The commented out lines below show how to access properties set in the extension
          // definition in the calling build.gradle file
          /*TestRunnerPluginExtension testRunnerPluginExtension =
              (TestRunnerPluginExtension) project.getExtensions().findByName("testRunner");
          System.out.println("SaKeyDirectoryPath: " + testRunnerPluginExtension.getSaKeyDirectoryPath());*/
        });
  }
}
