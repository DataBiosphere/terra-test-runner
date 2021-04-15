#!/bin/bash
set -e

# Software requirements:
#    yq version 4+.
#    Any recent jq version is fine.
#
# Script to set up ConfigMap for Terra Component Versions.
# This clones the terra-helm and terra-helmfile git repos, and templates in the desired
#   Terra component versions for Terra environment/k8s namespace
# from terra-helmfile.

# This script requires only the namespace as the first argument. The rest are optional.

# Required input
# TERRA_ENV can be a namespace (e.g. wsmtest) or alpha, dev, perf, staging
TERRA_ENV=$1
# Optional input
TERRA_HELM_BRANCH=${2:-master}
TERRA_HELMFILE_BRANCH=${3:-master}
GIT_PROTOCOL=${4:-http}

if [ "$GIT_PROTOCOL" = "http" ]; then
    helmgit=https://github.com/broadinstitute/terra-helm
    helmfilegit=https://github.com/broadinstitute/terra-helmfile
else
    # use ssh
    helmgit=git@github.com:broadinstitute/terra-helm.git
    helmfilegit=git@github.com:broadinstitute/terra-helmfile.git
fi

# Clone Helm chart and helmfile repos
rm -rf terra-helm
rm -rf terra-helmfile
git clone -b "$TERRA_HELM_BRANCH" --single-branch ${helmgit}
git clone -b "$TERRA_HELMFILE_BRANCH" --single-branch ${helmfilegit}

declare -a TERRA_COMPONENTS
declare -a TERRA_COMPONENT_VERSIONS
declare -a TERRA_COMPONENT_CHART_VERSIONS

getIndex () {
  val=$1
  # echo $val
  index=0
  while ((index<${#TERRA_COMPONENTS[@]}))
  do
    # echo "$index ${TERRA_COMPONENTS[$index]}"
    if [ "${TERRA_COMPONENTS[$index]}" = "$val" ]
    then
      echo $index
      return 0
    fi
    ((index++))
  done
  echo 'Not found'
  return 1
}

# Load default versions from terra-helmfile/versions.yaml
while read TERRA_COMPONENT
do
  VERSION=$(yq e ".releases.${TERRA_COMPONENT}.appVersion" terra-helmfile/versions.yaml)
  CHART_VERSION=$(yq e ".releases.${TERRA_COMPONENT}.chartVersion" terra-helmfile/versions.yaml)
  TERRA_COMPONENTS+=(${TERRA_COMPONENT})
  TERRA_COMPONENT_VERSIONS+=(${VERSION})
  TERRA_COMPONENT_CHART_VERSIONS+=(${CHART_VERSION})
done < <(yq e '.releases | keys' terra-helmfile/versions.yaml -j | jq -r -c '.[]')

# Test: Before override
echo "${TERRA_COMPONENT_VERSIONS[$(getIndex 'ontology')]}"
echo "${TERRA_COMPONENT_CHART_VERSIONS[$(getIndex 'ontology')]}"

# Override default values with env specific values
case ${TERRA_ENV} in
  alpha|dev|perf|staging)
    _TERRA_NAMESPACE_=default
    YAML="terra-helmfile/versions/${TERRA_ENV}.yaml"
    if [ -e "${YAML}" ]
    then
      while read TERRA_COMPONENT
      do
        VERSION=$(yq e ".releases.${TERRA_COMPONENT}.appVersion" "${YAML}")
        CHART_VERSION=$(yq e ".releases.${TERRA_COMPONENT}.chartVersion" "${YAML}")
        COMPONENT_IDX=$(getIndex "${TERRA_COMPONENT}")
        TERRA_COMPONENT_VERSIONS[$COMPONENT_IDX]=${VERSION}
        TERRA_COMPONENT_CHART_VERSIONS[$COMPONENT_IDX]=${CHART_VERSION}
      done < <(yq e '.releases | keys' "${YAML}" -j | jq -r -c '.[]')
    fi
    YAML_LIVE="terra-helmfile/environments/live/${TERRA_ENV}.yaml"
    if [ -e "${YAML_LIVE}" ]
    then
      while read TERRA_COMPONENT
      do
        VERSION_EXIST=$(yq e '.releases.'"${TERRA_COMPONENT}"' | has("appVersion")' "${YAML_LIVE}")
        if [ "${VERSION_EXIST}" = "true"  ]
        then
          VERSION=$(yq e ".releases.${TERRA_COMPONENT}.appVersion" "${YAML_LIVE}")
          CHART_VERSION=$(yq e ".releases.${TERRA_COMPONENT}.chartVersion" "${YAML_LIVE}")
          COMPONENT_IDX=$(getIndex "${TERRA_COMPONENT}")
          TERRA_COMPONENT_VERSIONS[$COMPONENT_IDX]=${VERSION}
          TERRA_COMPONENT_CHART_VERSIONS[$COMPONENT_IDX]=${CHART_VERSION}
        fi
      done < <(yq e '.releases | keys' "${YAML_LIVE}" -j | jq -r -c '.[]')
    fi
    ;;
  *)
    _TERRA_NAMESPACE_="terra-${TERRA_ENV}"
    YAML="terra-helmfile/environments/personal/${TERRA_ENV}.yaml"
    if [ -e "${YAML}" ]
    then
      while read TERRA_COMPONENT
      do
        VERSION_EXIST=$(yq e '.releases.'"${TERRA_COMPONENT}"' | has("appVersion")' "${YAML}")
        if [ "${VERSION_EXIST}" = "true"  ]
        then
          VERSION=$(yq e ".releases.${TERRA_COMPONENT}.appVersion" "${YAML}")
          CHART_VERSION=$(yq e ".releases.${TERRA_COMPONENT}.chartVersion" "${YAML}")
          COMPONENT_IDX=$(getIndex "${TERRA_COMPONENT}")
          TERRA_COMPONENT_VERSIONS[$COMPONENT_IDX]=$VERSION
          TERRA_COMPONENT_CHART_VERSIONS[$COMPONENT_IDX]=${CHART_VERSION}
        fi
      done < <(yq e '.releases | keys' "${YAML}" -j | jq -r -c '.[]')
    fi
    ;;
esac

# Test: After override
echo "${TERRA_COMPONENT_VERSIONS[$(getIndex 'ontology')]}"
echo "${TERRA_COMPONENT_CHART_VERSIONS[$(getIndex 'ontology')]}"

# Template in overridden version values
SUB=""
CHART_SUB=""
index=0
while ((index<${#TERRA_COMPONENTS[@]}))
do
  VERSION="${TERRA_COMPONENT_VERSIONS[$index]}"
  CHART_VERSION="${TERRA_COMPONENT_CHART_VERSIONS[$index]}"
  SUBVAR='_TERRA_'"${TERRA_COMPONENTS[$index]}"'_appVersion_'
  CHART_SUBVAR='_TERRA_'"${TERRA_COMPONENTS[$index]}"'_chartVersion_'
  if [ $index == 0 ]
  then
    SUB+="s|${SUBVAR}|${VERSION}|g"
    CHART_SUB+="s|${CHART_SUBVAR}|${CHART_VERSION}|g"
  else
    SUB+="; s|${SUBVAR}|${VERSION}|g"
    CHART_SUB+="; s|${CHART_SUBVAR}|${CHART_VERSION}|g"
  fi
  ((index++))
done

echo $SUB
echo $CHART_SUB

cat component-version-configmap.yml.template | \
    sed "s|_TERRA_NAMESPACE_|${_TERRA_NAMESPACE_}|g" | \
    sed "${SUB}" | \
    sed "${CHART_SUB}" > "${TERRA_ENV}-component-version-configmap.yml"

# Uncomment this when ready to apply ConfigMap to Kubernetes namespace
# kubectl apply -f "${TERRA_ENV}-component-version-configmap.yml" -n ${_TERRA_NAMESPACE_}
