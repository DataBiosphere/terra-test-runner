#!/bin/bash
# Publish Test Runner client library:

# ----------------------
# Google Artifact Registry
# ----------------------
export GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT:-dsp-artifact-registry}
export GAR_LOCATION=${GAR_LOCATION:-us-central1}
export GAR_REPOSITORY_ID=${GAR_REPOSITORY_ID:-libs-snapshot-standard}

./gradlew clean
./gradlew spotlessCheck spotbugsMain test
./gradlew publish
