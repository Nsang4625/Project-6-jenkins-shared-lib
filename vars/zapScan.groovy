/**
 * Jenkins shared library function to perform a ZAP scan.
 * 
 * @param config A map containing configuration options for the scan.
 * - zapHost: The ZAP host (required)
 * - zapPort: The ZAP port (required)
 * - targetUrl: The target URL to scan (required)
 * - commitId: The commit ID for report storage (required)
 */

def call(Map config = [:]) {
    validateConfig(config)
    performAjaxSpider(config)
    performActiveScan(config)
    retrieveReport(config)
    uploadReport(config)
}

private void validateConfig(Map config) {
    if (!config.targetUrl) {
        error "ZAP scan failed: 'targetUrl' parameter is required"
    }
    if (!config.zapHost) {
        error "ZAP scan failed: 'zapHost' parameter is required"
    }
    if (!config.zapPort) {
        error "ZAP scan failed: 'zapPort' parameter is required"
    }
    if (!config.commitId) {
        error "ZAP scan failed: 'commitId' parameter is required"
    }
}

private void performAjaxSpider(Map config = [:]){
    sh "curl 'http://${config.zapHost}:${config.zapPort}/JSON/ajaxSpider/action/scan/?url=${config.targetUrl}'"
    echo "Starting AJAX Spider scan on ${config.targetUrl}"
    
    def state = 0
    while (state == 0) {
        sleep 5
        state = sh(
            script: "curl -s 'http://${config.zapHost}:${config.zapPort}/JSON/ajaxSpider/view/state/' | grep running",
            returnStatus: true
        )
    }
    echo "AJAX Spider scan completed"
}

private void performActiveScan(Map config = [:]) {
    sh "curl 'http://${config.zapHost}:${config.zapPort}/JSON/ascan/action/scan/?url=${config.targetUrl}'"
    echo "Starting Active scan on ${config.targetUrl}"
    
    def state = 1
    while (state != 0) {
        sleep 5
        state = sh(
            script: "curl -s 'http://${config.zapHost}:${config.zapPort}/JSON/ascan/view/status/?scanId=0' | grep -o '\"status\":\"100\"'",
            returnStatus: true
        )
    }
    echo "Active scan completed"
}

private void retrieveReport(Map config = [:]){
    sh """
    curl "http://${config.zapHost}:${config.zapPort}/OTHER/core/other/htmlreport/?formMethod=GET" -o zap-report.html
    """
}

private void uploadReport(Map config = [:]){
    sh """
    aws s3 cp zap-report.html s3://project-647-test-reports/${config.commitId}/zap-reports
    """
}