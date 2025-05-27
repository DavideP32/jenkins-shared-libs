#!/usr/bin/env groovy

/**
 * Generic SonarQube Scanning Library
 * Supports automatic language detection for Java, Node.js/Angular, Kotlin, Swift, and other languages
 */

def call(Map config) {
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
                name: 'SONAR_PROJECT_KEY', 
                defaultValue: config.defaultProjectKey ?: 'generic-project', 
                description: 'SonarQube project key'
            )
            string(
                name: 'SONAR_PROJECT_NAME', 
                defaultValue: config.defaultProjectName ?: 'Generic Project', 
                description: 'SonarQube project name'
            )
        }
        
        environment {
            SONAR_HOST_URL = credentials('sonar-host-url')
            SONAR_AUTH_TOKEN = credentials('sonar-auth-token')
        }
        
        stages {
            stage('Checkout') {
                steps {
                    script {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${params.BRANCH}"]],
                            userRemoteConfigs: [[url: "${params.REPO_URL}"]]
                        ])
                    }
                }
            }
            
            stage('Detect Project Type') {
                steps {
                    script {
                        env.PROJECT_TYPES = detectProjectTypes()
                        echo "Detected project types: ${env.PROJECT_TYPES}"
                    }
                }
            }
            
            stage('Setup Build Environment') {
                steps {
                    script {
                        setupBuildEnvironment(env.PROJECT_TYPES)
                    }
                }
            }
            
            stage('Build Project') {
                steps {
                    script {
                        buildProject(env.PROJECT_TYPES)
                    }
                }
            }
            
            stage('SonarQube Analysis') {
                steps {
                    script {
                        runSonarAnalysis()
                    }
                }
            }
            
            stage('Quality Gate') {
                steps {
                    script {
                        waitForQualityGate()
                    }
                }
            }
            
            stage('Export Results') {
                steps {
                    script {
                        exportSonarResults()
                    }
                }
            }
        }
        
        post {
            always {
                archiveArtifacts artifacts: 'sonar-results.xml', allowEmptyArchive: true
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: '.',
                    reportFiles: 'sonar-results.xml',
                    reportName: 'SonarQube Results'
                ])
            }
        }
    }
}

def detectProjectTypes() {
    def projectTypes = []
    
    // Java detection
    if (fileExists('pom.xml') || fileExists('build.gradle') || fileExists('build.gradle.kts')) {
        projectTypes.add('java')
        echo "Detected Java project"
    }
    
    // Node.js/Angular detection
    if (fileExists('package.json')) {
        projectTypes.add('nodejs')
        echo "Detected Node.js project"
        
        def packageJson = readJSON file: 'package.json'
        if (packageJson.dependencies && (packageJson.dependencies['@angular/core'] || packageJson.devDependencies['@angular/core'])) {
            projectTypes.add('angular')
            echo "Detected Angular project"
        }
    }
    
    // Kotlin detection
    if (fileExists('build.gradle.kts') || sh(script: 'find . -name "*.kt" -type f | head -1', returnStdout: true).trim()) {
        if (!projectTypes.contains('java')) {
            projectTypes.add('kotlin')
        }
        echo "Detected Kotlin files"
    }
    
    // Swift detection
    if (fileExists('Package.swift') || fileExists('*.xcodeproj') || fileExists('*.xcworkspace') || 
        sh(script: 'find . -name "*.swift" -type f | head -1', returnStdout: true).trim()) {
        projectTypes.add('swift')
        echo "Detected Swift project"
    }
    
    // Python detection
    if (fileExists('requirements.txt') || fileExists('setup.py') || fileExists('pyproject.toml') ||
        sh(script: 'find . -name "*.py" -type f | head -1', returnStdout: true).trim()) {
        projectTypes.add('python')
        echo "Detected Python project"
    }
    
    // C# detection
    if (fileExists('*.sln') || fileExists('*.csproj') || 
        sh(script: 'find . -name "*.cs" -type f | head -1', returnStdout: true).trim()) {
        projectTypes.add('csharp')
        echo "Detected C# project"
    }
    
    // Go detection
    if (fileExists('go.mod') || fileExists('go.sum') ||
        sh(script: 'find . -name "*.go" -type f | head -1', returnStdout: true).trim()) {
        projectTypes.add('go')
        echo "Detected Go project"
    }
    
    // PHP detection
    if (fileExists('composer.json') || 
        sh(script: 'find . -name "*.php" -type f | head -1', returnStdout: true).trim()) {
        projectTypes.add('php')
        echo "Detected PHP project"
    }
    
    // Ruby detection
    if (fileExists('Gemfile') || 
        sh(script: 'find . -name "*.rb" -type f | head -1', returnStdout: true).trim()) {
        projectTypes.add('ruby')
        echo "Detected Ruby project"
    }
    
    if (projectTypes.isEmpty()) {
        projectTypes.add('generic')
        echo "No specific project type detected, using generic scan"
    }
    
    return projectTypes.join(',')
}

def setupBuildEnvironment(String projectTypes) {
    def types = projectTypes.split(',')
    
    types.each { type ->
        switch(type) {
            case 'java':
                sh '''
                    if command -v java >/dev/null 2>&1; then
                        echo "Java already installed: $(java -version)"
                    else
                        echo "Installing Java..."
                        # Add Java installation commands here
                    fi
                '''
                break
            case 'nodejs':
            case 'angular':
                sh '''
                    if command -v node >/dev/null 2>&1; then
                        echo "Node.js already installed: $(node --version)"
                        echo "npm version: $(npm --version)"
                    else
                        echo "Installing Node.js..."
                        # Add Node.js installation commands here
                    fi
                '''
                break
            case 'swift':
                sh '''
                    if command -v swift >/dev/null 2>&1; then
                        echo "Swift already installed: $(swift --version)"
                    else
                        echo "Swift not found. Please ensure Xcode or Swift toolchain is installed."
                    fi
                '''
                break
            case 'python':
                sh '''
                    if command -v python3 >/dev/null 2>&1; then
                        echo "Python already installed: $(python3 --version)"
                    else
                        echo "Installing Python..."
                        # Add Python installation commands here
                    fi
                '''
                break
        }
    }
}

def buildProject(String projectTypes) {
    def types = projectTypes.split(',')
    
    types.each { type ->
        switch(type) {
            case 'java':
                if (fileExists('pom.xml')) {
                    sh 'mvn clean compile test-compile'
                } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
                    sh './gradlew clean compileJava compileTestJava || gradle clean compileJava compileTestJava'
                }
                break
            case 'nodejs':
            case 'angular':
                sh '''
                    npm ci || npm install
                    if npm run | grep -q "build"; then
                        npm run build || echo "Build script not found or failed"
                    fi
                '''
                break
            case 'swift':
                if (fileExists('Package.swift')) {
                    sh 'swift build || echo "Swift build failed"'
                } else {
                    echo "Xcode project detected, skipping command line build"
                }
                break
            case 'python':
                sh '''
                    if [ -f requirements.txt ]; then
                        pip3 install -r requirements.txt || echo "Failed to install requirements"
                    fi
                    python3 -m py_compile $(find . -name "*.py") || echo "Python compilation check completed"
                '''
                break
            case 'csharp':
                sh 'dotnet build || echo "C# build failed"'
                break
            case 'go':
                sh 'go build ./... || echo "Go build failed"'
                break
            case 'php':
                if (fileExists('composer.json')) {
                    sh 'composer install || echo "Composer install failed"'
                }
                break
            case 'ruby':
                if (fileExists('Gemfile')) {
                    sh 'bundle install || echo "Bundle install failed"'
                }
                break
        }
    }
}

def runSonarAnalysis() {
    def sonarProperties = generateSonarProperties()
    
    writeFile file: 'sonar-project.properties', text: sonarProperties
    
    withSonarQubeEnv('SonarQube') {
        sh '''
            sonar-scanner \
                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                -Dsonar.host.url=${SONAR_HOST_URL} \
                -Dsonar.token=${SONAR_AUTH_TOKEN}
        '''
    }
}

def generateSonarProperties() {
    def properties = """
# Project identification
sonar.projectKey=${params.SONAR_PROJECT_KEY}
sonar.projectName=${params.SONAR_PROJECT_NAME}
sonar.projectVersion=1.0

# Source code
sonar.sources=.
sonar.sourceEncoding=UTF-8

# Exclusions
sonar.exclusions=**/node_modules/**,**/target/**,**/build/**,**/.git/**,**/vendor/**,**/Pods/**

# Language-specific settings
sonar.java.source=8
sonar.java.target=8
sonar.java.binaries=target/classes,build/classes
sonar.java.test.binaries=target/test-classes,build/classes/test

# JavaScript/TypeScript settings
sonar.javascript.lcov.reportPaths=coverage/lcov.info
sonar.typescript.lcov.reportPaths=coverage/lcov.info

# Swift settings (if supported)
sonar.swift.coverage.reportPaths=coverage.xml

# Generic settings
sonar.scm.provider=git
"""
    
    return properties
}

def waitForQualityGate() {
    timeout(time: 10, unit: 'MINUTES') {
        def qg = waitForQualityGate()
        if (qg.status != 'OK') {
            echo "Quality Gate failed: ${qg.status}"
            // Continue execution but mark as unstable
            currentBuild.result = 'UNSTABLE'
        } else {
            echo "Quality Gate passed"
        }
    }
}

def exportSonarResults() {
    script {
        def projectKey = params.SONAR_PROJECT_KEY
        def sonarHostUrl = env.SONAR_HOST_URL
        def sonarToken = env.SONAR_AUTH_TOKEN
        
        // Export issues in XML format
        sh """
            curl -u "${sonarToken}:" \
                "${sonarHostUrl}/api/issues/search?componentKeys=${projectKey}&format=xml" \
                -o sonar-issues.xml || echo "Failed to export issues"
        """
        
        // Export measures in XML format
        sh """
            curl -u "${sonarToken}:" \
                "${sonarHostUrl}/api/measures/component?component=${projectKey}&metricKeys=ncloc,complexity,violations,coverage,duplicated_lines_density,reliability_rating,security_rating,maintainability_rating" \
                -H "Accept: application/xml" \
                -o sonar-measures.xml || echo "Failed to export measures"
        """
        
        // Combine results into a single XML file
        sh '''
            cat > sonar-results.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<sonarqube-results>
    <project-key>''' + projectKey + '''</project-key>
    <scan-date>''' + new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'") + '''</scan-date>
    <repository-url>''' + params.REPO_URL + '''</repository-url>
    <branch>''' + params.BRANCH + '''</branch>
EOF
            
            if [ -f sonar-issues.xml ]; then
                echo "    <issues>" >> sonar-results.xml
                grep -v "<?xml" sonar-issues.xml >> sonar-results.xml || echo "No issues data" >> sonar-results.xml
                echo "    </issues>" >> sonar-results.xml
            fi
            
            if [ -f sonar-measures.xml ]; then
                echo "    <measures>" >> sonar-results.xml
                grep -v "<?xml" sonar-measures.xml >> sonar-results.xml || echo "No measures data" >> sonar-results.xml
                echo "    </measures>" >> sonar-results.xml
            fi
            
            echo "</sonarqube-results>" >> sonar-results.xml
        '''
        
        echo "SonarQube results exported to sonar-results.xml"
    }
}

// Additional utility functions
def getProjectName() {
    def repoUrl = params.REPO_URL
    def projectName = repoUrl.tokenize('/').last().replace('.git', '')
    return projectName
}

def getSonarQubeMetrics() {
    return [
        'ncloc',
        'complexity',
        'violations',
        'coverage',
        'duplicated_lines_density',
        'reliability_rating',
        'security_rating',
        'maintainability_rating',
        'bugs',
        'vulnerabilities',
        'code_smells'
    ]
}