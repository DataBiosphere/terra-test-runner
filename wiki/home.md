# Introduction

In a nutshell, `Test Runner Framework` is a configurable test framework that supports both `Java` and `Scala`. The framework enables developers to quickly automate test cases with a few simple framework and build tool configurations. The `Quick Start` section under the [Developers Guide](#developers-guide) provides installation instructions and sample code pertaining to the following build tools.

* Gradle
* Maven
* Sbt

For developers, all it takes to use `Test Runner Framework` is to extend test classes by `TestScript` base class and override its `userJourney` method. There is no need to learn and maintain scripts written for different test tools (e.g. `Gatling` or `JMeter`).

The framework has built-in code and infrastructure for following activities related to hosting and publishing test results data. These activities require only minor framework configuration whereas the majority of work takes place behind the scene.

* Collection of comprehensive test statistics.
* Collection of build statistics (commits, dependencies, versions).
* Collection of CI/CD pipelines information (workflows, job runs).

The `Test Runner Framework` comes with a central visualization tool called [Test Runner Dashboard](https://trdash.dsp-eng-tools.broadinstitute.org/). The dashboard code and infrastructure are maintained in [terra-test-runner-dashboard](https://github.com/DataBiosphere/terra-test-runner-dashboard) repository. The dashboard displays test results for independent releases including trend and flakiness analytics.

The framework supports many different test types including

* Integration
* Performance
* Resiliency

The independent release test types and their definitions are still being actively discussed in `Independent Releases & Testing Steering Group Sync`.

# Developers Guide

* [Quick Start](https://github.com/DataBiosphere/terra-test-runner/wiki/Quick-Start)







