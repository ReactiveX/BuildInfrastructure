#!/bin/bash
# This script will upload to Bintray. It is intended to be conditionally executed on tagged builds.

echo -e "Starting upload to Bintray on Branch $TRAVIS_BRANCH and Tag $TRAVIS_TAG ...\n"

./gradlew candidate -PbintrayUser="${bintrayUser}" -PbintrayKey="${bintrayKey}" --stacktrace
RETVAL=$?

if [ $RETVAL -eq 0 ]; then
  echo 'Completed upload!'
else
  echo 'Upload failed.'
  return 1
fi
