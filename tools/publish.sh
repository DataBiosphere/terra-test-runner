#!/bin/bash
# Copied from Terra Resource Buffer service
# Publish Test Runner client library:
VAULT_TOKEN=${1:-$(cat $HOME/.vault-token)}

# Images built for arm64 platform are required to run this script on M1 Mac (arm64)
# pick recent stable image from https://hub.docker.com/r/broadinstitute/dsde-toolbox/tags
# original tag 'consul-0.20.0' built only for amd64
IMAGE_TAG=master # built for both amd64 & arm64
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:${IMAGE_TAG}
ARTIFACTORY_ACCOUNT_PATH=secret/dsp/accts/artifactory/dsdejenkins

export ARTIFACTORY_USERNAME=$(docker run -e VAULT_TOKEN=$VAULT_TOKEN --platform linux/amd64 ${DSDE_TOOLBOX_DOCKER_IMAGE} \
 vault read -field username ${ARTIFACTORY_ACCOUNT_PATH})
export ARTIFACTORY_PASSWORD=$(docker run -e VAULT_TOKEN=$VAULT_TOKEN --platform linux/amd64 ${DSDE_TOOLBOX_DOCKER_IMAGE} \
 vault read -field password ${ARTIFACTORY_ACCOUNT_PATH})
./gradlew clean
./gradlew spotlessCheck spotbugsMain test
./gradlew artifactoryPublish