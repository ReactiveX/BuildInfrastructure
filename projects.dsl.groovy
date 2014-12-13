// https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core
@Grab(group='org.eclipse.mylyn.github', module='org.eclipse.egit.github.core', version='2.1.5')

import org.eclipse.egit.github.core.*
import org.eclipse.egit.github.core.client.*
import org.eclipse.egit.github.core.service.*
import org.eclipse.egit.github.core.util.*
import java.util.regex.Pattern

// TODO Index jobs, so that customizations can be easily added

GitHubClient client = new GitHubClient()

def githubProperties = new File(GITHUB_PROPERTIES?:System.getenv()['GITHUB_PROPERTIES'])
Properties props = loadProperties(githubProperties)

loadCredentials(props, client)

def orgName = 'ReactiveX'
def parentFolderName = loadParentFolderName(props, githubProperties)
folder {
    name parentFolderName
}

List<Pattern> regexs = getRepoPattern(props, githubProperties)

// All work will be done inside this folder
RepositoryService repoService = new RepositoryService(client);
ContentsService contentsService = new ContentsService(client);

repoService.getOrgRepositories(orgName)
    .findAll { matchRepository(regexs, it.name) }
    .findAll { matchGradle(contentsService, it) }
    .each { Repository repo ->
    def repoName = repo.name
    def description = "${repo.description} - http://github.com/$orgName/$repoName"

    println "Creating jobs for $repoName"

    def repoFolderName = "${parentFolderName}/${repoName}"
    folder {
        name repoFolderName
    }

    def nameBase = "${repoFolderName}/${repoName}"

    List<RepositoryBranch> branches = repoService.getBranches(repo)
    def gradleBranches = branches.findAll { it.name.endsWith('.x') }
    gradleBranches.collect { RepositoryBranch branch ->
        release("${nameBase}-${branch.name}", description, orgName, repoName, branch.name)
        // TODO Find github contrib group, and permission each user to the job.
        // TODO Permission global group
    }
    if (gradleBranches.isEmpty()) {
        release(nameBase, description, orgName, repoName, 'master')
    }
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

List<Pattern> getRepoPattern(Properties props, githubProperties) {
    String repoPattern = props['repoPattern']
    if (!repoPattern) {
        throw new RuntimeException("Missing repoPattern in ${githubProperties}")
    }
    repoPattern.tokenize(',').collect { Pattern.compile(it) }
}

boolean matchRepository(Collection<Pattern> repoRegexs, String name) {
    repoRegexs.isEmpty() || repoRegexs.any { name =~ it }
}

boolean matchGradle(ContentsService contentsService, repo, match = null) {
    try {
        def allContents = contentsService.getContents(repo, "build.gradle")
        def content = allContents.iterator().next()
        def bytes = EncodingUtils.fromBase64(content.content)
        String str = new String(bytes, 'UTF-8');
        return match ? str.contains(match) : true
    } catch (Exception fnfe) { // RequestException
        return false
    }
}

def base(String repoDesc, String orgName, String repoName, String branchName, boolean linkPrivate = true) {
    job {
        description ellipsize(repoDesc, 255)
        logRotator(60,-1,-1,20)
        label 'hi-speed'
        wrappers {
            timeout {
                absolute(20)
            }
            if (linkPrivate) {
                sshAgent('d79432e3-42d8-48df-a99f-5a3693d3b1fe')
            }
        }
        jdk('OpenJDK 8 (latest)')
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

        parameters {
            // Scope
            choiceParam("scope", ["patch", "minor", "major"], "What is the scope of this change?")

            // Stage
            choiceParam("stage", ["snapshot", "candidate", "final"], "Which stage should this be published as?")
        }

        steps {
            gradle('clean $stage -Prelease.scope=$scope --stacktrace --refresh-dependencies')
        }
    }
}

String ellipsize(String input, int maxLength) {
  if (input == null || input.length() < maxLength) {
    return input;
  }
  return input.substring(0, maxLength) + '...';
}
