#!/usr/bin/env groovy

/**
 * Send email notifications via Gmail SMTP
 * 
 * @param config Map of configuration options:
 * - to: Recipients email addresses (comma-separated) (required)
 * - subject: Email subject (required)
 * - body: Email body content (required)
 * - attachments: List of file paths to attach (optional)
 * - cc: CC recipients (comma-separated) (optional)
 * - bcc: BCC recipients (comma-separated) (optional)
 * - replyTo: Reply-to email address (optional)
 * - from: Sender email address (optional, defaults to Jenkins default)
 * - credentialsId: Jenkins credentials ID storing Gmail credentials (optional)
 * - mimeType: Email content type (optional, defaults to 'text/html')
 * - attachLogs: Whether to attach build logs (optional, defaults to false)
 * - attachJUnitReports:   Whether to attach JUnit reports (optional, defaults to false)
 * - failOnError: Whether to fail the build if email sending fails (optional, defaults to false)
 * @return Boolean indicating success or failure
 */
def call(Map config = [:]) {
    // Validate required parameters
    validateInput(config)
    
    // Set default values
    def mimeType = config.mimeType ?: 'text/html'
    def attachLogs = config.containsKey('attachLogs') ? config.attachLogs : false
    def attachJUnitReports = config.containsKey('attachJUnitReports') ? config.attachJUnitReports : false
    def failOnError = config.containsKey('failOnError') ? config.failOnError : false
    
    // Build attachments list
    def attachmentsParam = []
    
    if (config.attachments) {
        if (config.attachments instanceof List) {
            config.attachments.each { attachment ->
                attachmentsParam.add([$class: 'AttachmentParam', name: attachment.split('/')[-1], content: attachment])
            }
        } else {
            attachmentsParam.add([$class: 'AttachmentParam', name: config.attachments.split('/')[-1], content: config.attachments])
        }
    }
    
    // Add build logs if requested
    if (attachLogs) {
        attachmentsParam.add([$class: 'BuildLogParam', compress: true, logLines: -1])
    }
    
    // Add JUnit reports if requested
    if (attachJUnitReports) {
        attachmentsParam.add([$class: 'JUnitTestResultsParam', attachmentsPattern: '**/target/surefire-reports/*.xml', namePattern: 'JUnit-Tests-${BUILD_NUMBER}'])
    }
    
    try {
        // Prepare email parameters
        def emailParams = [
            to: config.to,
            subject: config.subject,
            body: config.body,
            mimeType: mimeType
        ]
        
        // Add optional parameters
        if (config.cc) {
            emailParams.put('cc', config.cc)
        }
        
        if (config.bcc) {
            emailParams.put('bcc', config.bcc)
        }
        
        if (config.replyTo) {
            emailParams.put('replyTo', config.replyTo)
        }
        
        if (config.from) {
            emailParams.put('from', config.from)
        }
        
        // Add attachments if present
        if (attachmentsParam.size() > 0) {
            emailParams.put('attachmentsPattern', attachmentsParam)
        }
        
        // Use credentials if provided
        if (config.credentialsId) {
            emailext(
                to: config.to,
                subject: config.subject,
                body: config.body,
                mimeType: mimeType,
                attachmentsPattern: attachmentsParam,
                from: config.from ?: null,
                replyTo: config.replyTo ?: null,
                cc: config.cc ?: null,
                bcc: config.bcc ?: null,
                attachLog: attachLogs,
                compressLog: true,
                credentialsId: config.credentialsId
            )
        } else {
            // Use Jenkins default email configuration
            emailext(emailParams)
        }
        
        echo "Email notification sent successfully to ${config.to}"
    } catch (Exception e) {
        echo "Failed to send email notification: ${e.message}"
        if (failOnError) {
            error "Email notification failed: ${e.message}"
        }
    }
}

/**
 * Validate required input parameters
 */
private def validateInput(Map config) {
    def errors = []
    
    if (!config.to) {
        errors.add("'to' parameter is required")
    }
    
    if (!config.subject) {
        errors.add("'subject' parameter is required")
    }
    
    if (!config.body) {
        errors.add("'body' parameter is required")
    }
    
    if (errors.size() > 0) {
        error "Email notification failed: ${errors.join(', ')}"
    }
}