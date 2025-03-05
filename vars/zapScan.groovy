def call(Map config = [:]) {
    def zapOptions = [
        "-t ${config.targetUrl}",
        "-r zap-report.html",
        "-J zap-report.json",
        config.authToken ? "-z '-config auth.token=${config.authToken}'" : ""
    ].join(' ')

    sh "zap-baseline.py ${zapOptions}"
    
    // Fail build if high risk issues found
    def report = readJSON file: 'zap-report.json'
    def highIssues = report.site.sum { it.alerts.count { it.riskcode == '3' } }
    
    if (highIssues > 0) {
        error "ZAP scan found ${highIssues} high risk issues"
    }
    
    archiveArtifacts artifacts: 'zap-report.*'
}
