def call(Map config) {
    def sonarServer = config.sonarServer ?: 'MySonar'
    pipeline {
        agent any

        parameters {
            string(
                name: 'REPO_URL',
                defaultValue: config.defaultRepoUrl ?: 'https://github.com/example/repo.git',
                description: 'GitHub repository URL'
            )
            string(
                name: 'BRANCH',
                defaultValue: config.defaultBranch ?: 'main',
                description: 'Branch to scan'
            )
            string(
                name: 'SONAR_TOKEN',
                defaultValue: config.defaultToken ?: 'jenkins-sonar',
                description: 'Token for Sonar Connection'
            )
        }

        environment {
            SONAR_HOST_URL = credentials('http://sonarqube:9000')
            SONAR_AUTH_TOKEN = credentials(SONAR_TOKEN)
        }

        stages {
            stage('Checkout') {
                steps {
                    git branch: ${ BRANCH }, url: ${ REPO_URL }
                }
            }

            stage('sonarqube analysis') {
                steps {
                    withSonarQubeEnv(sonarServer) {
                        sh """
                            mvn clean verify sonar:sonar \
                            -Dsonar.projectKey=${projectKey} \
                            -Dsonar.login=$SONAR_TOKEN
                            """
                    }
                }
            }
        }
    }
}
