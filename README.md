# terra-test-runner

This library is used for running tests using Terra service client libraries.

Initial proposal document:

* [Performance Testing Infrastructure Proposal](https://docs.google.com/document/d/11PZIXZwOyd394BFOlBsDjOGjZdC-jwTr_n92nTJvFxw)

Jump to sections below:

<!-- TOC -->
* [terra-test-runner](#terra-test-runner)
  * [Terminology](#terminology)
      * [Test Script](#test-script)
      * [Test Configuration](#test-configuration)
      * [Test Run](#test-run)
      * [Test Runner](#test-runner)
      * [Test Suite](#test-suite)
  * [Execute a test run](#execute-a-test-run)
      * [Results are written to files](#results-are-written-to-files)
      * [Override the server from the command line](#override-the-server-from-the-command-line)
      * [Lock server during test run](#lock-server-during-test-run)
      * [Run against a local server](#run-against-a-local-server)
      * [Use a local Data Repo client JAR file](#use-a-local-data-repo-client-jar-file)
      * [Set the directory where SA key files live](#set-the-directory-where-sa-key-files-live)
      * [Running Resiliency Tests](#running-resiliency-tests)
  * [Collect measurements generated by the server](#collect-measurements-generated-by-the-server)
      * [Generated during a time interval](#generated-during-a-time-interval)
      * [Generated during a test run](#generated-during-a-test-run)
  * [Example workflows](#example-workflows)
      * [Execute test run and collect measurements](#execute-test-run-and-collect-measurements)
      * [Collect measurements for a time interval](#collect-measurements-for-a-time-interval)
      * [Execute a test suite](#execute-a-test-suite)
  * [Write a new test](#write-a-new-test)
      * [Add a new test configuration](#add-a-new-test-configuration)
      * [Add a new test script](#add-a-new-test-script)
      * [Add a new server specification](#add-a-new-server-specification)
      * [Add a new deployment script](#add-a-new-deployment-script)
      * [Add a new test user specification](#add-a-new-test-user-specification)
      * [Add a new service account specification](#add-a-new-service-account-specification)
  * [Add a new test suite](#add-a-new-test-suite)
  * [Collect a new measurement type](#collect-a-new-measurement-type)
      * [Add a new measurement collection script](#add-a-new-measurement-collection-script)
      * [Add a new measurement list](#add-a-new-measurement-list)
  * [Development](#development)
      * [Requirements](#requirements)
      * [Package structure](#package-structure)
      * [BigQuery Schema](#bigquery-schema)
      * [Debug a test configuration or script](#debug-a-test-configuration-or-script)
      * [Dependencies](#dependencies)
      * [Linters](#linters)
      * [Testing plugin changes locally](#testing-plugin-changes-locally)
      * [Publish to Artifactory](#publish-to-artifactory)
  * [Troubleshooting](#troubleshooting)
<!-- TOC -->

## Terminology

This testing infrastructure aims to separate the test from the system
configuration that it runs in. This is so that it's easier to profile variations
of API calls and environment without rewriting a lot of test code.

#### Test Script

A test script contains several API calls to perform some user action(s) or user
journey. They can be broken down into three parts:

1. Setup (e.g. Create a dataset)
2. User Journey (e.g. Make one bulk file load API call)
3. Cleanup (e.g. Delete dataset)

The User Journey part contains the API call(s) that we want to profile and it
may be scaled to run multiple journeys in parallel. By contrast, the Setup and
Cleanup parts contain the API call(s) that we do not want to profile and will
not be scaled to run multiple in parallel. For the example above, we could run
multiple bulk file loads in parallel to test the performance, but the dataset
creation and deletion would be done only once.

#### Test Configuration

A test configuration describes how to set up the test environment, which test
script(s) to run, how to scale the test script(s), and how to add stress into
the system.

The environment specification includes settings for:

* Deployment (e.g. developer’s dev namespace, performance testing Kubernetes
  cluster, how to deploy there)
* Kubernetes (e.g. initial size of replica set)
* Application (e.g. Stairway thread pool size, maximum number of bulk file
  loads)

The scripts to run specification includes the:

* Set of different User Journeys to run (e.g. bulk file load & DRS lookup, just
  DRS lookups) and any parameters they require (e.g. source/destination bucket
  region, number of files per bulk load) specified as key-value pairs.
* Number of each User Journey to run (e.g. 500 bulk file loads & 300 DRS
  lookups, 1000 DRS lookups)
* Timeout of client threads (e.g. each bulk file load should take < 15 minutes,
  each DRS lookup should take < 30 seconds)

The scaling specification includes:

* Number of different users making the User Journey calls.

Retry settings (0.1.6-SNAPSHOT):

* Added `maxRetries` and `timeToWait` properties.
  * `maxRetries`: maximum number of retries per user journey thread (default: 3)
  * `timeToWait`: wait time (milliseconds) till next retry (default: 2000 ms)

The application specification includes (this section applies to resiliency test
type)

* Default values of component label key-value pair for locating a Terra
  application.
    * The default component label key-value pair is set to
        * { "app.kubernetes.io/component": "api" }

* The default setting may not apply to your application, In that case, you need
  to configure the following fields in the application specification within the
  test configuration
    * componentLabel
    * apiComponentLabel

**This section will be updated as more pieces of the test configuration are
implemented.** See the
[Performance Testing Infrastructure Proposal](https://docs.google.com/document/d/11PZIXZwOyd394BFOlBsDjOGjZdC-jwTr_n92nTJvFxw)
for more details on the desired end goal.

#### Test Run

A test run is a single execution of a test configuration.

#### Test Runner

The test runner executes test configurations. The steps involved in each test
run are:

* Re-deploy the API (i.e. Helm delete then upgrade) with the application
  properties specified by the configuration.
* Modify the Kubernetes environment, as specified by the configuration.
* Run the Setup for each test script.
* Create a client thread pool for each test script specification.
* Kick off some number of threads, each running one User Journey, as specified
  by the configuration.
* Wait until all threads either finish or time out.
* Run the Cleanup for each test script.
* Teardown the API deployment, if applicable.

The implementation of the test runner is where the bulk of the testing
infrastructure code lives.

#### Test Suite

A test suite is a collection of test configurations that have some similar
purpose. For example, a smoke test suite to detect major performance problems
quickly or a very long running suite to detect possible memory leaks. The test
configurations are run serially.

## Execute a test run

Find a test configuration or suite to execute. Each configuration is a JSON file
in the resources/configs directory.

Call the Gradle runTest task and pass it the name of the test configuration or
suite to execute, along with the output directory to write the results to.

```
./gradlew runTest --args="configOrSuiteFileName outputDirectoryName"
  configOrSuiteFileName = file name of the test configuration or suite JSON file
  outputDirectoryName = name of the directory where the results will be written
```

#### Results are written to files

For test configurations, the results are written to the output directory
specified in the runTest Gradle command.

For test suites, the results are written to sub-directories of the output
directory specified in the runTest Gradle command.

#### Override the server from the command line

The environment variable TEST_RUNNER_SERVER_SPECIFICATION_FILE optionally
overrides the server configuration specified by either the test suite or
configuration. The server specification is determined by the following, in
order:

1. environment variable
2. test suite server property (if running a test suite)
3. test configuration server property

```
export TEST_RUNNER_SERVER_SPECIFICATION_FILE="mmdev.json"
./gradlew runTest --args="configOrSuiteFileName outputDirectoryName"
```

#### Lock server during test run

The Test Runner has multiple functionalities that directly manipulate the
specified server: it can deploy specific settings and manipulate kubernetes.
When you are testing, we want to assert that your test one is the only one
running on the specified server. We accomplish this by creating kubernetes
secrets specific to the namespace of the designated server. Switching to this "
locking" mode is as simple as switching the gradle command from "runTest" to "
lockAndRunTest,"
as demonstrated below. Note: We currently require the
TEST_RUNNER_SERVER_SPECIFICATION_FILE environment variable to be set, and this
overrides any server specification in test and suite configs.

```
export TEST_RUNNER_SERVER_SPECIFICATION_FILE="mmdev.json"
./gradlew lockAndRunTest --args="configOrSuiteFileName outputDirectoryName"
  configOrSuiteFileName = file name of the test configuration or suite JSON file
  outputDirectoryName = name of the directory where the results will be written
```

It is possible that the locks can get out of sync, so if you think you've
incorrectly hit an "unable to lock" error, you can you the following commands to
directly lock or unlock the namespace. Make sure the
TEST_RUNNER_SERVER_SPECIFICATION_FILE environment variable is set.

```
export TEST_RUNNER_SERVER_SPECIFICATION_FILE="mmdev.json"
./gradlew lockNamespace
./gradlew unlockNamespace
```

#### Run against a local server

There is a localhost.json server specification file in the resources/server
directory. This file contains a filepath to the top-level directory of the
jade-data-repo Git repository. Executing a test against this configuration, will
start a local Data Repo server by executing the Gradle bootRun task from that
directory. This is useful for debugging or testing local server code changes.

You need to modify the path for your own machine. See
deploymentScript.parameters (version 0.1.2-SNAPSHOT or below) or deploymentScript.parametersMap (version 0.1.3-SNAPSHOT or above).

For version up to 0.1.2-SNAPSHOT
```json
{
  "name": "localhost",
  "description": "Server running locally. Supports launching the server in a separate process. Does not support modifying Kubernetes post-deployment.",
  "datarepoUri": "http://localhost:8080/",
  "samUri": "https://sam.dsde-dev.broadinstitute.org",
  "samResourceIdForDatarepo": "broad-jade-dev",
  "deploymentScript": {
    "name": "LaunchLocalProcess",
    "parameters": {
      "tdr-file-path": "file:///Users/marikomedlock/Workspaces/jade-data-repo/"
    }
  },
  "skipDeployment": false,
  "skipKubernetes": true
}
```
For version up to 0.1.3-SNAPSHOT or above
```json
{
  "name": "localhost",
  "description": "Server running locally. Supports launching the server in a separate process. Does not support modifying Kubernetes post-deployment.",
  "datarepoUri": "http://localhost:8080/",
  "samUri": "https://sam.dsde-dev.broadinstitute.org",
  "samResourceIdForDatarepo": "broad-jade-dev",
  "deploymentScript": {
    "name": "LaunchLocalProcess",
    "parametersMap": {
      "tdr-file-path": "file:///Users/marikomedlock/Workspaces/jade-data-repo/"
    }
  },
  "skipDeployment": false,
  "skipKubernetes": true
}
```

#### Use a local Data Repo client JAR file

The version of the Data Repo client JAR file is specified in the build.gradle
file in this sub-project. This JAR file is fetched from the Broad Institute
Maven repository. You can override this to use a local version of the Data Repo
client JAR file by specifying a Gradle project property, either with a command
line argument

`./gradlew -Pdatarepoclientjar=/Users/marikomedlock/Workspaces/jade-data-repo/datarepo-client/build/libs/datarepo-client-1.0.39-SNAPSHOT.jar run --args="configs/BasicUnauthenticated.json`

or an environment variable.

```
export ORG_GRADLE_PROJECT_datarepoclientjar=../datarepo-client/build/libs/datarepo-client-1.0.39-SNAPSHOT.jar
./gradlew runTest --args="configs/BasicUnauthenticated.json /tmp/TestRunnerResults"
```

This is useful for debugging or testing local server code changes that affect
the generated client library (e.g. new API endpoint). You can generate the Data
Repo client library with the Gradle assemble task of the datarepo-client
sub-project.

```
cd /Users/marikomedlock/Workspaces/jade-data-repo/datarepo-client
../gradlew clean assemble
ls -la ./build/libs/*jar
```

#### Set the directory where SA key files live

The Test Runner looks for service account key files in the directory specified
in the service account JSON file
(e.g. jade-k8-sa.json). You can override this directory with an environment
variable.

```
export TEST_RUNNER_SA_KEY_DIRECTORY_PATH="/github/workspace"
./gradlew runTest --args="configs/BasicUnauthenticated.json /tmp/TestRunnerResults"
```

#### Running Resiliency Tests

***Background***

Test Runner Framework supports resiliency tests in addition to Integration,
Performance, and Connected tests. As a premier genomic platform for biomedical
research, the [*Terra.Bio*](https://terra.bio) migration to *PaaS* or Cloud
infrastructures (GCP, AWS, Azure) to create the new *MCTerra* platform, is a key
step to advance the field of genomic science. Without the migration and data
crunching capability that comes with the migration, it would be difficult for
researchers to develop, experiment with and evaluate next generation *SOTA*
algorithms for genomic computing at scale. Our goal is to ensure that the *
MCTerra* platform continues to function and scale properly as demand continues
to increase.

Given the above context, *Containerization* has become the *de facto* go to
paradigm for managing shared cloud resources the *MCTerra* platform requires
during its lifecycles. Although autoscaling and dynamic load balancing are some
of the key techniques for managing shared cloud resources, policies on how these
techniques should apply are often set by *development*, *devOps*, *QA* managers
iteratively over the course of the software lifecycle. Without the proper tool
to analyze a containerized application, stakeholders will be forced to set these
policies in *ad hoc* manners, risking either insufficient resources to meet
demand or too many idling resources. Resiliency testing fills the gap by
generating metrics that stakeholders can use to understand the performance of
containerized applications and make comparisons across cloud vendors. Resiliency
tests can trigger additional logging in targeted *MCTerra* service components
for further analysis.

***What is a resiliency test?***

Test Runner provides the framework to target containerized applications with
specific loads while at the same time controlling the cloud resources given to
the containerized applications. During a resiliency test flight, the framework
spawns concurrent threads to scale cloud resources up and down while delivering
load to the target *MCTerra* service components at scale according to user
specifications. Resiliency tests can be integrated with a CI/CD pipeline such as
GitHub Action Workflows, or they can be run locally for debugging purposes.

Test Runner Framework supports resiliency tests on a ***namespaced*** test
environment. The following discussion assumes a valid namespace already exists
in a Kubernetes cluster. Please refer
to [Creating the namespace](https://github.com/DataBiosphere/terra/blob/main/docs/dev-guides/personal-environments.md#step-3-creating-the-namespace)
for more details about namespace creation in *MCTerra*.

***Requirements on running resiliency test***

At a high level, running resiliency tests within namespaces require a set of
permissions to manipulate cluster resources with Kubernetes API. These
permissions are namespace scoped, no resiliency tests will have cluster-wide
access.

The required namespace permissions are specified in the 3 manifest templates
which comes with the Test Runner Library distribution.
The `setup-k8s-testrunner.sh` script templates the formation of the actual
manifests for deploying to a namespace. The `setup-k8s-testrunner.sh` script
also carries out the following functions:

* Provision the Kubernetes Service Account, RBAC Role and RoleBinding for Test
  Runner.
* Export credentials of the Test Runner Kubernetes Service Account to Vault.

***Setting up existing namespaces for resiliency tests***

To set up a namespace for Test Runner resiliency tests, simply run the command
as provided in the following example (`terra-zloery` namespace for example).

The first argument is the `kubectl context` mentioned elsewhere in this
document.

The second argument is the Terra namespace (without the `terra-` prefix).Without

The third argument is just some text to describe the application itself.

```shell script
$ ./setup-k8s-testrunner.sh gke_terra-kernel-k8s_us-central1-a_terra-integration zloery workspacemanager
```

In summary, the script automatically templates in the
variables `__KUBECONTEXT__, __NAMESPACE__, __APP__` based on the 3 arguments
presented above and creates the necessary namespace objects in Kubernetes that
enables Test Runner to control the namespace. Please follow
this [link](https://github.com/DataBiosphere/terra-test-runner/blob/main/setup-k8s-testrunner.sh)
to find out more details about this script.

The above script consumes the following template manifest files representing
objects in Kubernetes namespace. There is no need to apply these template files
manually.

<details>

<summary>testrunner-k8s-serviceaccount.yml.template</summary>

```yaml
# Do not modify this template file.

# This template file is used for setting a Test Runner K8S Service Account
# for running resiliency tests in a namespace.
#
# This template file is to be used in conjunction with the other template files
#
#   testrunner-k8s-role.yml.template
#   testrunner-k8s-rolebinding.yml.template
#
# within an automation pipeline and is not meant to be run separately or manually.

apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    app.kubernetes.io/component: __APP__
  name: testrunner-k8s-sa
  namespace: terra-__NAMESPACE__
```

<summary>testrunner-k8s-role.yml.template</summary>

```yaml
# Do not modify this template file.

# This template file is used for setting a Test Runner privileged RBAC role
# for running resiliency tests in a namespace.
#
# This template file is to be used in conjunction with the other template files
#
#   testrunner-k8s-sa.yml.template
#   testrunner-k8s-rolebinding.yml.template
#
# within an automation pipeline and is not meant to be run separately or manually.

apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: testrunner-k8s-role
  # A k8s namespace: e.g. terra-wsmtest, terra-ichang.
  # Avoid using default or system namespaces such as kube-system.
  namespace: terra-__NAMESPACE__
  labels:
    app.kubernetes.io/component: __APP__
rules:
- apiGroups: [ "" ]
  resources: [ "pods", "pods/exec" ]
  verbs: [ "get", "list", "watch", "delete", "patch", "create", "update" ]
- apiGroups: [ "extensions", "apps" ]
  resources: [ "deployments", "deployments/scale" ]
  verbs: [ "get", "list", "watch", "delete", "patch", "create", "update" ]
```

<summary>testrunner-k8s-rolebinding.yml.template</summary>

```yaml
# Do not modify this template file.

# This template file is used for binding a Test Runner K8S Service Account
# to a privileged RBAC role for running resiliency tests in a namespace.
#
# This template file is to be used in conjunction with the other template files
#
#   testrunner-k8s-sa.yml.template
#   testrunner-k8s-role.yml.template
#
# within an automation pipeline and is not meant to be run separately or manually.

apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: testrunner-k8s-sa-rolebinding
  # A k8s namespace: e.g. terra-wsmtest, terra-ichang.
  # Avoid using default or system namespaces such as kube-system.
  namespace: terra-__NAMESPACE__
  labels:
    app.kubernetes.io/component: __APP__
subjects:
# Authorize In-Cluster Service Account
- kind: ServiceAccount
  name: testrunner-k8s-sa
  namespace: terra-__NAMESPACE__
roleRef:
  kind: Role
  name: testrunner-k8s-role
  apiGroup: rbac.authorization.k8s.io
```

</details>

***Rendering credentials for resiliency tests***

The Kubernetes credentials stored in Vault needs to be rendered by means of
the `./render-k8s-config.sh` script in the application repository before kicking
off resiliency tests.

Using namespace `terra-zloery` as example:

```shell script
$ ./render-k8s-config.sh zloery
```

Now the namespace (`terra-zloery` in above example) is ready for resiliency
testing through Test Runner Framework.

## Collect measurements generated by the server

Find the list of measurements to collect. Each list is a JSON file in the
resources/measurementlists directory. There are 2 options for how to call the
collectMeasurements task.

#### Generated during a time interval

Call the Gradle collectMeasurements task and pass it the list of measurements to
collect, the output directory to write the results to, the server to query, and
the start and end timestamps for the time interval.

```
./gradlew collectMeasurements --args="measurementListFileName outputDirectoryName serverFileName startTimestamp endTimestamp"
  measurementListFileName = file name of the measurement list JSON file
  outputDirectoryName = name of the directory where the results will be written
  serverFileName = name of the server JSON file
  startTimestamp = start of the interval; format must be yyyy-mm-dd hh:mm:ss[.fffffffff] (UTC timezone)
  endTimestamp = end of the interval; format must be yyyy-mm-dd hh:mm:ss[.fffffffff] (UTC timezone)
```

#### Generated during a test run

Call the Gradle collectMeasurements task and pass it the list of measurements to
collect, and the output directory to write the results to. If the output
directory is the same one where a test run results are written, then it will
pull the start and end timestamps from the summary file for the test run.

```
./gradlew collectMeasurements --args="measurementListFileName outputDirectoryName"
  measurementListFileName = file name of the measurement list JSON file
  outputDirectoryName = name of the same directory that contains the Test Runner results
```

## Example workflows

#### Execute test run and collect measurements

Execute a test configuration and collect the measurements generated by the
server during the run.

```
  ./gradlew runTest --args="configs/basicexamples/BasicUnauthenticated.json /tmp/TestRunnerResults"
  ./gradlew collectMeasurements --args="BasicKubernetes.json /tmp/TestRunnerResults"
```

#### Collect measurements for a time interval

Collect the measurements generated by the server for a particular time interval.

```
  ./gradlew collectMeasurements --args="AllMeasurements.json /tmp/TestRunnerResults mmdev.json '2020-08-20 13:18:34' '2020-08-20 13:18:35.615628881'"
```

#### Execute a test suite

Execute a set of test configurations.

```
  ./gradlew runTest --args="suites/BasicSmoke.json /tmp/TestRunnerResults"
```

## Write a new test

#### Add a new test configuration

A test configuration is an instance of the TestConfiguration POJO class,
serialized into JSON and saved in the resources/configs directory. Below are the
available fields:

* name: Name of the configuration
* description: (optional) Description of the configuration
* serverSpecificationFile: Name of a file in the resources/servers directory
  that specifies the server to test against
* billingAccount: Google billing account to use
* kubernetes: Kubernetes-related settings that will be set after deploying the
  application and before executing any tests
    * numberOfInitialPods: (optional) Initial number of pods, defaults to 1
* application: Application-related settings that will be set before deploying
  the application and executing any tests
    * maxStairwayThreads: (optional) defaults to 20
    * maxBulkFileLoad: (optional) defaults to 1000000
    * loadConcurrentFiles: (optional) defaults to 80
    * loadConcurrentIngests: (optional) defaults to 2
    * inKubernetes: (optional) defaults to false
    * loadHistoryCopyChunkSize: (optional) defaults to 1000
    * loadHistoryWaitSeconds: (optional) defaults to 2
    * loadDriverWaitSeconds: (optional) defaults to 1
* testScripts: List of test script specifications (i.e. instance of the
  TestScriptSpecification POJO class, serialized into JSON). Each specification
  should include the below required fields:
    * name: Name of the test script class to run
    * numberOfUserJourneyThreadsToRun: Integer number of user journeys to run
    * userJourneyThreadPoolSize: Integer number of user journeys to run in
      parallel (i.e. size of the thread pool)
    * expectedTimeForEach: Integer number of time units indicating the maximum
      amount of time a user journey thread will be allowed to execute.
    * expectedTimeForEachUnit: String representation of the Java TimeUnit
      class (e.g. MILLISECONDS, SECONDS, MINUTES)
* testUserFiles: List of names of files in the resources/testusers directory
  that specify the users whose crendentials will be used to run the test scripts

#### Add a new test script

A test script is a sub-class of the TestScript base class. It specifies the
setup and cleanup methods that are run once at the beginning and end of a test
run, respectively. It also specifies the userJourney method, which will be
launched in multiple threads in parallel, as specified by the test
configuration.

#### Add a new server specification

A server specification is an instance of the ServerSpecification POJO class,
serialized into JSON and saved in the resources/servers directory. Below are the
available fields:

* name: Name of the server
* description: (optional) Description of the server
* uri: URI of the Data Repo instance
* clusterName: Name of the Kubernetes cluster where the Data Repo instance is
  deployed
* clusterShortName: Name of the cluster, stripped of the region and project
  qualifiers
* region: Region where the cluster is running
* zone: Zone where the cluster is running
* project: Google project under which the cluster is running
* componentLabel: A Kubernetes metadata label key used to identify the service
  deployment
* apiComponentLabel: The value associated with the `componentLabel`, defaults
  to `api` if unspecified.
* namespace: (optional) Name of the Kubernetes namespace where the Data Repo
  instance is deployed
* deploymentScript: Name of the deployment script class to run. Only required if
  skipDeployment is false
* skipDeployment: (optional) true to skip the deployment script, default is
  false
* skipKubernetes: (optional) true to skip the post-deployment Kubernetes
  modifications, default is false

#### Add a new deployment script

A deployment script is a sub-class of the DeploymentScript base class. It
specifies the deploy, waitForDeployToFinish, and optional teardown methods that
are run once at the beginning and end of a test run, respectively.

#### Add a new test user specification

A test user specification is an instance of the TestUserSpecification POJO
class, serialized into JSON and saved in the resources/testusers directory.
Below are the required fields:

* name: Name of the test user
* userEmail: Email of the test user
* delegatorServiceAccountFile: Name of a file in the resources/serviceaccounts
  directory that specifies the service account with permission to fetch
  domain-wide delegated credentials for the test user

All test users must be registered in Terra and there must be a service account
that can fetch domain-wide delegated credentials for the user. Jade has already
setup test users (see src/main/resources/application-integrationtest.properties)
and the jade-k8-sa service account to delegate for them. It's probably easiest
to reuse one of these test users when adding new tests.

#### Add a new service account specification

A service account specification is an instance of the
ServiceAccountSpecification POJO class, serialized into JSON and saved in the
resources/serviceaccounts directory. Below are the required fields:

* name: Name of the service account
* jsonKeyFilePath: JSON-formatted file that includes the client ID and private
  key

The service account email is stored in the JSON-formatted file.

The JSON key file for the jade-k8-sa service account match the paths used by the
render-configs script in the main datarepo project. Jade stores these files in
Vault and uses the script to fetch them locally for each test run.

## Add a new test suite

A test suite is an instance of the TestSuite POJO class, serialized into JSON
and saved in the resources/suites directory. Below are the available fields:

* name: Name of the test suite
* description: (optional) Description of the test suite
* serverSpecificationFile: Name of a file in the resources/servers directory
  that specifies the server to test against
* testConfigurationFiles: List of names of files in the resources/configs
  directory that specify the test configurations to include in this suite

The server specification file for the test suite overrides the server
specification file for all test configurations contained in the suite.

## Collect a new measurement type

#### Add a new measurement collection script

A measurement collection script is a sub-class of the
MeasurementCollectionScript base class. It specifies the "query"
to run on the server-generated data to pull down the relevant measurement data,
calculate summary statistics on it, and write it out to a file.

There are two base classes that query Google logs and metrics. If the new
measurement type relies on either of these data stores, it can extend these base
classes to reuse the Google client library class and serialization logic when
writing out the results to a file

#### Add a new measurement list

A measurement list is an instance of the MeasurementList POJO class, serialized
into JSON and saved in the resources/measurementlists directory. Below are the
available fields:

* name: Name of the measurement list
* description: (optional) Description of the measurement list
* measurementCollectionScripts: List of measurement collection script
  specifications (i.e. instance of the MeasurementCollectionScriptSpecification
  POJO class, serialized into JSON). Each specification should include the below
  fields:
    * name: Name of the measurement collection script class to run
    * description: Description of the parametrized metric
    * parameters: (optional) parameters to pass to the metric collection script (up to version 0.1.2-SNAPSHOT, replaced with parametersMap since version 0.1.3-SNAPSHOT)
    * parametersMap: (optional) parametersMap to pass to the metric collection script (version 0.1.3-SNAPSHOT or above)

## Development

#### Requirements

Java 17

#### Package structure

All the Java code is in the src/main/java directory.

* The runner package contains code for executing test configurations, including
  the POJO classes used to specify a test configuration and suite.
* The collector package contains code for collecting measurements generated by
  the server, including the POJO class used to specify a list of measurements.
* The scripts package contains the "user-specified" code:
    * **testscripts**: user journeys
    * **deploymentscripts**: methods of application deployment
    * **disruptivescripts**: types of stress/disruption to apply to the
      application
    * **measurementcollectionscripts**: metric definitions
    * **utils**: utility methods used only **within** this package
* The common package contains code that may be useful for the test runner and
  measurement collector code, in addition to the user-specified code in the
  scripts package.

#### BigQuery Schema

The `bq` folder consists of a couple `BigQuery` JSON table schema used for
storing test results. The `create-testrunner-bq-tables.sh` is used for setting
up the `BigQuery` tables to store the test results, it requires a Google Project
ID as input. Please refer
to `Design Document: Test Runner Cloud Function Reporting Infrastructure`[https://docs.google.com/document/d/1sm5J85K9Ihph4fffICePJAkOiRxvYtzt7Aol7U3M5AE/edit#] for
architecture related to streaming test results from `GCS` bucket to `BigQuery`.

#### Debug a test configuration or script

* The Gradle run task just calls the main method of the RunTest or
  MeasurementCollector class. To debug, add a Run/Debug Configuration in
  IntelliJ that calls this method with the same arguments.

![Intellij run configuration](https://user-images.githubusercontent.com/10929390/158693901-1292131b-7fce-426d-b791-0b1420848a25.png)

* To debug a test script without the test runner, for example to make sure the
  API calls are coded correctly, add a main method that executes the
  setup/userjourney/cleanup steps. You can copy the main method in the
  TestScript base class and paste it into the test script class you want to
  debug. Then change the method to call the constructor of the test script class
  you want to debug. Run the test script main method in debug mode.

#### Dependencies

We use Gradle's [dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
to ensure that builds use the same transitive dependencies, so they're reproducible. 
This means that adding or updating a dependency requires telling Gradle to save the change. 
If you're getting errors that mention "dependency lock state" after changing a dep, you need to do this step.

```
./gradlew dependencies --write-locks
```

#### Linters

Run the linters before putting up a PR. The output of the spotbugs plugin is in
the build/reports/spotbugs/main.txt

```
./gradlew spotlessApply spotbugsMain
cat build/reports/spotbugs/main.txt
```

#### Testing plugin changes locally

If you're making changes to the plugin, you can test them locally by publishing
to your local Maven repository. Update the version in `build.gradle`, replacing
`0.1.10` with the new version you're testing:

```
version '0.1.10-SNAPSHOT'
```

Publish to your local Maven repository:

```shell
./gradlew publishToMavenLocal
```

In the `build.gradle` of the project you're testing with,  update the version for 
the test-runner plugin, and add `mavenlocal()` to your plugin repositories:

```groovy
repositories {
    mavenLocal()
}
dependencies {
    implementation 'bio.terra:terra-test-runner:0.1.10-SNAPSHOT'
}
```

Now you can build your project with the new version of the plugin.


#### Publish to Artifactory

Publish the Test Runner library after making changes, once your PR is merged up.
The script assumes a valid Vault token is either passed as the first argument
to the script, or is stored at `$HOME/.vault-token`.

```
./tools/publish.sh
```

The library is published to the URL:
https://broadinstitute.jfrog.io/artifactory/libs-snapshot-local/bio/terra/terra-test-runner

## Troubleshooting

* Check that the server specification file property of the test configuration
  points to the correct URL you want to test against.

* Check that your IP address is included on the IP whitelist for the cluster
  you're testing against. The easiest way to do this is to connect to the
  Non-Split Broad VPN, because the VPN IP addresses are already included on the
  IP whitelist for the Jade dev cluster.

* Check that you are calling the correct version of Gradle (6.1.1 or higher).
  Use the Gradle wrapper in the sub-project
  (`.gradlew`), not the one in the parent directory (`../.gradlew`).
