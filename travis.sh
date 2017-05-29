#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v36 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
installTravisTools
. installJDK8

export DEPLOY_PULL_REQUEST=true
regular_mvn_build_deploy_analyze -Dmaven.test.redirectTestOutputToFile=true
