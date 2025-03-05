def call(Map config = [:]){
  withSonarQubeEnv('sonar') {
    sh """
    sonar-scanner \
      -Dsonar.projectKey=${config.sonarProjectKey} \
      -Dsonar.sources=. \
      -Dsonar.host.url=${config.sonarHostUrl} \
      -Dsonar.login=${config.sonarToken}
    """
  }
  timeout(time: config.qualityGateTimeout, unit: 'MINUTES') {
        waitForQualityGate abortPipeline: true
  }
}