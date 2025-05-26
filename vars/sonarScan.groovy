def call(Map config = [:]) {
  def projectKey = config.projectKey ?: error("Missing 'projectKey'")
  def repoUrl = config.repoUrl ?: error("Missing 'repoUrl'")
  def sonarServer = config.sonarServer ?: 'MySonar'

  pipeline {
    agent any

    tools {
      maven 'Maven3'
    }

    environment {
      SONAR_TOKEN = credentials('jenkins-sonar')
    }

    stages {
      stage('Checkout') {
        steps {
          git branch: 'main', url: repoUrl
        }
      }

      stage('SonarQube Analysis') {
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

      stage("Quality Gate") {
        steps {
          waitForQualityGate abortPipeline: true
        }
      }
    }
  }
}
