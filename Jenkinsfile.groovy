//@Grab(group = 'my.compay', module='my-module-name', version='1.0.0-SNAPSHOT')
//import my.company.MyFancyClass
@Grab('com.google.guava:guava:23.0')
import com.google.common.base.Joiner

pipeline {
	agent any
	stages {
		stage('Grape Test') {
			steps {
				echo "Joiner: ${Joiner.class}"
				// echo "MyFancyClass: ${MyFancyClass.class}"
			}
		}
	}
}