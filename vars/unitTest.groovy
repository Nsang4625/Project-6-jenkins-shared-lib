def call(Map config = [:]) {
  def installDepCommand

  def coverageCommand
  switch(config.language) {
    case "nodejs":
      installDepCommand = "npm install"
      coverageCommand = "npm run test"
    case "mvn":
      installDepCommand = "mvn compile"
      coverageCommand = "mvn test"
    break
  }
  sh """
  ${installDepCommand}
  ${coverageCommand}
  """
}