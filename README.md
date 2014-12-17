This is a template project used to test the rxjava-project-plugin.

# Build

[![Build Status](https://travis-ci.org/ReactiveX/BuildInfrastructure.svg?branch=master)](https://travis-ci.org/ReactiveX/BuildInfrastructure)

# New Project

* Clone some other project, TBD
  * Make sure to customize group variable
* Create repo
* Run Ensure
* Run SEED job
* Navigate to https://bintray.com/reactivex/PROJECT and click "Add to JCenter"
 * Check "Host my snapshot build artifacts on the OSS Artifactory at https://oss.jfrog.org"
 * Fill in "Group ID" field with the group used in the first step, e.g. io.reactive.rxjava.swing
 * There's no need for a comment, but it is appreciated

# Release

There should be a CloudBees job for "releasing" (the act of pushing out an artifact, could be a RC or a real release). You will have to choose the scope and stage.
Both concepts come from the gradle-git plugin. Scope refers to which part of the version number should be incremented, i.e. "<major>.<minor>.<patch>". The options 
are "major", "minor", and "patch", and "patch" is the default.  The stage relates to where in the release cycle you're in, we define "dev", then "rc", then a final
release. "dev" appends "-dev.#", where the number is the number of commits since the last release. "rc" appends "-rc.#", where the number is an incrementing number
from the last "rc". Both "rc" and final will apply a tag.
