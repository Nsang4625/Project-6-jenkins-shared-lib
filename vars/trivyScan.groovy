/**
 * Scans a Docker image using Trivy
 * 
 * @param config Map of configuration options:
 * - repository: Image repository name (required)
 * - tag: Image tag (required)
 * - registryType: Registry type(ecr | dockerhub) (required)
 * - awsRegion: AWS region for ECR registry
 * - awsAccountId: AWS account ID for ECR registry
 */

def call(Map config = [:]) {
    validateInput(config)
    def outputFile = "trivy-scan.json"
    def imageName = "${config.repository}:${config.tag}"
    pullImage(imageName, config)
    def exitCode = sh script: """
        trivy image \
            --severity CRITICAL,HIGH \
            --format json \
            --output ${outputFile} \
            --input ${imageName}.tar    
    """, returnStatus: true

    def result = readJSON file: outputFile
    handleScanResult(result, exitCode, outputFile)
}

private def handleScanResult(Map result, int exitCode, String outputFile) {
    def criticalCount = result.Results.sum { it.Vulnerabilities.count { it.Severity == 'CRITICAL' } }
    
    if (criticalCount > 0) {
        sh "aws s3 cp ${outputFile} s3://my-bucket/trivy-reports/"
        error "Trivy scan failed: ${criticalCount} critical vulnerabilities found"
    } else if (exitCode != 0) {
        error "Trivy scan failed with exit code ${exitCode}"
    }
    sh "aws s3 cp ${outputFile} s3://my-bucket/trivy-reports/"
}

private def validateInput(Map config){
    if(!config.repository){
        error "Trivy scan failed: 'repository' parameter is required"
    }
    if(!config.tag){
        error "Trivy scan failed: 'tag' parameter is required"
    }
    if(!config.registryType){
        error "Trivy scan failed: 'registryType' parameter is required"
    }
    if(config.registryType == 'ecr' && (!config.awsRegion || !config.awsAccountId)){
        error "Trivy scan failed: 'awsRegion' and 'awsAccountId' parameters are required for ECR registry"
    }
}

private def pullImage(String imageName, Map config){
    if(config.registryType == 'ecr'){
        // Get ECR auth token
        def ecrAuthToken = sh(script: "aws ecr get-login-password --region ${config.awsRegion}", returnStdout: true).trim()
        
        // Set up a temp auth.json file in the workspace
        def authConfig = """
        {
            "auths": {
                "${config.awsAccountId}.dkr.ecr.${config.awsRegion}.amazonaws.com": {
                    "auth": "${ecrAuthToken}"
                }
            }
        }
        """
        writeFile file: "auth.json", text: authConfig
        sh "cat auth.json"
        
        // Use the auth file with skopeo
        sh """
            skopeo copy --authfile=auth.json \
                docker://${config.awsAccountId}.dkr.ecr.${config.awsRegion}.amazonaws.com/${config.repository}:${config.tag} \
                docker-archive:${imageName}.tar
            rm -f auth.json
        """
    } else if(config.registryType == 'dockerhub'){
        sh "skopeo copy docker://docker.io/${imageName} docker-archive:${imageName}.tar"
    }
}
