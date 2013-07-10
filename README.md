master-builder
==============

A Jenkins Job DSL script that generates all our builds for us.

License
-------

This code is open source under the MIT license. See the LICENSE.md file for 
full details.

Requirements
------------

[Jenkins Job DSL](https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin) >= 0.15

Deployment
----------

1. Install the [Jenkins Job DSL](https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin) plugin.
2. Create a new freestyle job.
3. Under Source Code Management, select git, and enter the git clone URL for this repository.
4. Add a GITHUB_OAUTH_TOKEN environment variable (for instance, using the [EnvInject plugin](https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin)).
5. Add a build step for 'Process Job DSLs'.
6. Click 'Look on filesystem' and enter 'master-builder.groovy' in the box.
7. Set the action for removed jobs to 'Delete' (or 'Disable', if you're more paranoid).
8. Set then job to build periodically (every 10 minutes, say).

Adding new projects
-------------------

1. Create a new branch on this repo.
2. On that branch, add the repository name to the `projects` array at the top of `master-builder.groovy`.
3. Send a PR, get reviewed, and merge into master.
4. Sit back and relax as Jenkins wakes up, builds your updated version, and creates all the right jobs for you.

Local Development
-----------------

You can run the DSL locally if you want to. 

1. set the GITHUB_OAUTH_TOKEN in your shell environment.
2. Run `make setup` to install the job dsl plugin locally (as described on the [DSL wiki](https://github.com/jenkinsci/job-dsl-plugin/wiki/User-Power-Moves)).
3. Run `make` to produce a set of XML files for the generated jobs.
