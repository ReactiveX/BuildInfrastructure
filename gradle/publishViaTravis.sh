#!/bin/bash
# This script will upload to Bintray. It is intended to be conditionally executed on tagged builds.

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then
  echo -e 'Bintray Uploadb=> Starting upload on Branch' $TRAVIS_BRANCH ' and Tag ' $TRAVIS_TAG ' ...\n'
  
  ./gradlew candidate -PbintrayUser="${bintrayUser}" -PbintrayKey="${bintrayKey}" --stacktrace
  RETVAL=$?
  
  if [ $RETVAL -eq 0 ]; then
    echo 'Completed upload!'
  else
    echo 'Upload failed.'
    return 1
  fi
else
  echo 'Bintray Uploadb=> Not a tagged build so will not upload' 
fi
