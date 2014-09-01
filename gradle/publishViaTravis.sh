#!/bin/bash
# This script initiates the Gradle publishing task when pushes to master occur.

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
  echo -e "Starting publish to Sonatype...\n"

  ./gradlew candidate -PbintrayUser="${bintrayUser}" -PbintrayKey="${bintrayKey}"
  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo 'Completed publish!'
  else
    echo 'Publish failed.'
    return 1
  fi

fi
