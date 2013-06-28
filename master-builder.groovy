def projects = [
  'git-data-viewer',
  'open-orgn-services'
]

projects.each {
  def projectName = it

  // Get the list of branches from github
  def branchApi = new URL("https://api.github.com/repos/theodi/${projectName}/branches")
  def branches = new groovy.json.JsonSlurper().parse(branchApi.newReader())

  // Generate a job for each branch
  branches.each { 
    def branchName = it.name

    job {

      // Job name
      name "dsl-test-${projectName}-${branchName}".replaceAll('/','-')

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
        
        // CI game
        project/publishers << "hudson.plugins.cigame.GamePublisher" {}
        
        // Mail notifications
        project/publishers << "hudson.tasks.Mailer" {
          recipients "tech@theodi.org"
          dontNotifyEveryUnstableBuild "false"
          sendToIndividuals "true"
        }
        
        // Coverage
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
        
        // Some post-build tasks for master
        if(branchName == "master") {
          // Post-build release tagging if on master
          project/publishers << "hudson.plugins.postbuildtask.PostbuildTask" {
            tasks {
              "hudson.plugins.postbuildtask.TaskProperties" {
                "EscalateStatus" "true"
                "RunIfJobSuccessful" "true"
                script """\
cd \$WORKSPACE;
git tag release-\$BUILD_ID;
git push origin release-\$BUILD_ID;
git tag -f CURRENT;
git push origin CURRENT"""
              }
            }
          }
          // Push features to relish if available
          project/publishers << "hudson.plugins.postbuildtask.PostbuildTask" {
            tasks {
              "hudson.plugins.postbuildtask.TaskProperties" {
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
      }
    }
  } 
}