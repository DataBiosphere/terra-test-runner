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

  @Override
  public void apply(Project project) {
    // create a default version of the extension = properties relevant to >1 task
    // these values can be overridden by defining them in the build.gradle file
    logger.debug("Adding the TestRunner plugin extension to {}", project.getName());
    project
        .getExtensions()
        .create(TestRunnerPluginExtension.EXTENSION_NAME, TestRunnerPluginExtension.class);

    // apply the Java plugin, so that the JavaExec task will work
    logger.debug("Applying the Java plugin to {}", project.getName());
    project.getPlugins().apply(JavaPlugin.class);

    // create the tasks
    logger.debug("Adding the TestRunner tasks to {}", project.getName());
    Class[] commandClasses = {
      CollectMeasurements.class,
      LockAndRunTest.class,
      LockNamespace.class,
      PrintHelp.class,
      RunTest.class,
      UnlockNamespace.class,
      UploadResults.class
    };
    TaskContainer taskContainer = project.getTasks();
    for (int ctr = 0; ctr < commandClasses.length; ctr++) {
      Class commandClass = commandClasses[ctr];
      taskContainer
          .create(commandClass.getSimpleName(), TestRunnerTask.class)
          .setMain(commandClass.getCanonicalName());
    }

    // set the classpath of all tasks to sourceSets.main.runtimeClasspath
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
        });
  }
}
