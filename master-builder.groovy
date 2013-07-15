def projects = [
  'csv-profiler',
  'dashboards',
  'data_kitten',
  'git-data-viewer',
  'member-directory',
  'office-calendar',  
  'open-data-certificate',
  'open-orgn-services',
  'services-manager',
  'noodile'
]

// Not all projects include coverage information
def noCoverage = [
  'csv-profiler',
  'member-directory',
  'office-calendar',  
  'open-data-certificate',
  'services-manager',
  'noodile'
]

def ignoreBranches = [
  'gh-pages',
  'auto-generate-country-questionnaires'
]

def tagBranches = [
  'master'  : 'CURRENT',
  'staging' : 'STAGING'
]

projects.each {
  def projectName = it

  // Get the list of branches from github
  def branchApi = new URL("https://api.github.com/repos/theodi/${projectName}/branches?access_token=${GITHUB_OAUTH_TOKEN}")
  def branches = new groovy.json.JsonSlurper().parse(branchApi.newReader())

  // Generate a job for each branch
  branches.each { 
    def branchName = it.name
    
    // Check ignored branches
    if (!ignoreBranches.contains(branchName)) {
    
      job {

        // Job name
        name "${projectName}-${branchName}".replaceAll('/','-')

        // Git configuration
        scm {
          git("git@github.com:theodi/${projectName}.git", branchName)
        }
  
        // Trigger builds on github pushes
        configure { project ->
          project/triggers << "com.cloudbees.jenkins.GitHubPushTrigger" {
            spec ""
          }
        }
  
        // Configure build wrappers
        configure { project ->
          // use colour xterm output
          project/buildWrappers << "hudson.plugins.ansicolor.AnsiColorBuildWrapper" {
            colorMapName "xterm"
          }
          // Use RVM environment '.'
          project/buildWrappers << "ruby-proxy-object" {
            ruby-object("ruby-class":"Jenkins::Plugin::Proxies::BuildWrapper", pluginid:"rvm") {
              object("ruby-class":"RvmWrapper", pluginid:"rvm") {
                impl(pluginid:"rvm", "ruby-class":"String", ".")
              }
              pluginid(pluginid:"rvm", "ruby-class":"String", "rvm")
            }
          }            
        }
  
        // Build steps: shell script
        steps {
            shell("""\
#!/bin/bash
source /var/lib/jenkins/env
[[ -s 'package.json' ]] && npm install
[[ -s 'Gemfile' ]] && bundle --without=production
[[ -s 'db' ]] && rake db:migrate
rake""")
        }

        // Publishers
        configure { project ->
        
          // Some post-build tasks for master
          if(tagBranches[branchName]) {
            // Post-build release tagging
            project/publishers << "hudson.plugins.postbuildtask.PostbuildTask" {
              tasks {
                "hudson.plugins.postbuildtask.TaskProperties" {
                  logTexts {
                    "hudson.plugins.postbuildtask.LogProperties" {
                      logText ""
                      operator "AND"
                    }
                  }
                  "EscalateStatus" "true"
                  "RunIfJobSuccessful" "true"
                  script """\
cd \$WORKSPACE;
git tag ${branchName}-\$BUILD_ID;
git tag -f ${tagBranches[branchName]};
git push origin --tags --force"""
                }
              }
            }
          }

          if(branchName == "master") {
            // Push features to relish if available
            project/publishers << "hudson.plugins.postbuildtask.PostbuildTask" {
              tasks {
                "hudson.plugins.postbuildtask.TaskProperties" {
                  logTexts {
                    "hudson.plugins.postbuildtask.LogProperties" {
                      logText ""
                      operator "AND"
                    }
                  }
                  "EscalateStatus" "false"
                  "RunIfJobSuccessful" "true"
                  script """\
#!/bin/bash
source "/var/lib/jenkins/.rvm/scripts/rvm" && rvm use .
[[ -s 'features' ]] && bundle exec relish push theodi/${projectName}"""
                }
              }
            }
          }          
            
          // CI game
          project/publishers << "hudson.plugins.cigame.GamePublisher" {}
      
          // Coverage
          if (!noCoverage.contains(projectName)) {
            project/publishers << "hudson.plugins.rubyMetrics.rcov.RcovPublisher" {
              reportDir "coverage/rcov"
              targets {
                "hudson.plugins.rubyMetrics.rcov.model.MetricTarget" {
                  metric "TOTAL_COVERAGE"
                  healthy "90"
                  unhealthy "75"
                  unstable "0"
                }
                "hudson.plugins.rubyMetrics.rcov.model.MetricTarget" {
                  metric "CODE_COVERAGE"
                  healthy "90"
                  unhealthy "75"
                  unstable "0"
                }
              }
            }
          }
          
          // Github notification
          project/publishers << "com.cloudbees.jenkins.GitHubCommitNotifier"(plugin: "github@1.6") {}

          // Mail notifications
          project/publishers << "hudson.tasks.Mailer" {
            recipients "tech@theodi.org"
            dontNotifyEveryUnstableBuild "false"
            sendToIndividuals "true"
          }
        
        }
      }
    }
  } 
}
