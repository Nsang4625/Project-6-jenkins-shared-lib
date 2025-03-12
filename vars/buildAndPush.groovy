/**
 * Build and push the docker image using Kaniko
 * @param config Map of configuration options:
 * - language: Programming language/framework (required)
 * - repository: ECR repository name(required)
 * - tag: Docker image tag(required)
 * - registry: ECR registry URL(required)
 * - dockerfile: Path to Dockerfile(optional, default: Dockerfile)
 * - context: Path to build context(optional, default: .)
 * - stashName: Name of the stash to unstash(optional, default: source)
 * 
 */

def call(Map config = [:]){
  validateConfig(config)
  overrideConfig(config)
  buildAndPushImage(config)
  if(config.language == 'java'){
    sh "mvn package"
  }
  stash(name: 'source')
}

private def validateConfig(Map config){
  if(!config.language){
    error "Build and push failed: 'language' parameter is required"
  }
  if(!config.repository){
    error "Build and push failed: 'repository' parameter is required"
  }
  if(!config.tag){
    error "Build and push failed: 'tag' parameter is required"
  }
  if(!config.registry){
    error "Build and push failed: 'registry' parameter is required"
  }
}
private def overrideConfig(Map config){
  config.dockerfile = config.dockerfile ?: "Dockerfile"
  config.context = config.context ?: "."
  config.stashName = config.stashName ?: "source"
}
private def buildAndPushImage(){
  def kanikoPodYaml = """
apiVersion: v1
kind: Pod
metadata:
  name: kaniko
  namespace: cicd
spec:
  serviceAccountName: jenkins
  restartPolicy: Never
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:latest
    args:
    - --dockerfile=${config.dockerfile}
    - --context=${config.context}
    - --destination=${config.registry}/${config.repository}:${config.tag}
    env:
    - name: AWS_SDK_LOAD_CONFIG
      value: "true"
"""
  agent{
      kubernetes {
        yaml kanikoPodYaml
      }
  }
  container(name: 'kaniko'){
    unstash(name: 'source')
    sh "ls"
    sh "/kaniko/executor --dockerfile=${config.dockerfile} --context=${config.context} --destination=${config.registry}/${config.repository}:${config.tag}"
  }
}