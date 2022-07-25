The `Test Runner Framework` can be installed through dependency declaration for the following build tools. Throughout this Wiki,  `IntelliJ` IDE has been used to demonstrate the steps of creating and launching test classes. These steps will work for any compatible IDE.

* [Gradle](#gradle)
  * Under construction
* [Maven](#maven)
  * Under construction
* [Sbt](#sbt)
  * [Overview](#sbt-overview)
  * [Create a `test-runner` subproject](#sbt-subproject)
  * [Dependencies](#sbt-dependencies)
  * [Building a Test Class for `Sam` API `getSystemStatus`](#sbt-sample-code-1)
  * [Test Runner Resource Configurations](#test-runner-configs)
    * [Server Specification](#test-runner-server-spec)
    * [Service Account Specification](#test-runner-service-account-spec)
    * [Test Script Specification](#test-runner-test-script-spec)
    * [Test Configuration](#test-runner-test-config)
    * [Test Suite](#test-runner-test-suite)
  * [Test User Specification](#test-runner-test-user-spec)

# Gradle

### <a name="gradle-overview"></a>Under construction

The following Java projects integrate `Test Runner Framework` using `Gradle` build tool.

* [Data Catalog](https://github.com/DataBiosphere/terra-data-catalog)
* [External Credentials Manager](https://github.com/DataBiosphere/terra-external-credentials-manager)
* [Workspace Manager](https://github.com/DataBiosphere/terra-workspace-manager)

# Maven

### <a name="maven-overview"></a>Under construction

# Sbt

### <a name="sbt-overview"></a>Overview

In this section, we will show you how to integrate `Test Runner Framework` with the [`Sam` service repository](https://github.com/broadinstitute/sam) to run tests against `BEE`. The `Sam` service repo is structured as a `Scala` project.

The build workflow [https://github.com/broadinstitute/sam/blob/develop/.github/workflows/build.yml](https://github.com/broadinstitute/sam/blob/develop/.github/workflows/build.yml) is triggered by a pull request and a new version of `Sam` client is published to `Artifactory` through the `generateAndPublishClient` job. For example, `0.1-bf9c033-SNAP` below is just a version tag published on `07-20-2022`.

* [https://broadinstitute.jfrog.io/artifactory/libs-release-local/org/broadinstitute/dsde/workbench/sam-client_2.13/0.1-bf9c033-SNAP/](https://broadinstitute.jfrog.io/artifactory/libs-release-local/org/broadinstitute/dsde/workbench/sam-client_2.13/0.1-bf9c033-SNAP/)

New versions are published frequently. The latest version tag can be found in the `generateAndPublishClient` job log from the most recent GitHub Actions run in [https://github.com/broadinstitute/sam/actions](https://github.com/broadinstitute/sam/actions).

### <a name="sbt-subproject"></a>Create a `test-runner` subproject.

Perform the next two steps.

1. Open `IntelliJ` and clone [https://github.com/broadinstitute/sam](https://github.com/broadinstitute/sam).
1. Create a new branch.

Now open the `project/Settings.scala` file as shown below. Take note of the `artifactory` and `commonResolvers` variables. We will need them later.

!['Project Settings'](https://drive.google.com/uc?export=view&id=1EXA6J_sr63Pulju6Hq-NRBEQ_QWsgngu)

Open the `build.sbt` file, copy and paste the code below, save the changes.

<a name="build-sbt"></a>
```scala
val samVersion  = "0.1-bf9c033-SNAP"
val testRunnerVersion = "0.1.6-SNAPSHOT"

lazy val testRunner = project
  .in(file("test-runner"))
  .settings(
    name := "test-runner",
    resolvers ++= commonResolvers,
    libraryDependencies ++= Seq(
      "org.broadinstitute.dsde.workbench" % "sam-client_2.13" % samVersion,
      "bio.terra" % "terra-test-runner" % testRunnerVersion
    )
  )
```

The new `build.sbt` should look like this

!['New build.sbt'](https://drive.google.com/uc?export=view&id=1_k5A8vaOQLTmTlif5IyF59cqwuuMcJxF)

:warning: The `samVersion` and `testRunnerVersion` variables should be replaced by the desired values if appropriate.

<a name="sbt-reload"></a>
Reload the project. Select `View > Tool Windows > Build` to launch the `IntelliJ` Build tool window.

!['New build.sbt'](https://drive.google.com/uc?export=view&id=1IGz9egzy2W3SlEa0TKtFsbzu0-PDvQsi)

Click the reload button (marked by the red box). Wait for syncing to finish successfully (green check next to the text `sam: finished`). The `test-runner` subproject folder should now appear in the Project Explorer. The `test-runner` subproject is largely empty at this point and does not yet have a code structure. We will start adding some code later.

:warning: If you are getting errors during the reload, then most likely you are missing some local environment variables. If you are using `bash` shell, make sure the following code is in your `~/.bash_profile`. Don't forget to run `source ~/.bash_profile` after you made the changes. The `ARTIFACTORY_USERNAME` and `ARTIFACTORY_PASSWORD` are required to download binaries from Artifactory.

```bash
VAULT_TOKEN=${1:-$(cat $HOME/.vault-token)}
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:consul-0.20.0
ARTIFACTORY_ACCOUNT_PATH=secret/dsp/accts/artifactory/dsdejenkins

export ARTIFACTORY_USERNAME=$(docker run -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
 vault read -field username ${ARTIFACTORY_ACCOUNT_PATH})
export ARTIFACTORY_PASSWORD=$(docker run -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
 vault read -field password ${ARTIFACTORY_ACCOUNT_PATH})
```

### <a name="sbt-dependencies"></a>Dependencies

If you are starting out with a brand new `Scala` project, then you need the following code in `build.sbt` to be able to use `Test Runner Framework`. It is up to you to place some of these code in common project settings (see [https://github.com/broadinstitute/sam/blob/develop/project/Settings.scala](https://github.com/broadinstitute/sam/blob/develop/project/Settings.scala)). Once you save changes to `build.sbt`, reload the project as described in [here](#sbt-reload).

```scala
val artifactory_username = sys.env.get("ARTIFACTORY_USERNAME").getOrElse("")
val artifactory_password = sys.env.get("ARTIFACTORY_PASSWORD").getOrElse("")

resolvers += "artifactory-snapshots-local" at "https://broadinstitute.jfrog.io/broadinstitute/libs-snapshot-local/"
credentials += Credentials("Artifactory Realm", "broadinstitute.jfrog.io", 
  artifactory_username, artifactory_password)

val testRunnerVersion = "0.1.6-SNAPSHOT"
libraryDependencies ++= Seq("bio.terra" % "terra-test-runner" % testRunnerVersion)
```

### <a name="sbt-sample-code-1"></a>Building a Test Class for `Sam` API `getSystemStatus` (`/status` endpoint)

In the `test-runner` subproject, create a Scala class `GetSamSystemStatus.scala` as shown in the following diagram. For now, just focus on manually creating the `src/main/scala` folder structure as shown below. The [Test Runner Resource Configurations](#test-runner-configs) section will talk about all other resources in the `src/main/resources` folder.

!['GetSamSystemStatus'](https://drive.google.com/uc?export=view&id=1-okHrJoxU5RkjZWF1upHZV7ncFpniPfM)

A proper test class must

* reside in the `scripts.testscripts` package
* inherit from base class `bio.terra.testrunner.runner.TestScript`
* override and provide the necessary steps for the `userJourney` method

Here's the code snippet for `GetSamSystemStatus.scala`. The `userJourney` was intentionally designed to simulate failures on first 2 tries in order to illustrate the `Test Runner` retry logic.

<a name="test-runner-get-sam-system-status-scala"></a>
`test-runner/src/main/scala/scripts/testscripts/GetSamSystemStatus.scala`
```scala
package scripts.testscripts

import bio.terra.testrunner.runner.TestScript
import bio.terra.testrunner.runner.config.TestUserSpecification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.broadinstitute.dsde.workbench.client.sam.ApiClient
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi
import org.slf4j.LoggerFactory

import java.lang.reflect.Type
import java.util.HashMap
import java.util.List

class GetSamSystemStatus extends TestScript {
  val logger = LoggerFactory.getLogger(classOf[GetSamSystemStatus])

  var counter = ThreadLocal.withInitial[Int](() => 0)

  @throws(classOf[Exception])
  override def setup(testUsers: List[TestUserSpecification]): Unit = {
    logger.info("setup")
  }

  @throws(classOf[Exception])
  override def userJourney(testUser: TestUserSpecification): Unit = {
    val client = new ApiClient()
    client.setBasePath(this.server.samUri)
    val api = new StatusApi(client)
    counter.set(counter.get + 1)
    if (counter.get <= 2) {
      logger.info("Attempt={}", counter.get)
      throw new Exception("Service hanged")
    }
    val status = api.getSystemStatus()
    logger.info("systems: {}", status.getSystems())
    val gson = new Gson()
    val t : Type = new TypeToken[HashMap[String, HashMap[String, Boolean]]]() {}.getType()
    val systems : HashMap[String, HashMap[String, Boolean]] = gson.fromJson(api.getSystemStatus().getSystems().toString(), t)

    logger.info("Attempt={} {}", counter.get, if (status.getOk()) "passed" else "failed")
    systems.forEach {
      case(k, v) => {
        v.get("ok") match {
          case true => logger.info("System '{}' passed", k)
          case false => logger.info("System '{}' failed", k)
        }
      }
    }
    counter.set(0)
  }

  @throws(classOf[Exception])
  override def cleanup(testUsers: List[TestUserSpecification]): Unit = {
    logger.info("cleanup")
  }
}
```

### <a name="test-runner-configs"></a>Test Runner Resource Configurations

The `Test Runner Framework` consumes a number of resource configuration files each with its own `json` specification.

* [Server Specification](#test-runner-server-spec)
* [Service Account Specification](#test-runner-service-account-spec)
* [Test Script Specification](#test-runner-test-script-spec)
* [Test Configuration](#test-runner-test-config)
* [Test Suite](#test-runner-test-suite)

All resource files must reside under the `src/main/resources` directory as shown in the following diagram.

!['Test Runner Resources'](https://drive.google.com/uc?export=view&id=1Satv76TVTyfenyFmSp97IpR3Z7iuEqGW)

#### <a name="test-runner-server-spec"></a>Server Specification

Server specification files must reside in the `src/main/resources/servers` directory. The base URL of each application has a corresponding `json` property <code><i>service</i>Uri</code>. The supported <code><i>service</i></code> codes are <code><i>buffer, catalog, datarepo, drsHub, externalCredentialsManager, rawls, sam, workspaceManager</i></code>.

For example, to configure the base URL of the `Sam` application in one of the `BEE` environments, we will include the <code><i>sam</i>Uri</code> property as shown in the `sam-ichang-comic-lizard-bee.json` file below.

<a name="test-runner-server-spec-sample"></a>
`src/main/resources/servers/sam-ichang-comic-lizard-bee.json`
```json
{
  "name": "sam-ichang-comic-lizard-bee",
  "description": "Sam BEE Testing Environment",

  "samUri": "https://sam.fiab-ichang-comic-lizard.bee.envs-terra.bio",

  "deploymentScript": {},
  "testRunnerServiceAccountFile": "testrunner-sa.json",

  "skipDeployment": true,
  "skipKubernetes": true
}
```

The `name` and `description` properties are required and are used to provide some easily identifiable information.

For now, set both `skipDeployment` and `skipKubernetes` properties to `true` since the `Sam` application has already been deployed to `BEE` and we are not modifying the deployment during test flight.

The `testRunnerServiceAccountFile` property is described in the [Service Account Specification](#test-runner-service-account-spec) section below.

#### <a name="test-runner-service-account-spec"></a>Service Account Specification

Service account specification files must reside in the `src/main/resources/serviceaccounts` directory.

The `testRunnerServiceAccountFile` property in [`sam-ichang-comic-lizard-bee.json`](#test-runner-server-spec-sample) references a service account specification file `testrunner-sa.json` with the following structure.

<a name="test-runner-service-account-spec-sample"></a>
`src/main/resources/serviceaccounts/testrunner-sa.json`
```json
{
  "name": "testrunner-perf@broad-dsde-perf.iam.gserviceaccount.com",
  "jsonKeyFilename": "testrunner-sa.json",
  "jsonKeyDirectoryPath": "test-runner/src/main/resources/rendered"
}
```

The `jsonKeyFilename` property is the name of the file that contains the `json`-formatted service account key.

Service account keys can be placed anywhere within the project file system.

The `jsonKeyDirectoryPath` property is the path to the service account key. Relative path should only be used and is resolved through the project root directory.

The `Test Runner` service account is a Google Service Account for carrying out the following responsibilities

* uploading test results to Cloud Storage
* modifying application deployment

For now, we will put a placeholder file `testrunner-sa.json` under `test-runner/src/main/resources/rendered` directory since we are going to skip uploading test results and since `skipDeployment` and `skipKubernetes` were both set to `true`.

<a name="test-runner-sa-placeholder"></a>
`test-runner/src/main/resources/rendered/testrunner-sa.json`
```json
{}
```

#### <a name="test-runner-test-script-spec"></a>Test Script Specification

Test Script Specification allows the following properties to be set for individual test class. These properties could potentially be used for benchmarking and tuning purposes.

* `numberOfUserJourneyThreadsToRun` - total number of user journey threads to execute
* `userJourneyThreadPoolSize` - allocated thread pool size for concurrent user journey threads (between 1 and `numberOfUserJourneyThreadsToRun`)
* `expectedTimeForEachUnit` - quoted strings of `java.util.concurrent.TimeUnit` enums: `"DAYS", "HOURS", "MICROSECONDS", "MILLISECONDS", "MINUTES", "NANOSECONDS", "SECONDS"`
* `expectedTimeForEach` - the max duration the thread pool will last (roughly `numberOfUserJourneyThreadsToRun` * `expectedTimeForEach`) in units of `expectedTimeForEachUnit`

Please refer to the [Test Configuration](#test-runner-test-config) section for more details on test script specification.

#### <a name="test-runner-test-config"></a>Test Configuration

Test Configuration files must be placed under the `src/main/resources/configs` directory or its subdirectory.

A test configuration represents a test scenario consisting of a collection of test script specifications (see [Test Script Specification](#test-runner-test-script-spec) section). In addition, the test configuration references the server specification file that contains service URLs (see [Server Specification](#test-runner-server-spec) section).

The test configuration file below has been configured (`serverSpecificationFile` property) with server specification file [`sam-ichang-comic-lizard-bee.json`](#test-runner-server-spec-sample). Relative path must be used, it is resolved through the `src/main/resources/servers` directory.

<a name="test-runner-test-config-sample"></a>
`test-runner/src/main/resources/configs/perf/GetSamSystemStatus.json`
```json
{
  "name": "GetSamSystemStatus",
  "description": "Call the Sam getSystemStatus multiple times. No authentication required.",
  "serverSpecificationFile": "sam-ichang-comic-lizard-bee.json",
  "kubernetes": {},
  "application": {},
  "testScripts": [
    {
      "name": "GetSamSystemStatus",
      "numberOfUserJourneyThreadsToRun": 10,
      "userJourneyThreadPoolSize": 2,
      "expectedTimeForEach": 5,
      "expectedTimeForEachUnit": "SECONDS"
    }
  ],
  "testUserFiles": []
}
```

The `testScripts` property contains a single test script specification referencing the name of the test class [`GetSamSystemStatus`](#test-runner-get-sam-system-status-scala) (`name` property) under the `scripts/testscripts` directory. It directs `Test Runner` to schedule 10 `GetSamSystemStatus::userJourney` threads with a concurrency of 2. The corresponding thread pool will last roughly 50 seconds maximum.

The `name` and `description` properties of the test configuration are used to identify the test scenario.

The `kubernetes` and `application` properties provide cluster url and application deployment metadata to `Test Runner`. They are set to `{}` since no application deployment will be modified.

User credentials are referenced by `testUserFiles` property. It is set to `[]` because `GetSamSystemStatus` does not require authentication. Later we will add another test scenario that requires user authentication.

#### <a name="test-runner-test-suite"></a>Test Suite

Test Suite files must be placed under the `src/main/resources/suites` directory or its subdirectory.

A test suite is simply a collection of test configurations. The `serverSpecificationFile` properties in test configurations are overwritten by their test suite `serverSpecificationFile` property. Relative path must be used, it is resolved through the `src/main/resources/servers` directory.

The test suite file [`FullPerf.json`](#test-runner-test-suite-sample) references [`sam-ichang-comic-lizard-bee.json`](#test-runner-server-spec-sample) as the master server specification.

The `testConfigurationFiles` property is the collection of test configuration files included in the test suite. Relative paths must be used, they are resolved through the `src/main/resources/configs` directory.

<a name="test-runner-test-suite-sample"></a>
`test-runner/src/main/resources/suites/perf/FullPerf.json`
```json
{
  "name": "FullPerf",
  "description": "All perf tests",
  "serverSpecificationFile": "sam-ichang-comic-lizard-bee.json",
  "testConfigurationFiles": [
    "perf/GetSamSystemStatus.json"
  ]
}
```

### <a name="test-runner-launch-test"></a>Launching Test Configuration / Test Suite

We can load a test configuration or test suite into `Test Runner` and launch tests through `sbt` command.

Open a shell, copy and paste the one of the following `sbt` commands and hit `Return`.

```bash
sbt "testRunner/runMain bio.terra.testrunner.common.commands.RunTest suites/perf/FullPerf.json /tmp/testrunner"
```

```bash
sbt "testRunner/runMain bio.terra.testrunner.common.commands.RunTest configs/perf/GetSamSystemStatus.json /tmp/testrunner"
```

Here's the meaning of various arguments in the `sbt` command string

* `testRunner/runMain` - `testRunner` is the subproject ID (see [`build.sbt`](#build-sbt))
  * `runMain` is a `Scala` task that specifies a main method/class to run
  * `testRunner/runMain` specifies a a main method/class to run in the `testRunner` subproject
* `bio.terra.testrunner.common.commands.RunTest` - `Test Runner` test driver
* Positional arguments to `RunTest`
  * `configs/perf/GetSamSystemStatus.json` - test configuration (1st positional input arg to the main method of `RunTest`)
  * `suites/perf/FullPerf.json` - test suite (1st positional input arg to the main method of `RunTest`)
  * `/tmp/testrunner` - the absolute path of a local directory to store `Test Runner` results (2nd positional input arg to the main method of `RunTest`)

If everything goes well, `Test Runner` will output a console log similar to the one below.

```console
[info] welcome to sbt 1.6.2 (AdoptOpenJDK Java 11.0.11)
[info] loading global plugins from /Users/ichang/.sbt/1.0/plugins
[info] loading settings for project sam-build from plugins.sbt ...
[info] loading project definition from /Users/ichang/repos/broadinstitute/sam/project
[info] loading settings for project root from build.sbt ...
[info] set current project to sam (in build file:/Users/ichang/repos/broadinstitute/sam/)
[info] running bio.terra.testrunner.common.commands.RunTest configs/perf/GetSamSystemStatus.json /tmp/scala-testrunner
22:54:52.749 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - ==== READING IN TEST SUITE/CONFIGURATION(S) ====
22:54:53.005 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Found a single test configuration: GetSamSystemStatus
22:54:53.007 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.config.TestSuite - Validating the test configurations
22:54:53.007 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.config.TestConfiguration - Validating the server, Kubernetes and application specifications
22:54:53.007 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.config.TestConfiguration - Validating the test script specifications
22:54:53.008 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.config.TestConfiguration - Validating the test user specifications
22:54:53.008 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - ==== EXECUTING TEST CONFIGURATION (1) GetSamSystemStatus ====
22:54:53.050 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - {
  "name" : "GetSamSystemStatus",
  "description" : "Call the Sam getSystemStatus multiple times. No authentication required.",
  "serverSpecificationFile" : "sam-ichang-comic-lizard-bee.json",
  "billingAccount" : null,
  "isFunctional" : false,
  "resourceFileName" : "perf/GetSamSystemStatus.json",
  "testUserFiles" : [ ],
  "server" : {
    "name" : "sam-ichang-comic-lizard-bee",
    "description" : "Sam BEE",
    "samUri" : "https://sam.fiab-ichang-comic-lizard.bee.envs-terra.bio",
    "datarepoUri" : null,
    "samResourceIdForDatarepo" : null,
    "bufferUri" : null,
    "bufferClientServiceAccountFile" : null,
    "bufferClientServiceAccount" : null,
    "workspaceManagerUri" : null,
    "externalCredentialsManagerUri" : null,
    "catalogUri" : null,
    "cluster" : null,
    "testRunnerServiceAccountFile" : "testrunner-sa.json",
    "testRunnerServiceAccount" : {
      "name" : "testrunner-perf@broad-dsde-perf.iam.gserviceaccount.com",
      "jsonKeyDirectoryPath" : "test-runner/src/main/resources/rendered",
      "jsonKeyFilename" : "testrunner-sa.json"
    },
    "testRunnerK8SServiceAccountFile" : null,
    "testRunnerK8SServiceAccount" : null,
    "deploymentScript" : {
      "name" : "",
      "scriptClass" : null,
      "parameters" : [ "{}" ]
    },
    "skipKubernetes" : true,
    "skipDeployment" : true,
    "versionScripts" : null
  },
  "kubernetes" : {
    "numberOfInitialPods" : null
  },
  "application" : {
    "maxStairwayThreads" : 20,
    "maxBulkFileLoad" : 1000000,
    "maxBulkFileLoadArray" : 1000000,
    "loadConcurrentFiles" : 80,
    "loadConcurrentIngests" : 2,
    "loadDriverWaitSeconds" : 1,
    "loadHistoryCopyChunkSize" : 1000,
    "loadHistoryWaitSeconds" : 2
  },
  "testScripts" : [ {
    "name" : "GetSamSystemStatus",
    "numberOfUserJourneyThreadsToRun" : 10,
    "userJourneyThreadPoolSize" : 2,
    "expectedTimeForEach" : 5,
    "expectedTimeForEachUnit" : "SECONDS",
    "expectedTimeForEachUnitObj" : "SECONDS",
    "description" : "GetSamSystemStatus",
    "parameters" : [ "{}" ]
  } ],
  "testUsers" : [ ],
  "disruptiveScript" : null
}
22:54:53.054 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Deployment: Skipping deployment
22:54:53.054 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Kubernetes: Skipping Kubernetes configuration post-deployment
22:54:53.054 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Version: Skipping version determination
22:54:53.056 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Fetching instance of each class, setting billing account and parameters
22:54:53.056 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Calling the setup methods
22:54:53.056 [sbt-bg-threads-1] INFO scripts.testscripts.GetSamSystemStatus - setup
22:54:53.056 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Creating a thread pool for each TestScript and kicking off the user journeys
22:54:53.057 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Waiting until all threads either finish or time out
22:54:53.253 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
22:54:53.253 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)22:54:53.258 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.

        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:54:53.259 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:54:53.260 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
22:54:53.260 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
22:54:53.301 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:54:53.305 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
22:54:53.948 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:54:53.948 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:54:54.076 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:54:54.076 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:54:54.076 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:54:54.076 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:54:54.076 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:54:54.077 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:54:54.077 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:54:54.077 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:54:54.077 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:54:54.077 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:54:54.078 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:54:54.078 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:54:54.078 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:54:54.078 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:54:56.078 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:54:56.078 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:54:58.084 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)22:54:58.084 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1

        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:54:58.085 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:54:58.085 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:54:58.086 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
22:54:58.086 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
22:54:58.089 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:54:58.091 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
22:54:58.337 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:54:58.337 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:54:58.453 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:54:58.453 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:54:58.454 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:54:58.454 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:54:58.454 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:54:58.454 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:54:58.454 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:54:58.454 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:54:58.455 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:54:58.455 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:54:58.455 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:54:58.455 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:54:58.455 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:54:58.455 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:00.459 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:00.459 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:02.461 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
java.lang.Exception: Service hanged
22:55:02.461 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:55:02.462 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:55:02.462 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:55:02.462 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
22:55:02.463 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)22:55:02.465 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.

        at java.base/java.lang.Thread.run(Thread.java:829)
22:55:02.467 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
22:55:02.715 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:55:02.715 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:55:02.827 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:55:02.827 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:55:02.827 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:55:02.827 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:55:02.827 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:55:02.827 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:55:02.827 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:55:02.828 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:55:02.828 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:55:02.828 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:55:02.828 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:55:02.828 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:55:02.828 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:02.828 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:04.833 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:04.833 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:06.838 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
22:55:06.838 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged22:55:06.838 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.

        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:55:06.839 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:55:06.839 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)22:55:06.839 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2

        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged22:55:06.840 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.

        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:55:06.844 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
22:55:07.075 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:55:07.086 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:55:07.195 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:55:07.195 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:55:07.195 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:55:07.195 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:55:07.195 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:55:07.195 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:55:07.195 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:55:07.195 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:55:07.196 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:55:07.196 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:55:07.196 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:07.196 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:55:07.196 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:55:07.196 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:09.200 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:09.200 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:11.206 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1
java.lang.Exception: Service hanged22:55:11.206 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=1

        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:55:11.207 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:55:11.207 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 1.
22:55:11.209 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)22:55:11.211 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=2

        at java.base/java.lang.Thread.run(Thread.java:829)
java.lang.Exception: Service hanged
        at scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)
        at bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)
        at bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)
        at bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)
        at bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)
        at bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)22:55:11.212 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.

        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
22:55:11.215 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - Retry attempt 2.
22:55:11.489 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:55:11.489 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - systems: {GooglePubSub={ok=true}, Database={ok=true}, GoogleGroups={ok=true}, GoogleIam={ok=true}, OpenDJ={ok=true}}
22:55:11.589 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:55:11.589 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:55:11.589 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:55:11.589 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:55:11.589 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:55:11.590 [pool-9-thread-2] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:55:11.590 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:11.611 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - Attempt=3 passed
22:55:11.611 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'Database' passed
22:55:11.612 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'OpenDJ' passed
22:55:11.612 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GooglePubSub' passed
22:55:11.612 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleIam' passed
22:55:11.613 [pool-9-thread-1] INFO scripts.testscripts.GetSamSystemStatus - System 'GoogleGroups' passed
22:55:11.613 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:13.592 [pool-9-thread-2] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:13.614 [pool-9-thread-1] INFO bio.terra.testrunner.runner.RetryLogic - 1 retry attempt(s) left. Waiting for 2000 milliseconds before exiting or next retry.
22:55:15.619 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Compiling the results from all thread pools
22:55:15.650 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Calling the cleanup methods
22:55:15.650 [sbt-bg-threads-1] INFO scripts.testscripts.GetSamSystemStatus - cleanup
22:55:15.650 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Deployment: Skipping deployment teardown
22:55:15.651 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - ==== TEST RUN RESULTS (1) GetSamSystemStatus ====
22:55:15.654 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - {
  "id" : "d193105c-7261-4b04-ae7c-84785335c597",
  "startTime" : 1658717693054,
  "startUserJourneyTime" : 1658717693056,
  "endUserJourneyTime" : 1658717715619,
  "endTime" : 1658717715651,
  "testScriptResultSummaries" : [ {
    "testScriptName" : "GetSamSystemStatus",
    "testScriptDescription" : "GetSamSystemStatus",
    "elapsedTimeStatistics" : {
      "min" : 4368.753777,
      "max" : 5023.447779,
      "mean" : 4509.5419685,
      "standardDeviation" : 271.1612240618952,
      "median" : 4377.1510755,
      "percentile95" : 5023.447779,
      "percentile99" : 5023.447779,
      "sum" : 45095.419685
    },
    "totalRun" : 10,
    "numCompleted" : 10,
    "numExceptionsThrown" : 0,
    "isFailure" : false
  } ],
  "startTimestamp" : "2022-07-25T02:54:53.0000054Z",
  "startUserJourneyTimestamp" : "2022-07-25T02:54:53.0000056Z",
  "endUserJourneyTimestamp" : "2022-07-25T02:55:15.0000619Z",
  "endTimestamp" : "2022-07-25T02:55:15.0000651Z",
  "testSuiteName" : "GENERATED_SingleTestSuite",
  "githubRunId" : null,
  "githubRepository" : null,
  "githubServerUrl" : null
}
22:55:15.655 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.TestRunner - outputDirectoryCreated /tmp/scala-testrunner: false
22:55:15.655 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test run results written to directory: /tmp/scala-testrunner
22:55:15.668 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Rendered test configuration written to file: RENDERED_testConfiguration.json
22:55:15.673 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - All user journey results written to file: RAWDATA_userJourneyResults.json
22:55:15.675 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test run summary written to file: SUMMARY_testRun.json
22:55:15.689 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test run full output written to file: FULL_testRunOutput.json
22:55:15.690 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Version script result written to file: ENV_versionResult.json
PASSED test configurations
GetSamSystemStatus

FAILED test configurations

[success] Total time: 30 s, completed Jul 24, 2022, 10:55:20 PM
```

The log demonstrates a number of `Test Runner` features

* Retry logic
* User journey threads
* Thread concurrency
* `setup()` and `cleanup()` both executed once per test script (not per thread)

The test results will be sent to the local directory `/tmp/testrunner` as shown below:

!['New build.sbt'](https://drive.google.com/uc?export=view&id=1_1uz5rrIdAXHa0iWul1ceq9ZU2BAwhUL)

The file `FULL_testRunOutput.json` contains all the test results as shown below.

```json
{
  "testConfiguration" : {
    "name" : "GetSamSystemStatus",
    "description" : "Call the Sam getSystemStatus multiple times. No authentication required.",
    "serverSpecificationFile" : "sam-ichang-comic-lizard-bee.json",
    "billingAccount" : null,
    "isFunctional" : false,
    "resourceFileName" : "perf/GetSamSystemStatus.json",
    "testUserFiles" : [ ],
    "server" : {
      "name" : "sam-ichang-comic-lizard-bee",
      "description" : "Sam BEE",
      "samUri" : "https://sam.fiab-ichang-comic-lizard.bee.envs-terra.bio",
      "datarepoUri" : null,
      "samResourceIdForDatarepo" : null,
      "bufferUri" : null,
      "bufferClientServiceAccountFile" : null,
      "bufferClientServiceAccount" : null,
      "workspaceManagerUri" : null,
      "externalCredentialsManagerUri" : null,
      "catalogUri" : null,
      "cluster" : null,
      "testRunnerServiceAccountFile" : "testrunner-sa.json",
      "testRunnerServiceAccount" : {
        "name" : "testrunner-perf@broad-dsde-perf.iam.gserviceaccount.com",
        "jsonKeyDirectoryPath" : "test-runner/src/main/resources/rendered",
        "jsonKeyFilename" : "testrunner-sa.json"
      },
      "testRunnerK8SServiceAccountFile" : null,
      "testRunnerK8SServiceAccount" : null,
      "deploymentScript" : {
        "name" : "",
        "scriptClass" : null,
        "parameters" : [ "{}" ]
      },
      "skipKubernetes" : true,
      "skipDeployment" : true,
      "versionScripts" : null
    },
    "kubernetes" : {
      "numberOfInitialPods" : null
    },
    "application" : {
      "maxStairwayThreads" : 20,
      "maxBulkFileLoad" : 1000000,
      "maxBulkFileLoadArray" : 1000000,
      "loadConcurrentFiles" : 80,
      "loadConcurrentIngests" : 2,
      "loadDriverWaitSeconds" : 1,
      "loadHistoryCopyChunkSize" : 1000,
      "loadHistoryWaitSeconds" : 2
    },
    "testScripts" : [ {
      "name" : "GetSamSystemStatus",
      "numberOfUserJourneyThreadsToRun" : 10,
      "userJourneyThreadPoolSize" : 2,
      "expectedTimeForEach" : 5,
      "expectedTimeForEachUnit" : "SECONDS",
      "expectedTimeForEachUnitObj" : "SECONDS",
      "description" : "GetSamSystemStatus",
      "parameters" : [ "{}" ]
    } ],
    "testUsers" : [ ],
    "disruptiveScript" : null
  },
  "testScriptResults" : [ {
    "userJourneyResults" : [ {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-1",
      "completed" : true,
      "elapsedTimeNS" : 5054807011,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-2",
      "completed" : true,
      "elapsedTimeNS" : 5054806502,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-1",
      "completed" : true,
      "elapsedTimeNS" : 4377778063,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-2",
      "completed" : true,
      "elapsedTimeNS" : 4377741988,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-2",
      "completed" : true,
      "elapsedTimeNS" : 4402714351,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-1",
      "completed" : true,
      "elapsedTimeNS" : 4402702011,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-2",
      "completed" : true,
      "elapsedTimeNS" : 4380956060,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-1",
      "completed" : true,
      "elapsedTimeNS" : 4381001615,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-2",
      "completed" : true,
      "elapsedTimeNS" : 4374119771,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    }, {
      "userJourneyDescription" : "GetSamSystemStatus",
      "threadName" : "pool-9-thread-1",
      "completed" : true,
      "elapsedTimeNS" : 4374096560,
      "exceptionWasThrown" : false,
      "exceptionStackTrace" : "java.lang.Exception: Service hanged\n\tat scripts.testscripts.GetSamSystemStatus.userJourney(GetSamSystemStatus.scala:33)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:442)\n\tat bio.terra.testrunner.runner.TestRunner.lambda$tryDoUserJourney$0(TestRunner.java:451)\n\tat bio.terra.testrunner.runner.RetryLogic.retry(RetryLogic.java:24)\n\tat bio.terra.testrunner.runner.TestRunner.tryDoUserJourney(TestRunner.java:448)\n\tat bio.terra.testrunner.runner.TestRunner.access$000(TestRunner.java:27)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:427)\n\tat bio.terra.testrunner.runner.TestRunner$UserJourneyThread.call(TestRunner.java:401)\n\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n\tat java.base/java.lang.Thread.run(Thread.java:829)\n",
      "exceptionMessage" : "Service hanged\nService hanged",
      "retryAttempts" : 2
    } ],
    "testScriptName" : "GetSamSystemStatus",
    "testScriptDescription" : "GetSamSystemStatus"
  } ],
  "testScriptResultSummaries" : [ {
    "testScriptName" : "GetSamSystemStatus",
    "testScriptDescription" : "GetSamSystemStatus",
    "elapsedTimeStatistics" : {
      "min" : 4374.09656,
      "max" : 5054.807011,
      "mean" : 4518.0723932,
      "standardDeviation" : 283.0785448998904,
      "median" : 4380.978837500001,
      "percentile95" : 5054.807011,
      "percentile99" : 5054.807011,
      "sum" : 45180.72393199999
    },
    "totalRun" : 10,
    "numCompleted" : 10,
    "numExceptionsThrown" : 0,
    "isFailure" : false
  } ],
  "versionScriptResults" : null,
  "id" : "3c7e545b-3abd-4a1c-bb9a-7e9d04971efa",
  "startTime" : 1658716629936,
  "startUserJourneyTime" : 1658716629939,
  "endUserJourneyTime" : 1658716652533,
  "endTime" : 1658716652569,
  "startTimestamp" : "2022-07-25T02:37:09.0000936Z",
  "startUserJourneyTimestamp" : "2022-07-25T02:37:09.0000939Z",
  "endUserJourneyTimestamp" : "2022-07-25T02:37:32.0000533Z",
  "endTimestamp" : "2022-07-25T02:37:32.0000569Z",
  "testSuiteName" : "FullPerf",
  "githubRunId" : null,
  "githubRepository" : null,
  "githubServerUrl" : null
}
```

### <a name="test-runner-test-user-spec"></a>Test User Specification

Many tests require test users authentication, their credentials are specified in Test User Specification files and must reside in `src/main/resources/testusers` directory.

The following screenshots show a new test class `GetProxyGroup` that requires user authentication. The additional files needed are surrounded by red boxes.

!['Test User Authentication'](https://drive.google.com/uc?export=view&id=1LpzqzfNPVSm57caLS4mkz6qN3vCMJNLK)

!['Test User Authentication'](https://drive.google.com/uc?export=view&id=194gNwa97-0s-k_THU4eSEEOY2Cy7AMNk)

First we need to update `build.sbt` by including the `Google OAuth Client` in `libraryDependencies`. Save the new `build.sbt` and reload it in `Build tool window`.

`"com.google.auth" % "google-auth-library-oauth2-http" % "1.8.1"`

`build.sbt`
```scala
import Settings._
import Testing._

lazy val root = project
  .in(file("."))
  .settings(rootSettings: _*)
  .withTestSettings

val samVersion  = "0.1-bf9c033-SNAP"
val testRunnerVersion = "0.1.6-SNAPSHOT"

lazy val testRunner = project
  .in(file("test-runner"))
  .settings(
    name := "test-runner",
    resolvers ++= commonResolvers,
    libraryDependencies ++= Seq(
      "org.broadinstitute.dsde.workbench" % "sam-client_2.13" % samVersion,
      "com.google.auth" % "google-auth-library-oauth2-http" % "1.8.1",
      "bio.terra" % "terra-test-runner" % testRunnerVersion
    )
  )

Revolver.settings
Global / excludeLintKeys += debugSettings // To avoid lint warning

javaOptions in reStart += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5050"

// When JAVA_OPTS are specified in the environment, they are usually meant for the application
// itself rather than sbt, but they are not passed by default to the application, which is a forked
// process. This passes them through to the "re-start" command, which is probably what a developer
// would normally expect.
// for some reason using ++= causes revolver not to find the main class so do the stupid map below
//javaOptions in reStart ++= sys.env.getOrElse("JAVA_OPTS", "").split(" ").toSeq
sys.env.getOrElse("JAVA_OPTS", "").split(" ").toSeq.map { opt =>
  javaOptions in reStart += opt
}

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
```

Manually create `testrunner.sh` under `scripts` directory and use the code below.

`scripts/testrunner.sh`
```bash
#!/bin/bash

set -eux

VAULT_TOKEN=${1:-$(cat "$HOME"/.vault-token)}

DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:consul-0.20.0

SAM_ACCOUNT_VAULT_PATH=secret/dsde/firecloud/qa/sam/sam-account.json
FIRECLOUD_ACCOUNT_VAULT_PATH=secret/dsde/firecloud/qa/common/firecloud-account.json

docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" \
    $DSDE_TOOLBOX_DOCKER_IMAGE \
    vault read -format=json "${SAM_ACCOUNT_VAULT_PATH}" \
    | jq -r .data > test-runner/src/main/resources/rendered/sam-qa.json

docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" \
    $DSDE_TOOLBOX_DOCKER_IMAGE \
    vault read -format=json "${FIRECLOUD_ACCOUNT_VAULT_PATH}" \
    | jq -r .data > test-runner/src/main/resources/rendered/firecloud-qa.json
```

Open a shell and run `./scripts/testrunner.sh` to download `firecloud-qa.json` into `test-runner/src/main/resources/rendered` directory.

Now manually create the following files.

* `test-runner/src/main/resources/serviceaccounts/firecloud-qa.json`
* `test-runner/src/main/resources/testusers/harry-potter-qa.json`

`test-runner/src/main/resources/serviceaccounts/firecloud-qa.json`
```json
{
  "name": "firecloud-qa@broad-dsde-qa.iam.gserviceaccount.com",
  "jsonKeyFilename": "firecloud-qa.json",
  "jsonKeyDirectoryPath": "test-runner/src/main/resources/rendered"
}
```
`test-runner/src/main/resources/testusers/harry-potter-qa.json`
```json
{
  "name": "Harry Potter",
  "userEmail": "harry.potter@quality.firecloud.org",
  "delegatorServiceAccountFile": "firecloud-qa.json"
}
```

We need a HTTP Client that supports user authentication so we will write a simple extension of the `Sam` `ApiClient` and call it `SamClient`.

`test-runner/src/main/scala/scripts/client/SamClient.scala`
```scala
package scripts.client

import bio.terra.testrunner.common.utils.AuthenticationUtils
import bio.terra.testrunner.runner.config.{ServerSpecification, TestUserSpecification}
import org.broadinstitute.dsde.workbench.client.sam.ApiClient
import com.google.auth.oauth2.{AccessToken, GoogleCredentials}
import org.slf4j.LoggerFactory

class SamClient extends ApiClient {
  val logger = LoggerFactory.getLogger(classOf[SamClient])

  @throws(classOf[Exception])
  def this(server: ServerSpecification, testUser: TestUserSpecification) {
    this()
    setBasePath(server.samUri)
    if (testUser != null) {
      val userCredential: GoogleCredentials = AuthenticationUtils
        .getDelegatedUserCredential(testUser, AuthenticationUtils.userLoginScopes)

      val accessToken: AccessToken = AuthenticationUtils.getAccessToken(userCredential)

      if (accessToken != null) setAccessToken(accessToken.getTokenValue)
      logger.info("Access token=***{}", accessToken.getTokenValue()
        .substring(accessToken.getTokenValue().length - 7))
    }
  }
}
```

The test class `GetProxyGroup` will use the `SamClient` to make HTTP requests.

`test-runner/src/main/scala/scripts/testscripts/GetProxyGroup.scala`
```scala
package scripts.testscripts

import bio.terra.testrunner.runner.TestScript
import bio.terra.testrunner.runner.config.TestUserSpecification
import org.broadinstitute.dsde.workbench.client.sam.api.{GoogleApi, UsersApi}
import org.slf4j.LoggerFactory
import scripts.client.SamClient

import java.util.List

class GetProxyGroup extends TestScript{
  val logger = LoggerFactory.getLogger(classOf[GetProxyGroup])

  @throws(classOf[Exception])
  override def setup(testUsers: List[TestUserSpecification]): Unit = {
    logger.info("setup")
  }

  @throws(classOf[Exception])
  override def userJourney(testUser: TestUserSpecification): Unit = {
    val client = new SamClient(server, testUser)
    val usersApi = new UsersApi(client)
    val googleApi = new GoogleApi(client)
    val userStatusInfo = usersApi.getUserStatusInfo()
    val proxyGroup = googleApi.getProxyGroup(userStatusInfo.getUserEmail())
    logger.info("User={}", userStatusInfo.getUserEmail())
    logger.info("Proxy Group={}", proxyGroup)
  }

  @throws(classOf[Exception])
  override def cleanup(testUsers: List[TestUserSpecification]): Unit = {
    logger.info("cleanup")
  }
}
```

We can now configure the test, let's create the config file `GetProxyGroup.json` under `test-runner/src/main/resources/configs/perf` directory. Notice that we have added the test user specification file `harry-potter-qa.json` instructing `Test Runner` to use the appropriate credentials for user authentication.

`test-runner/src/main/resources/configs/perf/GetProxyGroup.json`
```json
{
  "name": "GetProxyGroup",
  "description": "Should retrieve a user's proxy group as any user.",
  "serverSpecificationFile": "sam-ichang-comic-lizard-bee.json",
  "kubernetes": {},
  "application": {},
  "testScripts": [
    {
      "name": "GetProxyGroup",
      "numberOfUserJourneyThreadsToRun": 10,
      "userJourneyThreadPoolSize": 2,
      "expectedTimeForEach": 5,
      "expectedTimeForEachUnit": "SECONDS"
    }
  ],
  "testUserFiles": [ "harry-potter-qa.json" ]
}
```

Let's update the test suite to point to `GetProxyGroup.json`.

`test-runner/src/main/resources/suites/perf`
```json
{
  "name": "FullPerf",
  "description": "All perf tests",
  "serverSpecificationFile": "sam-ichang-comic-lizard-bee.json",
  "testConfigurationFiles": [
    "perf/GetProxyGroup.json"
  ]
}
```

Run the test suite again with the `sbt` command.

`sbt "testRunner/runMain bio.terra.testrunner.common.commands.RunTest suites/perf/FullPerf.json /tmp/scala-testrunner"`

If all goes well, you'll see the following console output.

```console
[info] welcome to sbt 1.6.2 (AdoptOpenJDK Java 11.0.11)
[info] loading global plugins from /Users/ichang/.sbt/1.0/plugins
[info] loading settings for project sam-build from plugins.sbt ...
[info] loading project definition from /Users/ichang/repos/broadinstitute/sam/project
[info] loading settings for project root from build.sbt ...
[info] set current project to sam (in build file:/Users/ichang/repos/broadinstitute/sam/)
[info] compiling 1 Scala source to /Users/ichang/repos/broadinstitute/sam/test-runner/target/scala-2.12/classes ...
[info] running bio.terra.testrunner.common.commands.RunTest suites/perf/FullPerf.json /tmp/scala-testrunner
19:13:16.513 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - ==== READING IN TEST SUITE/CONFIGURATION(S) ====
19:13:16.699 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.config.TestSuite - Parsing the test suite file as JSON
19:13:16.821 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.config.TestSuite - Parsing the test configuration file as JSON
19:13:16.835 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Found a test suite: FullPerf
19:13:16.836 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.config.TestSuite - Validating the test configurations
19:13:16.836 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.config.TestConfiguration - Validating the server, Kubernetes and application specifications
19:13:16.836 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.config.TestConfiguration - Validating the test script specifications
19:13:16.838 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.config.TestConfiguration - Validating the test user specifications
19:13:16.839 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - ==== EXECUTING TEST CONFIGURATION (1) GetProxyGroup ====
19:13:16.871 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - {
  "name" : "GetProxyGroup",
  "description" : "Should retrieve a user's proxy group as any user.",
  "serverSpecificationFile" : "sam-ichang-comic-lizard-bee.json",
  "billingAccount" : null,
  "isFunctional" : false,
  "resourceFileName" : "perf/GetProxyGroup.json",
  "testUserFiles" : [ "harry-potter-qa.json" ],
  "server" : {
    "name" : "sam-ichang-comic-lizard-bee",
    "description" : "Sam BEE",
    "samUri" : "https://sam.fiab-ichang-comic-lizard.bee.envs-terra.bio",
    "datarepoUri" : null,
    "samResourceIdForDatarepo" : null,
    "bufferUri" : null,
    "bufferClientServiceAccountFile" : null,
    "bufferClientServiceAccount" : null,
    "workspaceManagerUri" : null,
    "externalCredentialsManagerUri" : null,
    "catalogUri" : null,
    "cluster" : null,
    "testRunnerServiceAccountFile" : "testrunner-sa.json",
    "testRunnerServiceAccount" : {
      "name" : "testrunner-perf@broad-dsde-perf.iam.gserviceaccount.com",
      "jsonKeyDirectoryPath" : "test-runner/src/main/resources/rendered",
      "jsonKeyFilename" : "testrunner-sa.json"
    },
    "testRunnerK8SServiceAccountFile" : null,
    "testRunnerK8SServiceAccount" : null,
    "deploymentScript" : {
      "name" : "",
      "scriptClass" : null,
      "parameters" : [ "{}" ]
    },
    "skipKubernetes" : true,
    "skipDeployment" : true,
    "versionScripts" : null
  },
  "kubernetes" : {
    "numberOfInitialPods" : null
  },
  "application" : {
    "maxStairwayThreads" : 20,
    "maxBulkFileLoad" : 1000000,
    "maxBulkFileLoadArray" : 1000000,
    "loadConcurrentFiles" : 80,
    "loadConcurrentIngests" : 2,
    "loadDriverWaitSeconds" : 1,
    "loadHistoryCopyChunkSize" : 1000,
    "loadHistoryWaitSeconds" : 2
  },
  "testScripts" : [ {
    "name" : "GetProxyGroup",
    "numberOfUserJourneyThreadsToRun" : 10,
    "userJourneyThreadPoolSize" : 2,
    "expectedTimeForEach" : 5,
    "expectedTimeForEachUnit" : "SECONDS",
    "expectedTimeForEachUnitObj" : "SECONDS",
    "description" : "GetProxyGroup",
    "parameters" : [ "{}" ]
  } ],
  "testUsers" : [ {
    "name" : "Harry Potter",
    "userEmail" : "harry.potter@quality.firecloud.org",
    "delegatorServiceAccountFile" : "firecloud-qa.json",
    "delegatorServiceAccount" : {
      "name" : "firecloud-qa@broad-dsde-qa.iam.gserviceaccount.com",
      "jsonKeyDirectoryPath" : "test-runner/src/main/resources/rendered",
      "jsonKeyFilename" : "firecloud-qa.json"
    }
  } ],
  "disruptiveScript" : null
}
19:13:16.875 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Deployment: Skipping deployment
19:13:16.875 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Kubernetes: Skipping Kubernetes configuration post-deployment
19:13:16.875 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Version: Skipping version determination
19:13:16.876 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Fetching instance of each class, setting billing account and parameters
19:13:16.876 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Calling the setup methods
19:13:16.876 [sbt-bg-threads-1] INFO scripts.testscripts.GetProxyGroup - setup
19:13:16.876 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Creating a thread pool for each TestScript and kicking off the user journeys
19:13:16.877 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Waiting until all threads either finish or time out
19:13:17.564 [pool-9-thread-1] INFO scripts.client.SamClient - Access token=***qZw0214
19:13:17.564 [pool-9-thread-2] INFO scripts.client.SamClient - Access token=***ydw0214
19:13:18.014 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:18.014 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:18.014 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:18.014 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:18.148 [pool-9-thread-1] INFO scripts.client.SamClient - Access token=***SUQ0214
19:13:18.231 [pool-9-thread-2] INFO scripts.client.SamClient - Access token=***pUQ0214
19:13:18.522 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:18.522 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:18.614 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:18.615 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:18.637 [pool-9-thread-1] INFO scripts.client.SamClient - Access token=***BUQ0214
19:13:18.713 [pool-9-thread-2] INFO scripts.client.SamClient - Access token=***QUQ0214
19:13:19.013 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:19.013 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:19.092 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:19.092 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:19.133 [pool-9-thread-1] INFO scripts.client.SamClient - Access token=***4UQ0214
19:13:19.240 [pool-9-thread-2] INFO scripts.client.SamClient - Access token=***xdw0214
19:13:19.497 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:19.497 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:19.611 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:19.612 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:19.617 [pool-9-thread-1] INFO scripts.client.SamClient - Access token=***aZw0214
19:13:19.743 [pool-9-thread-2] INFO scripts.client.SamClient - Access token=***XZw0214
19:13:19.996 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:19.997 [pool-9-thread-1] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:20.110 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - User=harry.potter@quality.firecloud.org
19:13:20.110 [pool-9-thread-2] INFO scripts.testscripts.GetProxyGroup - Proxy Group=eb6fPROXY_2658347593447d41f898b@quality.firecloud.org
19:13:20.110 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Compiling the results from all thread pools
19:13:20.135 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test Scripts: Calling the cleanup methods
19:13:20.136 [sbt-bg-threads-1] INFO scripts.testscripts.GetProxyGroup - cleanup
19:13:20.136 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Deployment: Skipping deployment teardown
19:13:20.136 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - ==== TEST RUN RESULTS (1) GetProxyGroup ====
19:13:20.141 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - {
  "id" : "d565641a-4432-4ee8-8897-9257f268876d",
  "startTime" : 1658790796875,
  "startUserJourneyTime" : 1658790796876,
  "endUserJourneyTime" : 1658790800110,
  "endTime" : 1658790800136,
  "testScriptResultSummaries" : [ {
    "testScriptName" : "GetProxyGroup",
    "testScriptDescription" : "GetProxyGroup",
    "elapsedTimeStatistics" : {
      "min" : 477.323829,
      "max" : 1135.000024,
      "mean" : 634.7685503,
      "standardDeviation" : 265.78516604744215,
      "median" : 503.89229750000004,
      "percentile95" : 1135.000024,
      "percentile99" : 1135.000024,
      "sum" : 6347.685503
    },
    "totalRun" : 10,
    "numCompleted" : 10,
    "numExceptionsThrown" : 0,
    "isFailure" : false
  } ],
  "startTimestamp" : "2022-07-25T23:13:16.0000875Z",
  "startUserJourneyTimestamp" : "2022-07-25T23:13:16.0000876Z",
  "endUserJourneyTimestamp" : "2022-07-25T23:13:20.0000110Z",
  "endTimestamp" : "2022-07-25T23:13:20.0000136Z",
  "testSuiteName" : "FullPerf",
  "githubRunId" : null,
  "githubRepository" : null,
  "githubServerUrl" : null
}
19:13:20.142 [sbt-bg-threads-1] DEBUG bio.terra.testrunner.runner.TestRunner - outputDirectoryCreated /tmp/scala-testrunner/GetProxyGroup_d565641a-4432-4ee8-8897-9257f268876d: true
19:13:20.142 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test run results written to directory: /tmp/scala-testrunner/GetProxyGroup_d565641a-4432-4ee8-8897-9257f268876d
19:13:20.156 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Rendered test configuration written to file: RENDERED_testConfiguration.json
19:13:20.161 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - All user journey results written to file: RAWDATA_userJourneyResults.json
19:13:20.162 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test run summary written to file: SUMMARY_testRun.json
19:13:20.172 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Test run full output written to file: FULL_testRunOutput.json
19:13:20.173 [sbt-bg-threads-1] INFO bio.terra.testrunner.runner.TestRunner - Version script result written to file: ENV_versionResult.json
PASSED test configurations
GetProxyGroup

FAILED test configurations

[success] Total time: 13 s, completed Jul 25, 2022, 7:13:25 PM
```

We have just successfully ported Scala test [SamApiSpec](https://github.com/broadinstitute/sam/blob/develop/automation/src/test/scala/org/broadinstitute/dsde/workbench/sam/api/SamApiSpec.scala#L152-L180) using `Test Runner Framework`.

You can now also refactor the previous `GetSamSystemStatus.scala` to use `SamClient`. Notice that we explicitly pass a `null` to `SamClient` since user authentication is not required.

`test-runner/src/main/scala/scripts/testscripts/GetSamSystemStatus.scala`
```scala
package scripts.testscripts

import bio.terra.testrunner.runner.TestScript
import bio.terra.testrunner.runner.config.TestUserSpecification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi
import org.slf4j.LoggerFactory
import scripts.client.SamClient

import java.lang.reflect.Type
import java.util.HashMap
import java.util.List

class GetSamSystemStatus extends TestScript {
  val logger = LoggerFactory.getLogger(classOf[GetSamSystemStatus])

  var counter = ThreadLocal.withInitial[Int](() => 0)

  @throws(classOf[Exception])
  override def setup(testUsers: List[TestUserSpecification]): Unit = {
    logger.info("setup")
  }

  @throws(classOf[Exception])
  override def userJourney(testUser: TestUserSpecification): Unit = {
    val client = new SamClient(server, null)
    val api = new StatusApi(client)
    counter.set(counter.get + 1)
    if (counter.get <= 2) {
      logger.info("Attempt={}", counter.get)
      throw new Exception("Service hanged")
    }
    val status = api.getSystemStatus()
    logger.info("systems: {}", status.getSystems())
    val gson = new Gson()
    val t : Type = new TypeToken[HashMap[String, HashMap[String, Boolean]]]() {}.getType()
    val systems : HashMap[String, HashMap[String, Boolean]] = gson.fromJson(api.getSystemStatus().getSystems().toString(), t)

    logger.info("Attempt={} {}", counter.get, if (status.getOk()) "passed" else "failed")
    systems.forEach {
      case(k, v) => {
        v.get("ok") match {
          case true => logger.info("System '{}' passed", k)
          case false => logger.info("System '{}' failed", k)
        }
      }
    }
    counter.set(0)
  }

  @throws(classOf[Exception])
  override def cleanup(testUsers: List[TestUserSpecification]): Unit = {
    logger.info("cleanup")
  }
}
```

### Summary

The following table illustrates sample code for version `0.1.6-SNAPSHOT` of `Test Runner Framework`. The Releases section has the details on all available versions.

|Build Tool|Dependency Declaration| Plugin Declaration |
|----------|----------------------|--------------------|
| Maven    | <pre>&lt;dependency&gt;<br>  &lt;groupId&gt;bio.terra&lt;/groupId&gt;<br>  &lt;artifactId&gt;terra-test-runner&lt;/artifactId&gt;<br>  &lt;version&gt;0.1.6-SNAPSHOT&lt;/version&gt;<br>&lt;/dependency&gt;</pre> | Plugin unavailable |
| Gradle  | <pre>dependencies {<br>  ext {<br>    testRunnerVersion = "0.1.6-SNAPSHOT"<br>  }<br>  implementation "bio.terra:terra-test-runner:${testRunnerVersion}"<br>}</pre> | <pre>plugins {<br>  id 'bio.terra.test-runner-plugin'<br>}</pre> |
| Sbt     | <pre>val testRunnerVersion = "0.1.6-SNAPSHOT"<br>libraryDependencies += "bio.terra" % "terra-test-runner" % testRunnerVersion</pre> | Plugin unavailable |

The `Test Runner Framework` contains a `Gradle Plugin` implementation to provide a succinct way of launching useful tasks when using `Gradle` build tool CLI. Plugins for other build tools will be available in the future. The `Test Runner Framework Plugin` supports the following tasks

| Task Name | Description | Gradle | Maven | Sbt |
|-----------|-------------|--------|-------|-----|
| PrintHelp | Output available task list to console | <pre>./gralew printHelp</pre> | <pre>mvn compile exec:java -Dexec.mainClass="bio.terra.testrunner.common.commands.PrintHelp"</pre> | <pre>sbt runMain bio.terra.testrunner.common.commands.PrintHelp</pre> |
| RunTest   | Launch Test Runner | <pre>./gradlew runTest --args="suites/FullIntegration.json /tmp" --scan</pre> | <pre>mvn compile exec:java -Dexec.mainClass="bio.terra.testrunner.common.commands.RunTest" -Dexec.args="suites/FullIntegration.json /tmp"</pre> | <pre>sbt runMain bio.terra.testrunner.common.commands.RunTest "suites/FullIntegration.json" "/tmp"</pre> |
| UploadResults | Upload test results to GCS bucket | <pre>./gradlew uploadResults --args="CompressDirectoryToTerraKernelK8S.json /tmp" --scan</pre> | <pre>mvn compile exec:java -Dexec.mainClass="bio.terra.testrunner.common.commands.UploadResults" -Dexec.args="CompressDirectoryToTerraKernelK8S.json /tmp"</pre> | <pre>sbt runMain bio.terra.testrunner.common.commands.UploadResults "CompressDirectoryToTerraKernelK8S.json" "/tmp"</pre> |


