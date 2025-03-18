/**
 * Deploy to k8s
 * @param config
 * - tag: Image tag
 * - env: Environment (dev, staging, prod)
 * - appName: Application name
 * - helmRepo: Helm repository URL(not contain protocol)
 * - githubToken: Github token name
 * - githubEmail: Github email
 */

def call(Map config = [:]){
  validateInput(config)
  cleanWs()
  checkout(scm: [$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "${config.githubToken}", url: "https://${config.helmRepo}"]]])
  sh """
    sed -i 's|tag: .*|tag: ${config.tag}|' helm/application/values-${config.appName}-${config.env}.yaml
    cat helm/application/values-${config.appName}-${config.env}.yaml
    """
  withCredentials([usernamePassword(credentialsId: "${config.githubToken}", usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]){
    sh """
      git config --global user.email "${config.githubEmail}"
      git config --global user.name "${GITHUB_USER}"
      git add helm/application/values-${config.appName}-${config.env}.yaml
      git commit -m "Update ${config.appName} ${config.env} image tag to ${config.tag}"
      git push https://${GITHUB_TOKEN}@${config.helmRepo} HEAD:main
    """
  }
}

private def validateInput(Map config){
  if(!config.tag){
    error "Deploy failed: 'tag' parameter is required"
  }
  if(!config.env){
    error "Deploy failed: 'env' parameter is required"
  }
  if(!config.appName){
    error "Deploy failed: 'appName' parameter is required"
  }
  if(!config.helmRepo){
    error "Deploy failed: 'helmRepo' parameter is required"
  } 
  if(!config.githubToken){
    error "Deploy failed: 'githubToken' parameter is required"
  }
  if(!config.githubEmail){
    error "Deploy failed: 'githubEmail' parameter is required"
  }
}