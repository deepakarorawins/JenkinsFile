import hudson.tasks.test.AbstractTestResultAction
import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

properties([
    // Only keep the last few build data in Jenkins
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '20')),
    // Dropdown style parameters for which test configuration to use
    parameters([
		choice(choices: ['ios-connected', 'brkiosbuild2', 'brkiosbuild3', 'brkiosbuild5', 'brkiosbuild8', 'brkiosbuild9', 'brkiosbuild10', 'brkiosbuild14', 'brkiosbuild15'], description: '', name: 'Machine'),
		choice(choices: ['', '1', '2', '3', '4', '5'], description: 'Number of phones you want to use to run the tests (default is blank, which means use all avaliable phones on the computer)', name: 'DeviceCount'),
		string(defaultValue:'', description: 'Comma separated list of udids', name: 'Udid'),
        booleanParam(defaultValue: true, description: '', name: 'InstallApp'),
        choice(choices: ['0', '1', '2'], description: '', name: 'Retry'),
        choice(choices: ['master', 'dee_branch', 'q_branch', 'dq_branch', 'reportportal'], description: '', name: 'Branch'),
        choice(choices: ['test','stage', 'stage_anz', 'stage_emea', 'prod'], description: '', name: 'Environment'),
        choice(choices: ['ios-devices', 'ios-simulator'], description: '', name: 'Platform'),
        choice(choices: ['Baseline', 'Settings', 'OKAUTO', 'Kits', 'Multiuser', 'Places', 'Inventory', 'Location_History', 'Auth0', 'Quality-Gates', 'Non-Baseline', 'BVT', 'BVT_ANZ', 'BVT_EMEA', 'Equipment', 'MX', 'Lights', 'Crimpers', 'Cutters', 'Wrenches', 'Drivers', 'Barcode', 'Tick', 'Tracker', 'Prod_Smoke_Test', 'Backward-Compatibility', 'barcode-security', 'tool-addition'], description: '', name: 'ToolCategory'),
		choice(choices: ['', 'okauto@mailinator.com/1Keyautomation', 'nasanitytest@mailinator.com', 'places_onekey@mailinator.com/miP4cvma', 'pushpa@mailinator.com/Password1', 'pushpa1@mailinator.com/Password1', 'yogeshthakur@mailinator.com/miP4cvma', 'ythakur@mailinator.com/miP4cvma', 'eknoorsingh@mailinator.com/miP4cvma', 'esingh@mailinator.com/miP4cvma', '1keytestregression@mailinator.com/1Keyautomation', '1keystageregression@mailinator.com/1Keyautomation', '1keycrregression@mailinator.com/1Keyautomation', '1keyprodregression@mailinator.com/1Keyautomation', 'deemet@mailinator.com/miP4cvma', 'drivers@mailinator.com/1Keyautomation', 'lights@mailinator.com/1Keyautomation', 'crimpers@mailinator.com/1Keyautomation', 'wrenches@mailinator.com/1Keyautomation', 'cutters@mailinator.com/1Keyautomation', 'places@mailinator.com/1Keyautomation', 'geofence@mailinator.com/1Keyautomation', 'transfers@mailinator.com/1Keyautomation', 'settings@mailinator.com/miP4cvma'], description: 'Credentials that device 1 will use (default is blank, which means credentials specified for class in excel data will be used)', name: 'Credentials1'),
		choice(choices: ['', 'okauto@mailinator.com/1Keyautomation', 'nasanitytest@mailinator.com', 'places_onekey@mailinator.com/miP4cvma', 'pushpa@mailinator.com/Password1', 'pushpa1@mailinator.com/Password1', 'yogeshthakur@mailinator.com/miP4cvma', 'ythakur@mailinator.com/miP4cvma', 'eknoorsingh@mailinator.com/miP4cvma', 'esingh@mailinator.com/miP4cvma', '1keytestregression@mailinator.com/1Keyautomation', '1keystageregression@mailinator.com/1Keyautomation', '1keycrregression@mailinator.com/1Keyautomation', '1keyprodregression@mailinator.com/1Keyautomation', 'deemet@mailinator.com/miP4cvma', 'drivers@mailinator.com/1Keyautomation', 'lights@mailinator.com/1Keyautomation', 'crimpers@mailinator.com/1Keyautomation', 'wrenches@mailinator.com/1Keyautomation', 'cutters@mailinator.com/1Keyautomation', 'places@mailinator.com/1Keyautomation', 'geofence@mailinator.com/1Keyautomation', 'transfers@mailinator.com/1Keyautomation', 'settings@mailinator.com/miP4cvma'], description: 'Credentials that device 2 will use (default is blank, which means credentials specified for class in excel data will be used)', name: 'Credentials2'),
		choice(choices: ['', 'yes', 'no'], description: '', name: 'ReportPortal'),
		string(defaultValue: 'deepak.arora@milwaukeetool.com, suchithra.basavaraju@milwaukeetool.com, ritika.wadhwa@milwaukeetool.com, divyam.mahajan@milwaukeetool.com, prashant.kumarManjhi@milwaukeetool.com', description: 'Comma (or whitespace) separated list of email recipients', name: 'EmailRecipients'),
		string(defaultValue: '', description: 'This is app major version (default is blank, which means the latest version)', name: 'AppVersion'),
		string(defaultValue: '', description: 'This is app short version (default is blank, which means the latest version)', name: 'AppBuild')])
		
])

// For web tests, we use the node tag 'webautomation'
// iOS & Android tests will use 'ios-connected'
// Note: temporarily set to windows until Mac issues are resolved
//node('ios-connected') {
node("${Machine}") {
    cleanWs()
    def mvnHome
	def nexusUrl
    stage('Preparation') {
        // Get some code from a git repository
        git branch: "${params.Branch}", credentialsId: 'MkeDevelopmentKey', url: 'git@bitbucket.org:mkeonekey/poc-appium-test-automation2.git'

        // Get the Maven tool.s
        // ** NOTE: This 'M3' Maven tool is configured
        // **       in the global configuration.
        mvnHome = tool 'M3'
    }
    stage('Build') {
		//check for install flag
		def installFlag
		if (params.InstallApp == "true" || params.InstallApp == true) {
		//if (params.CustomData) {
			installFlag = 'y'
		}else {
			installFlag = 'n'
		}
		
		//find region and evironment based on app build selected
		def region
		def environment
		def appName
		switch(params.Environment) {
			case "test":
			  region = "na"
			  environment = "test"
			  appName = "OneKey-Test-QA"
			  break
			case "stage":
			  region = "na"
			  environment = "stage"
			  appName = "OneKey-Stage-NA"
			  break
			case "stage_emea":
			  region = "emea"
			  environment = "stage"
			  appName = "OneKey-Stage-EMEA"
			  break
			case "stage_anz":
			  region = "anz"
			  environment = "stage"
			  appName = "OneKey-Stage-AN"
			  break
			case "prod":
			  region = "na"
			  environment = "prod"
			  appName = "null"
			  break
		  }
		  
		  def version = getAppVersion(appName)
		  
		  def jenkinsUrl = "${BUILD_URL}".replace("build.milwaukeetool.com", "jenkins.milwaukeetool.com")
		  
		  //get all credentials for and combine them to a single comma seperated string
		  def users = ""
		  for(int i = 1; i <= 2; i++) {
			  if(!params["Credentials${i}"].isEmpty()) {
				  if(users.isEmpty()) {
					  users = params["Credentials${i}"]
				  }else {
					  users = users + "," + params["Credentials${i}"] 
				  }
			  }
		  }
		  
		//verify machine is running on Mac and not Windows machine
        if (isUnix()) {
			withCredentials([usernamePassword(credentialsId: 'AWS_CREDENTIALS', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]){
				if((params.ReportPortal=='yes') || (params.ReportPortal=='' && params.Branch=='master')){
					sh "'${mvnHome}/bin/mvn' clean -Dtest=Runner test -DfailIfNoTests=false -Dsystem=jenkins -DplatformName=iOS -Dinstall=${installFlag} -Dregion=${region} -Denvironment=${environment} -Dudids=${params.Udid} -DdeviceCount=${params.DeviceCount} -DappVersion=${params.AppVersion} -DappBuild=${params.AppBuild} -Dretry=${params.Retry} -DtoolCategory=${params.ToolCategory} -Dcredentials=${users} -Drp.launch=${params.ToolCategory} '-Drp.attributes=environment:${environment};region:${region};version:${version};branch:${params.Branch} ' '-Drp.description=[Link to ${JOB_NAME} #${BUILD_NUMBER}](${jenkinsUrl})'"
				}
				else{
					sh "'${mvnHome}/bin/mvn' clean -Dtest=Runner test -DfailIfNoTests=false -DplatformName=iOS -Dinstall=${installFlag} -Dregion=${region} -Denvironment=${environment} -Dudids=${params.Udid} -DdeviceCount=${params.DeviceCount} -DappVersion=${params.AppVersion} -DappBuild=${params.AppBuild} -Dretry=${params.Retry} -DtoolCategory=${params.ToolCategory} -Dcredentials=${users} "
				}
			}	   
        } 
    }
    stage('Results') {
        // Publish the junit results for visibility within Jenkins		
		junit "**/test-output/xml/*.xml"
    }
    stage('Archive') {
        def version = "${BUILD_NUMBER}-${new SimpleDateFormat('yyyy.MM.dd__HH.mm.ss').format(new Date())}"
		nexusUrl = "https://build.milwaukeetool.com/nexus/repository/onekey-test-automation/reports/brookfield/ios/${version}"
		
		//Upload the files to Nexus for download
		withCredentials([usernameColonPassword(credentialsId: 'Nexus', variable: 'nexusup')]) {
			sh "curl -v -u ${nexusup} --upload-file extentReport.html ${nexusUrl}/extentReport.html"
		}
		def logs = findFiles(glob: 'logs/**/*')
		for(file in logs) {
			withCredentials([usernameColonPassword(credentialsId: 'Nexus', variable: 'nexusup')]) {
				sh "curl -v -u ${nexusup} --upload-file ${file.path} ${nexusUrl}/${file.path}"
			}
		}
		def serverlogs = findFiles(glob: 'ServerLogs/**/*')
		for(file in serverlogs) {
			withCredentials([usernameColonPassword(credentialsId: 'Nexus', variable: 'nexusup')]) {
				sh "curl -v -u ${nexusup} --upload-file ${file.path} ${nexusUrl}/${file.path}"
			}
		}


		def emailBody, emailSubject, attachLog = false
		def artifactUrl =  "https://build.milwaukeetool.com/nexus/service/rest/repository/browse/onekey-test-automation/reports/brookfield/ios/${version}/".toString()
		def eEnv = "${params.Environment}"
		switch(currentBuild.result){
			case "UNSTABLE":
				emailBody = "Attached are the test results.\n ${testStatuses()}\n View test results here ${env.BUILD_URL}/testReport/\n View report from here \n${artifactUrl}"
				emailSubject = "${params.Platform}-${params.Environment}-${params.ToolCategory} : ${env.JOB_NAME} marked as unstable"
				break
			case "SUCCESS":
				emailBody = "Attached are the test results.\n ${testStatuses()}\n View report from here \n${artifactUrl}\nLink to job ${env.BUILD_URL}"
				emailSubject = "${params.Platform}-${params.Environment}-${params.ToolCategory} : ${env.JOB_NAME} completed successfully"
				break
			case "FAILURE":
				emailBody = "Attached are the test results.\n ${testStatuses()}\n Link to job ${env.BUILD_URL}"
				emailSubject = "${params.Platform}-${params.Environment}-${params.ToolCategory} : ${env.JOB_NAME} marked as failed"
				attachLog = true
				break
			default:
				emailBody = "Attached are the test results.\n ${testStatuses()}\n View report from here \n${artifactUrl}\nLink to job ${env.BUILD_URL}"
				emailSubject = "${params.Platform}-${params.Environment}-${params.ToolCategory} : ${env.JOB_NAME} completed successfully"
				break
		}

//		//upload each of the screenshots
		def screenshots = findFiles(glob: 'Screenshots/**/*')
		for(image in screenshots) {
			withCredentials([usernameColonPassword(credentialsId: 'Nexus', variable: 'nexusup')]) {
				//httpRequest isn't working for non-text files so just using curl for now even though it won't work on Windows
					sh "curl -v -u ${nexusup} --upload-file ${image.path} ${nexusUrl}/${image.path}"
			}
		}

            env.extentReportUrl=artifactUrl
             env.executionEnv= eEnv
            // Send an email based on the status of the build
             if (isUnix()) {
			sh "cp -R \"${WORKSPACE}/src/test/resources/email-templates/OKAuto-Email.template\" \"${JENKINS_HOME}\\email-templates\\OKAuto-Email.template\""
		} else {
			//bat "copy /Y \"${WORKSPACE}\\src\\test\\resources\\email-templates\\OKAuto-Email.template\" \"${JENKINS_HOME}\\email-templates\\OKAuto-Email.template\""
			bat "xcopy \"${WORKSPACE}\\src\\test\\resources\\email-templates\\OKAuto-Email.template\" \"${JENKINS_HOME}\\email-templates\\OKAuto-Email.template*\""
		}
            emailext attachLog: attachLog,
			mimeType: 'text/html',
                //body: emailBody,
		        body: '''${SCRIPT, template="OKAuto-Email.template"}''',
                compressLog: true,
                replyTo: 'noreply@milwaukeetool.com',
                subject: emailSubject,
                to: "${params.EmailRecipients}"
        }
		stage('Update TM4J test state') {
			try {
				withCredentials([usernameColonPassword(credentialsId: 'Nexus', variable: 'NEXUS_UP')]){
					sh "curl https://build.milwaukeetool.com/nexus/repository/onekey-static/tests/zephyr-sync-cli/0.3/zephyr-sync-cli-0.3.jar --output zephyr-sync-cli-0.3.jar -u ${NEXUS_UP}"
				}
				withCredentials([string(credentialsId: 'tm4j-access-token', variable: 'TOKEN')]){
                    withCredentials([usernamePassword(credentialsId: 'AWS_CREDENTIALS', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]){
						sh "java -jar zephyr-sync-cli-0.3.jar --reportType=junit --projectKey=\"OKI\"  --reportPath=\"test-output/xml/\" --reportUrl=\"$nexusUrl/Extent.html\" --testCycle=\"${params.Environment}${params.ToolCategory}\" --tm4jAccessToken=\"${TOKEN}\""
                    }
                }
			} catch (Exception e) {
				echo e.message
			}
		}
		stage('Update TM4J test state') {
			try {
				withCredentials([usernameColonPassword(credentialsId: 'Nexus', variable: 'NEXUS_UP')]){
					sh "curl https://build.milwaukeetool.com/nexus/repository/onekey-static/tests/zephyr-sync-cli/0.3/zephyr-sync-cli-0.3.jar --output zephyr-sync-cli-0.3.jar -u ${NEXUS_UP}"
				}
        		
				withCredentials([string(credentialsId: 'tm4j-access-token', variable: 'TOKEN')]){
					withCredentials([usernamePassword(credentialsId: 'AWS_CREDENTIALS', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]){
						sh "java -jar zephyr-sync-cli-0.3.jar --reportType=junit --projectKey=\"OKI\"  --reportPath=\"test-output/xml/\" --reportUrl=\"$nexusUrl/Extent.html\" --testCycle=\"${params.Environment}\" --tm4jAccessToken=\"${TOKEN}\""
					}
				}
			} catch (Exception e) {
				echo e.message
			}
		}
    }


/**
 * Get tests results as a string for use in the email
 * @return
 * Test Status:
 *   Passed: N, Failed: N  / �N, Skipped: N
 */
@NonCPS
def testStatuses() {
    def testStatus = ""
    AbstractTestResultAction testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    if (testResultAction != null) {
        def total = testResultAction.totalCount
        def failed = testResultAction.failCount
        def skipped = testResultAction.skipCount
        def passed = total - failed - skipped
        testStatus = "Test Status:\n  Passed: ${passed}, Failed: ${failed} ${testResultAction.failureDiffString}, Skipped: ${skipped}"

        if (failed == 0) {
            currentBuild.result = 'SUCCESS'
        }
    }
    return testStatus
}

def getAppVersion(String appName) {
	if(params.AppBuild.isEmpty()) {
		try {
			withCredentials([string(credentialsId: 'appcenter-api-token', variable: 'TOKEN')]){
				//api is slow when requesting info about a lot of different cycle
				//this first api call is to get the total number of cycles
				def findCycles = httpRequest httpMode: 'GET',
											 url: "https://api.appcenter.ms/v0.1/apps/onekeyadmin-781x/${appName}",
											 contentType: 'APPLICATION_JSON',
											 customHeaders:[[name:'X-API-TOKEN', value:"${TOKEN}"]]
				def appcenter = readJSON text: findCycles.getContent()
				def secret = appcenter.app_secret
				//with the total number of entries an api call can be made to just display the latests cycle's info
				def latestCycle = httpRequest httpMode: 'GET',
											  url: "https://api.appcenter.ms/v0.1/sdk/apps/${secret}/releases/latest",
											  contentType: 'APPLICATION_JSON',
											  customHeaders:[[name:'X-API-TOKEN', value:"${TOKEN}"]]
				def appInfo = readJSON text: latestCycle.getContent()
				//removing extra .0 at end of version number
				def appCenterVersion = appInfo.short_version.split("\\.")
				def appVersion = appCenterVersion[0] + '.' + appCenterVersion[1]
				return appVersion
			}
		}catch (Exception e) {
			echo e.message
			echo 'Error interacting with app center api to get app version. Version will be marked as null in report portal.'
			return 'null'
		}
	}else {
		//removing extra .0 at end of version number
		def appCenterVersion = params.AppBuild.split("\\.")
		def appVersion = appCenterVersion[0] + '.' + appCenterVersion[1]
		return appVersion
	}
}
