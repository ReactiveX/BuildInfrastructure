#!/bin/bash
# This script will upload to Bintray. It is intended to be conditionally executed on tagged builds.

echo -e 'Bintray Upload Script => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then
  echo -e 'Bintray Upload => Starting upload ...\n'
  
  ./gradlew -Prelease.useLastTag=true -PbintrayUser="${bintrayUser}" -PbintrayKey="${bintrayKey}" final --stacktrace
  RETVAL=$?
  
  if [ $RETVAL -eq 0 ]; then
    echo 'Completed upload!'
  else
    echo 'Upload failed.'
    false
  fi
else
  echo 'Bintray Upload => Not a tagged build so will not upload' 
fi
