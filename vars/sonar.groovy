def call(Map config = [:]) {
    def sonarServer = config.get('sonarServer', 'sq1')
    def projectKey = config.get('projectKey', '')

    withSonarQubeEnv(sonarServer) {
        script {
            env.SONAR_SCANNER = tool 'SonarScanner'
            env.SONAR_TOKEN = credentials('jenkins-sonar')

            // Autodetect pom.xml
            def pomDir = sh(
                script: "find . -name pom.xml | head -n 1 | xargs dirname",
                returnStdout: true
            ).trim()

            if (pomDir) {
                echo "Trovato pom.xml in: ${pomDir}"
                dir(pomDir) {
                    sh """
                        mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=${projectKey} \
                        -Dsonar.login=$SONAR_TOKEN
                    """
                }
            } else {
                echo "Nessun pom.xml trovato. Uso sonar-scanner CLI."
                sh """
                    ${env.SONAR_SCANNER}/bin/sonar-scanner \
                    -Dsonar.projectKey=${projectKey} \
                    -Dsonar.sources=. \
                    -Dsonar.login=$SONAR_TOKEN
                """
            }
        }
    }
}