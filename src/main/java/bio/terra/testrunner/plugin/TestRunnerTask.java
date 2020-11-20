package bio.terra.testrunner.plugin;

import org.gradle.api.tasks.JavaExec;

/**
 * This sub-class of the JavaExec class exists only to distinguish TestRunner tasks from other
 * (non-TestRunner) JavaExec tasks defined in the same project.
 */
public class TestRunnerTask extends JavaExec {}
