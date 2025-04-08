/**
 * Jenkins shared library function to perform SonarQube scan.
 * 
 * @param config A map containing configuration options for the scan.
 * - sonarProjectKey: The SonarQube project key (required)
 * - sonarToken: The SonarQube token (required)
 * - sonarHostUrl: The SonarQube host URL (required)
 * - qualityGateTimeout: The timeout for the quality gate check (optional, default: 5 minutes)
 * - sonarServer: The SonarQube server name (optional, default: 'sonar-server')
 * - sonarScanner: The SonarQube scanner name (optional, default: 'sonar-scanner')
 */

def call(Map config = [:]) {
  validateConfig(config)
  withCredentials([string(credentialsId: "${config.sonarToken}", variable: 'SONAR_TOKEN')]) {
    withSonarQubeEnv("sonar-server") {
      sh """
      ${config.sonarScanner}/bin/sonar-scanner \
        -Dsonar.projectKey=${config.sonarProjectKey} \
        -Dsonar.sources=. \
        -Dsonar.host.url=${config.sonarHostUrl} \
        -Dsonar.login=\${SONAR_TOKEN}
      """
    }
  }
  
  timeout(time: config.qualityGateTimeout ?: 5, unit: 'MINUTES') {
    waitForQualityGate abortPipeline: true
  }
}

def validateConfig(Map config) {
  if (!config.sonarProjectKey) {
    error("sonarProjectKey is required")
  }
  if (!config.sonarToken) {
    error("sonarToken is required")
  }
  if (!config.sonarHostUrl) {
    error("sonarHostUrl is required")
  }
  if (!config.sonarServer) {
    config.sonarServer = 'sonar-server'
  }
}