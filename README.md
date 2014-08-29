This is a template project used to test the rxjava-project-plugin.

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

The release targets are 'release', 'candidate' and 'snapshot. All of which should be called from the CloudBees jobs. 

On release, by default the patch field will be incremented. To change this behavior the Jenkins job has to be reconfigured with "-Prelease.scope=major" or "-Prelease.scope=minor".

On candidate, the calculated next release field will be used and appended with "-rc." with an incrementing number.
