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
3. Add a build step for 'Process Job DSLs'.
4. Click 'Use the provided DSL script'.
5. Copy the contents of master-builder.groovy into the 'DSL script' box.
6. Set your github oauth token in the first line.
7. Set the action for removed jobs to 'Delete' (or 'Disable', if you're more paranoid).
8. Set then job to build periodically (every 10 minutes, say).

Local Development
-----------------

You can run the DSL locally if you want to. 

1. Run `make setup` to install the job dsl plugin locally (as described on the [DSL wiki](https://github.com/jenkinsci/job-dsl-plugin/wiki/User-Power-Moves)).
2. Run `make` to produce a set of XML files for the generated jobs.