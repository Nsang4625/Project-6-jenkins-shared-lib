/**
 * Scans a Docker image using Trivy
 * 
 * @param config Map of configuration options:
 * - commitId: identifier for the commit
 * - repository: Image repository name (required)
 * - tag: Image tag (required)
 * - registryType: Registry type(ecr | dockerhub) (required)
 * - awsRegion: AWS region for ECR registry
 * - awsAccountId: AWS account ID for ECR registry
 */

def call(Map config = [:]) {
    validateInput(config)
    def outputFile = "trivy-scan.json"
    def imageName = "${config.repository}"
    pullImage(imageName, config)
    def exitCode = sh script: """
        trivy image \
            --format json \
            --output ${outputFile} \
            --input ${imageName}.tar    
    """, returnStatus: true
    sh "rm -f ${imageName}.tar"

    def result = null
    def jsonText = sh(script: "cat ${outputFile}", returnStdout: true).trim()
    result = new groovy.json.JsonSlurperClassic().parseText(jsonText)
    handleScanResult(result, exitCode, outputFile, config.commitId)
}

private def handleScanResult(Map result, int exitCode, String outputFile, String commitId){
    def criticalCount = result.Results.sum { it.Vulnerabilities.count { it.Severity == 'CRITICAL' } }
    if(exitCode != 0){
        error "Trivy scan failed with exit code ${exitCode}"
    }
    if(criticalCount > 0){
        echo "Trivy scan failed: ${criticalCount} critical vulnerabilities found"
    }
    sh "aws s3 cp ${outputFile} s3://project-647-test-reports/${commitId}/trivy-reports"
}

private def validateInput(Map config){
    if(!config.commitId){
        error "Trivy scan failed: 'commitId' parameter is required"
    }
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
        // Get ECR registry and full image name
        def registry = "${config.awsAccountId}.dkr.ecr.${config.awsRegion}.amazonaws.com"
        def fullImageName = "${registry}/${config.repository}:${config.tag}"
        
        // Get the token
        def ecrToken = sh(script: "aws ecr get-login-password --region ${config.awsRegion}", returnStdout: true).trim()
        
        // Use maskPasswords to hide the token in logs
        wrap([$class: 'MaskPasswordsBuildWrapper', 
              varPasswordPairs: [[password: ecrToken, var: 'ECR_TOKEN']]]) {
            sh """
                ECR_TOKEN='${ecrToken}'
                skopeo copy --src-creds="AWS:\$ECR_TOKEN" \
                    docker://${fullImageName} \
                    docker-archive:${imageName}.tar
            """
        }
    } else if(config.registryType == 'dockerhub'){
        sh "skopeo copy docker://docker.io/${imageName} docker-archive:${imageName}.tar"
    }
}
