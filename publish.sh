#!/bin/bash
# Copied from Terra Resource Buffer service
# Publish Test Runner client library:
VAULT_TOKEN=${1:-$(cat $HOME/.vault-token)}
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:consul-0.20.0
ARTIFACTORY_ACCOUNT_PATH=secret/dsp/accts/artifactory/dsdejenkins

export ARTIFACTORY_USERNAME=$(docker run -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
 vault read -field username ${ARTIFACTORY_ACCOUNT_PATH})
export ARTIFACTORY_PASSWORD=$(docker run -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
 vault read -field password ${ARTIFACTORY_ACCOUNT_PATH})
./gradlew spotlessCheck spotbugsMain test
./gradlew artifactoryPublish