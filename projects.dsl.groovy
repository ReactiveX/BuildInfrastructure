// https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core
@Grab(group='org.eclipse.mylyn.github', module='org.eclipse.egit.github.core', version='2.1.5')

import org.eclipse.egit.github.core.*
import org.eclipse.egit.github.core.client.*
import org.eclipse.egit.github.core.service.*
import java.util.regex.Pattern

// TODO Index jobs, so that customizations can be easily added

GitHubClient client = new GitHubClient()

def githubProperties = new File(GITHUB_PROPERTIES?:System.getenv()['GITHUB_PROPERTIES'])
Properties props = loadProperties(githubProperties)

loadCredentials(props, client)

def orgName = 'ReactiveX'
def parentFolderName = loadParentFolderName(props, githubProperties)
/* Waiting on CloudBees
folder {
    name parentFolderName
}
*/

Pattern regex = getRepoPattern(props, githubProperties)

// All work will be done inside this folder
RepositoryService repoService = new RepositoryService(client);

repoService.getOrgRepositories(orgName).findAll { it.name =~ regex }.each { Repository repo ->
    def repoName = repo.name
    def description = "${repo.description} - http://github.com/$orgName/$repoName"

    println "Creating jobs for $repoName"

    def repoFolderName = "${parentFolderName}/${repoName}"
    /* Waiting on CloudBees
    folder {
        name repoFolderName
    }
    */

    def nameBase = "${repoFolderName}/${repoName}"
    snapshot(nameBase, description, orgName, repoName, 'build-dev') // 'master')
    release(nameBase, description, orgName, repoName, 'build-dev') // 'master')
    // TODO Find github contrib group, and permission each user to the job.
    // TODO Permission global group

    // Pull Requests are outside of a specific branch
    pullrequest(nameBase, description, orgName, repoName) // Not sure what the branch should be
}

def String loadParentFolderName(Properties props, githubProperties) {
    def parentFolderName = props['jenkinsFolder']
    if (!parentFolderName) {
        throw new RuntimeException("Missing jenkinsFolder in ${githubProperties}")
    }
    parentFolderName
}

def loadCredentials(Properties props, GitHubClient client) {
    if (props['githubToken']) {
        def gitHubCredentials = props['githubToken']

        //OAuth2 token authentication
        client.setOAuth2Token(gitHubCredentials)
    } else {
        println "Not provided credentials"
    }
}

def loadProperties(githubProperties) {
    def props = new Properties()
    if (githubProperties.exists()) {
        githubProperties.withInputStream {
            stream -> props.load(stream)
        }
    } else {
        println "Missing properties file: ${githubProperties}"
        throw new RuntimeException("Missing properties file: ${githubProperties}")
    }
    props
}

Pattern getRepoPattern(Properties props, githubProperties) {
    String repoPattern = props['repoPattern']
    if (!repoPattern) {
        throw new RuntimeException("Missing repoPattern in ${githubProperties}")
    }
    Pattern.compile(repoPattern)
}

def base(String repoDesc, String orgName, String repoName, String branchName, boolean linkPrivate = true) {
    job {
        description ellipsize(repoDesc, 255)
        logRotator(60,-1,-1,20)
        wrappers {
        /* Waiting for CloudBees
            timeout {
                absolute(20)
            }
            */
            if (linkPrivate) {
                sshAgent('d79432e3-42d8-48df-a99f-5a3693d3b1fe')
            }
        }
        jdk('Oracle JDK 1.7 (latest)')
        scm {
            github("${orgName}/${repoName}", branchName, 'ssh') {
                if (linkPrivate) {
                    it / extensions / 'hudson.plugins.git.extensions.impl.LocalBranch' / localBranch(branchName)
                    it / userRemoteConfigs / 'hudson.plugins.git.UserRemoteConfig' / credentialsId('d79432e3-42d8-48df-a99f-5a3693d3b1fe')
                }
                it / skipTags('true')
            }
        }
        if (linkPrivate) {
            steps {
                shell("""
                if [ ! -d \$HOME/.gradle ]; then
                   mkdir \$HOME/.gradle
                fi
    
                rm -f \$HOME/.gradle/gradle.properties
                ln -s /private/netflixoss/reactivex/gradle.properties \$HOME/.gradle/gradle.properties

                # Get us a tracking branch
                git checkout $branchName || git checkout -b $branchName
                git reset --hard origin/$branchName
                # Git 1.8
                # git branch --set-upstream-to=origin/$branchName $branchName
                # Git 1.7
                git branch --set-upstream $branchName origin/$branchName
                git pull
                """.stripIndent())
            }
        }
        configure { project ->
            project / 'properties' / 'com.cloudbees.jenkins.plugins.PublicKey'(plugin:'cloudbees-public-key@1.1')
            if (linkPrivate) {
                project / buildWrappers / 'com.cloudbees.jenkins.forge.WebDavMounter'(plugin:"cloudbees-forge-plugin@1.6")
            }
        }
        publishers {
            archiveJunit('**/build/test-results/TEST*.xml')
        }
    }
}

def release(nameBase, repoDesc, orgName, repoName, branchName) {
    def job = base(repoDesc, orgName, repoName, branchName)
    job.with {
        name "${nameBase}-release"
        label 'hi-speed'
        steps {
            gradle('clean release --stacktrace')
        }
    }
}

def snapshot(nameBase, repoDesc, orgName, repoName, branchName) {
    def job = base(repoDesc, orgName, repoName, branchName)
    job.with {
        name "${nameBase}-snapshot"
        triggers {
            cron('@daily')
        }
        steps {
            gradle('clean build snapshot --stacktrace')
        }
        configure { project ->
            project / triggers / 'com.cloudbees.jenkins.GitHubPushTrigger' / spec
        }
    }
}

def pullrequest(nameBase, repoDesc, orgName, repoName) {
    def job = base(repoDesc, orgName, repoName, '', false)
    job.with {
        name "${nameBase}-pull-requests"
        steps {
            gradle('clean check --stacktrace --refresh-dependencies')
        }
        configure { project ->
            project / triggers / 'com.cloudbees.jenkins.plugins.github__pull.PullRequestBuildTrigger'(plugin:'github-pull-request-build@1.0-beta-2') / spec ()
            project / 'properties' / 'com.cloudbees.jenkins.plugins.git.vmerge.JobPropertyImpl'(plugin:'git-validated-merge@3.6') / postBuildPushFailureHandler(class:'com.cloudbees.jenkins.plugins.git.vmerge.pbph.PushFailureIsFailure')
        }
        publishers {
            // TODO Put pull request number in build number, $GIT_PR_NUMBER
        }
    }
}

String ellipsize(String input, int maxLength) {
  if (input == null || input.length() < maxLength) {
    return input;
  }
  return input.substring(0, maxLength) + '...';
}
