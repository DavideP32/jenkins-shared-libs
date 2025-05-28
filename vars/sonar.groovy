def call(Map config = [:]) {
    def sonarServer = config.get('sonarServer', 'sq1')
    def projectKey = config.get('projectKey', '')

    withSonarQubeEnv(sonarServer) {
        script {
            env.SONAR_SCANNER = tool 'SonarScanner'
            env.SONAR_TOKEN = credentials('jenkins-sonar')

            if (!env.SONAR_SCANNER?.trim()) {
                error "SonarScanner tool path is empty. Check Jenkins tool configuration."
            }

            echo "DEBUG - SONAR_SCANNER path: ${env.SONAR_SCANNER}"
            echo "DEBUG - PROJECT_KEY: ${projectKey}"

            // Trova pom.xml se presente
            def pomPath = sh(
                script: "find . -name pom.xml | head -n 1",
                returnStdout: true
            ).trim()

            if (pomPath) {
                def pomDir = sh(
                    script: "dirname ${pomPath}",
                    returnStdout: true
                ).trim()

                echo "Trovato pom.xml in: ${pomDir}"
                dir(pomDir) {
                    sh """
                        mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=${projectKey} \
                        -Dsonar.login=${env.SONAR_TOKEN}
                    """
                }
            } else {
                echo "Nessun pom.xml trovato. Uso sonar-scanner CLI."
                // Usare riga singola per evitare errori di shell
                sh "${env.SONAR_SCANNER}/bin/sonar-scanner -Dsonar.projectKey=${projectKey} -Dsonar.sources=. -Dsonar.login=${env.SONAR_TOKEN}"
            }
        }
    }
}
