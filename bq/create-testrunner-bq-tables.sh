#!/bin/bash

# This script lists the commands to setup a BQ dataset to hold TestRunner results.
PROJECT_ID=${1}
if [ -z "$1" ]
  then
    echo "PROJECT_ID is a required input."
    echo "Usage: ./create-bq-testrunner-results.sh <PROJECT_ID>"
    exit 1;
fi

dataset=test_runner_results

# create the dataset if it does not exist
bq show $project_id:$dataset || \
    bq --location=US mk -d --description "TestRunner results" $project_id:$dataset

# create the tables if they do not exist
bq show --schema $project_id:$dataset.testRun || \
    bq mk --table $project_id:$dataset.testRun ./tableSchema_testRun.json
bq show --schema $project_id:$dataset.testScriptResults || \
    bq mk --table $project_id:$dataset.testScriptResults ./tableSchema_testScriptResults.json
bq show --schema $project_id:$dataset.measurementCollection || \
    bq mk --table $project_id:$dataset.measurementCollection ./tableSchema_measurementCollection.json

# list tables in the BQ dataset
bq ls $project_id:$dataset
