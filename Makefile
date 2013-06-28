jobs:
	java -jar job-dsl-plugin/job-dsl-core/build/libs/job-dsl-core-*-standalone.jar master-builder.groovy
	
setup:
	git clone https://github.com/jenkinsci/job-dsl-plugin.git
	cd job-dsl-plugin; ./gradlew :job-dsl-core:oneJar	
