def call(String imageName) {
    def outputFile = "trivy-scan-${env.BUILD_ID}.json"
    def exitCode = sh script: """
        trivy image \
            --severity CRITICAL,HIGH \
            --format json \
            --output ${outputFile} \
            ${imageName}
    """, returnStatus: true

    def result = readJSON file: outputFile
    handleScanResult(result, exitCode, outputFile)
}

private void handleScanResult(Map result, int exitCode, String outputFile) {
    archiveArtifacts artifacts: outputFile
    def criticalCount = result.Results.sum { it.Vulnerabilities.count { it.Severity == 'CRITICAL' } }
    
    if (criticalCount > 0) {
        error "Trivy scan failed: ${criticalCount} critical vulnerabilities found"
    } else if (exitCode != 0) {
        error "Trivy scan failed with exit code ${exitCode}"
    }
}
