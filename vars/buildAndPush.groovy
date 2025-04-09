/**
 * Generate Kaniko pod template to build and push Docker image to ECR
 * @param config Map of configuration options:
 * - language: Programming language/framework (optional )
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
  unstash(config.stashName)
  if(config.language == 'java'){
    container(name: 'maven') {
      sh "mvn package"
      sh "ls -la target/"
    }
  }
  echo "Maven build completed, starting Kaniko build"
  
  container(name: 'kaniko') {
    sh "echo 'Kaniko container is running'"
    sh "ls -la /"
    sh( "/kaniko/executor --context=${config.context} --dockerfile=${config.dockerfile} --destination=${config.registry}/${config.repository}:${config.tag}")
  }
  echo "Build and push process completed"
}

private def validateConfig(Map config){
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
private def genTemplate(){
  return kanikoPodYaml = """
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
}