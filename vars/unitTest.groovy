/**
* Execute unit tests based on the project language
* 
* @param config Map of configuration options:
*  - language: Programming language/framework (required)
*  - customInstall: Custom installation command (optional)
*  - customTest: Custom test command (optional)
*  - workDir: Working directory for tests (optional)
*  - outputFormat: Output format for test results (optional)
*  - extraArgs: Extra arguments for test command (optional)
*  @return Test execution status
*/

/**
* Further considerations:
* - Add support for multiple version languages and frameworks
* - Add support for multiple version package managers
*/

def call(Map config = [:]) {
  // validate required parameters
  if(!config.language){
    error "Unit test failed: 'language' parameter is required"
  }

  // Language commands configuration map
  def languageCommands = [
    'nodejs': [
      'install': 'npm install',
      'test': 'npm test'
    ],
    'python': [
      'install': 'pip install -r requirements.txt',
      'test': 'python -m pytest'
    ],
    'java': [
      'install': 'mvn clean install',
      'test': 'mvn test'
    ],
    'golang': [
      'install': 'go get -v ./...',
      'test': 'go test ./...'
    ],
    'dotnet': [
      'install': 'dotnet restore',
      'test': 'dotnet test'
    ]
  ]
  def commands = languageCommands[config.language]
  if(!commands){
    error "Unit test failed: Unsupported language '${config.language}'"
  }
  // Use custom commands if provided
  def installCommand = config.customInstall ?: commands.install
  def testCommand = config.customTest ?: commands.test

  if(config.extraArgs){
    testCommand += " ${config.extraArgs}"
  }

  // Change to working directory if provided
  if(config.workDir){
    dir(config.workDir){
      executeTests(installCommand, testCommand)
    }
  } else {
    executeTests(installCommand, testCommand)
  }

  if(config.outputFormat){
    processTestReports(config.language, config.outputFormat)
  }
}

/**
 * Execute installation and test commands
 */
private def executeTests(String installCommand, String testCommand) {
    try {
        echo "Installing dependencies with: ${installCommand}"
        sh "${installCommand}"
        
        echo "Running tests with: ${testCommand}"
        sh "${testCommand}"
    } catch (Exception e) {
        echo "Test execution failed: ${e.message}"
        currentBuild.result = 'FAILURE'
        throw e
    }
}

/**
 * Process test reports based on language and format
 */
private def processTestReports(String language, String format) {
    echo "Processing test reports in ${format} format"
    
    switch(language) {
        case "nodejs":
            if (format == "junit") {
                junit 'test-results/*.xml'
            } else if (format == "cobertura") {
                cobertura coberturaReportFile: 'coverage/cobertura-coverage.xml'
            }
            break
            
        case "mvn":
            junit 'target/surefire-reports/*.xml'
            break
            
        // Add more language-specific report processing
        default:
            echo "No specific report processing for ${language} in ${format} format"
    }
}