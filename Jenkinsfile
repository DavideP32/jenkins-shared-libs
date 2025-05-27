#!/usr/bin/env groovy

@Library('my-shared-library') _

pipeline {
    agent any
    
    parameters {
        string(
            name: 'REPO_URL',
            defaultValue: 'https://github.com/example/repository.git',
            description: 'GitHub repository URL to scan'
        )
        string(
            name: 'BRANCH',
            defaultValue: 'main',
            description: 'Branch to checkout and scan'
        )
        string(
            name: 'SONAR_PROJECT_KEY',
            defaultValue: '',
            description: 'SonarQube project key (leave empty for auto-generation)'
        )
        string(
            name: 'SONAR_PROJECT_NAME',
            defaultValue: '',
            description: 'SonarQube project name (leave empty for auto-generation)'
        )
        choice(
            name: 'SCAN_TYPE',
            choices: ['AUTO_DETECT', 'JAVA', 'NODEJS', 'KOTLIN', 'SWIFT', 'PYTHON', 'CSHARP', 'GO', 'PHP', 'RUBY'],
            description: 'Force specific language scan or use auto-detection'
        )
        booleanParam(
            name: 'FAIL_ON_QUALITY_GATE',
            defaultValue: false,
            description: 'Fail the build if Quality Gate fails'
        )
    }
    
    environment {
        // SonarQube environment variables
        SONAR_HOST_URL = credentials('sonar-host-url')
        SONAR_AUTH_TOKEN = credentials('sonar-auth-token')
        
        // Auto-generate project identifiers if not provided
        PROJECT_KEY = "${params.SONAR_PROJECT_KEY ?: generateProjectKey()}"
        PROJECT_NAME = "${params.SONAR_PROJECT_NAME ?: generateProjectName()}"
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        skipStagesAfterUnstable()
    }
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "=== SonarQube Code Analysis Pipeline ==="
                    echo "Repository: ${params.REPO_URL}"
                    echo "Branch: ${params.BRANCH}"
                    echo "Project Key: ${env.PROJECT_KEY}"
                    echo "Project Name: ${env.PROJECT_NAME}"
                    echo "Scan Type: ${params.SCAN_TYPE}"
                    
                    // Set build description
                    currentBuild.description = "Scanning ${params.REPO_URL} (${params.BRANCH})"
                }
            }
        }
        
        stage('Checkout Repository') {
            steps {
                script {
                    try {
                        echo "Checking out repository: ${params.REPO_URL}"
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${params.BRANCH}"]],
                            userRemoteConfigs: [[
                                url: "${params.REPO_URL}",
                                credentialsId: 'github-credentials' // Configure this credential in Jenkins
                            ]],
                            extensions: [
                                [$class: 'CleanBeforeCheckout'],
                                [$class: 'CloneOption', depth: 1, shallow: true]
                            ]
                        ])
                        
                        // Get commit information
                        env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                        env.GIT_AUTHOR = sh(returnStdout: true, script: 'git log -1 --pretty=format:"%an"').trim()
                        
                        echo "Checked out commit: ${env.GIT_COMMIT}"
                        echo "Author: ${env.GIT_AUTHOR}"
                        
                    } catch (Exception e) {
                        error "Failed to checkout repository: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Detect Project Structure') {
            steps {
                script {
                    if (params.SCAN_TYPE == 'AUTO_DETECT') {
                        env.DETECTED_LANGUAGES = detectProjectLanguages()
                        echo "Auto-detected languages: ${env.DETECTED_LANGUAGES}"
                    } else {
                        env.DETECTED_LANGUAGES = params.SCAN_TYPE.toLowerCase()
                        echo "Using forced scan type: ${env.DETECTED_LANGUAGES}"
                    }
                    
                    // Analyze project structure
                    analyzeProjectStructure()
                }
            }
        }
        
        stage('Setup Build Tools') {
            steps {
                script {
                    setupBuildTools(env.DETECTED_LANGUAGES)
                }
            }
        }
        
        stage('Build & Compile') {
            steps {
                script {
                    buildProject(env.DETECTED_LANGUAGES)
                }
            }
        }
        
        stage('Pre-Analysis Validation') {
            steps {
                script {
                    validateSonarQubeConnection()
                    createSonarProjectIfNeeded()
                }
            }
        }
        
        stage('SonarQube Code Analysis') {
            steps {
                script {
                    performSonarAnalysis()
                }
            }
        }
        
        stage('Quality Gate Check') {
            steps {
                script {
                    checkQualityGate()
                }
            }
        }
        
        stage('Export Analysis Results') {
            steps {
                script {
                    exportAnalysisResults()
                }
            }
        }
        
        stage('Generate Reports') {
            steps {
                script {
                    generateDetailedReports()
                }
            }
        }
    }
    
    post {
        always {
            echo "Pipeline execution completed"
            
            // Archive artifacts
            archiveArtifacts artifacts: 'sonar-results.xml, sonar-detailed-report.xml, sonar-project.properties', 
                            allowEmptyArchive: true,
                            fingerprint: true
            
            // Publish HTML reports
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'sonar-detailed-report.xml',
                reportName: 'SonarQube Analysis Report',
                reportTitles: 'Code Quality Analysis'
            ])
        }
        
        success {
            echo "✅ SonarQube analysis completed successfully"
            // You can add notifications here (Slack, email, etc.)
        }
        
        failure {
            echo "❌ SonarQube analysis failed"
            // You can add failure notifications here
        }
        
        unstable {
            echo "⚠️ SonarQube analysis completed with warnings"
        }
        
        cleanup {
            // Clean up workspace
            cleanWs(cleanWhenNotBuilt: false,
                   deleteDirs: true,
                   disableDeferredWipeout: true,
                   notFailBuild: true)
        }
    }
}

// ===== HELPER FUNCTIONS =====

def generateProjectKey() {
    def repoUrl = params.REPO_URL
    def repoName = repoUrl.tokenize('/').last().replace('.git', '')
    def orgName = repoUrl.tokenize('/').get(-2)
    return "${orgName}-${repoName}".toLowerCase().replaceAll(/[^a-z0-9\-_]/, '-')
}

def generateProjectName() {
    def repoUrl = params.REPO_URL
    def repoName = repoUrl.tokenize('/').last().replace('.git', '')
    return repoName.replaceAll(/[-_]/, ' ').split(' ').collect { 
        it.toLowerCase().capitalize() 
    }.join(' ')
}

def detectProjectLanguages() {
    def languages = []
    
    // Java detection
    if (fileExists('pom.xml') || fileExists('build.gradle') || fileExists('build.gradle.kts')) {
        languages.add('java')
    }
    
    // Node.js/JavaScript/TypeScript detection
    if (fileExists('package.json')) {
        languages.add('javascript')
        
        def packageJson = readJSON file: 'package.json'
        if (packageJson.dependencies && packageJson.dependencies.containsKey('@angular/core')) {
            languages.add('angular')
        }
        if (packageJson.devDependencies && packageJson.devDependencies.containsKey('typescript')) {
            languages.add('typescript')
        }
    }
    
    // Kotlin detection
    if (fileExists('build.gradle.kts') || 
        sh(script: 'find . -name "*.kt" -type f | head -1', returnStatus: true) == 0) {
        languages.add('kotlin')
    }
    
    // Swift detection
    if (fileExists('Package.swift') || fileExists('*.xcodeproj') || fileExists('*.xcworkspace') ||
        sh(script: 'find . -name "*.swift" -type f | head -1', returnStatus: true) == 0) {
        languages.add('swift')
    }
    
    // Python detection
    if (fileExists('requirements.txt') || fileExists('setup.py') || fileExists('pyproject.toml') ||
        sh(script: 'find . -name "*.py" -type f | head -1', returnStatus: true) == 0) {
        languages.add('python')
    }
    
    // C# detection
    if (sh(script: 'find . -name "*.sln" -o -name "*.csproj" | head -1', returnStatus: true) == 0 ||
        sh(script: 'find . -name "*.cs" -type f | head -1', returnStatus: true) == 0) {
        languages.add('csharp')
    }
    
    // Go detection
    if (fileExists('go.mod') || fileExists('go.sum') ||
        sh(script: 'find . -name "*.go" -type f | head -1', returnStatus: true) == 0) {
        languages.add('go')
    }
    
    // PHP detection
    if (fileExists('composer.json') ||
        sh(script: 'find . -name "*.php" -type f | head -1', returnStatus: true) == 0) {
        languages.add('php')
    }
    
    // Ruby detection
    if (fileExists('Gemfile') ||
        sh(script: 'find . -name "*.rb" -type f | head -1', returnStatus: true) == 0) {
        languages.add('ruby')
    }
    
    return languages.isEmpty() ? ['generic'] : languages
}

def analyzeProjectStructure() {
    sh '''
        echo "=== Project Structure Analysis ==="
        find . -type f -name "*.java" | wc -l | xargs echo "Java files:"
        find . -type f -name "*.js" -o -name "*.ts" | wc -l | xargs echo "JavaScript/TypeScript files:"
        find . -type f -name "*.kt" | wc -l | xargs echo "Kotlin files:"
        find . -type f -name "*.swift" | wc -l | xargs echo "Swift files:"
        find . -type f -name "*.py" | wc -l | xargs echo "Python files:"
        find . -type f -name "*.cs" | wc -l | xargs echo "C# files:"
        find . -type f -name "*.go" | wc -l | xargs echo "Go files:"
        find . -type f -name "*.php" | wc -l | xargs echo "PHP files:"
        find . -type f -name "*.rb" | wc -l | xargs echo "Ruby files:"
        echo "================================="
    '''
}

def setupBuildTools(String languages) {
    def languageList = languages.split(',')
    
    languageList.each { lang ->
        switch(lang.trim()) {
            case 'java':
                sh '''
                    echo "Setting up Java environment..."
                    java -version || echo "Java not found"
                    mvn -version || echo "Maven not found"
                    gradle -version || echo "Gradle not found"
                '''
                break
            case 'javascript':
            case 'typescript':
            case 'angular':
                sh '''
                    echo "Setting up Node.js environment..."
                    node --version || echo "Node.js not found"
                    npm --version || echo "npm not found"
                '''
                break
            case 'swift':
                sh '''
                    echo "Setting up Swift environment..."
                    swift --version || echo "Swift not found"
                    xcodebuild -version || echo "Xcode not found"
                '''
                break
            case 'python':
                sh '''
                    echo "Setting up Python environment..."
                    python3 --version || echo "Python not found"
                    pip3 --version || echo "pip not found"
                '''
                break
        }
    }
}

def buildProject(String languages) {
    def languageList = languages.split(',')
    
    languageList.each { lang ->
        switch(lang.trim()) {
            case 'java':
                if (fileExists('pom.xml')) {
                    sh 'mvn clean compile test-compile -DskipTests=true'
                } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
                    sh './gradlew clean compileJava compileTestJava -x test || gradle clean compileJava compileTestJava -x test'
                }
                break
            case 'javascript':
            case 'typescript':
            case 'angular':
                sh '''
                    if [ -f package-lock.json ]; then
                        npm ci
                    else
                        npm install
                    fi
                    
                    if npm run | grep -q "build"; then
                        npm run build || echo "Build failed, continuing with analysis"
                    fi
                '''
                break
            case 'swift':
                if (fileExists('Package.swift')) {
                    sh 'swift build || echo "Swift build failed, continuing with analysis"'
                }
                break
            case 'python':
                sh '''
                    if [ -f requirements.txt ]; then
                        pip3 install -r requirements.txt || echo "Failed to install requirements"
                    fi
                '''
                break
        }
    }
}

def validateSonarQubeConnection() {
    sh '''
        echo "Validating SonarQube connection..."
        curl -s -u "${SONAR_AUTH_TOKEN}:" "${SONAR_HOST_URL}/api/system/status" | grep -q "UP" || {
            echo "SonarQube server is not accessible"
            exit 1
        }
        echo "SonarQube connection validated"
    '''
}

def createSonarProjectIfNeeded() {
    sh """
        echo "Checking if project exists in SonarQube..."
        PROJECT_EXISTS=\$(curl -s -u "\${SONAR_AUTH_TOKEN}:" "\${SONAR_HOST_URL}/api/projects/search?projects=${env.PROJECT_KEY}" | grep -c '"key":"${env.PROJECT_KEY}"' || echo "0")
        
        if [ "\$PROJECT_EXISTS" = "0" ]; then
            echo "Creating new SonarQube project..."
            curl -X POST -u "\${SONAR_AUTH_TOKEN}:" "\${SONAR_HOST_URL}/api/projects/create" \
                -d "project=${env.PROJECT_KEY}&name=${env.PROJECT_NAME}" || echo "Failed to create project"
        else
            echo "Project already exists in SonarQube"
        fi
    """
}

def performSonarAnalysis() {
    // Generate sonar-project.properties
    def sonarProps = generateSonarProperties()
    writeFile file: 'sonar-project.properties', text: sonarProps
    
    echo "Generated SonarQube properties:"
    sh 'cat sonar-project.properties'
    
    // Run SonarQube scanner
    withSonarQubeEnv('SonarQube') {
        sh '''
            echo "Starting SonarQube analysis..."
            sonar-scanner \
                -Dsonar.projectKey=${PROJECT_KEY} \
                -Dsonar.projectName="${PROJECT_NAME}" \
                -Dsonar.projectVersion=1.0-${BUILD_NUMBER} \
                -Dsonar.sources=. \
                -Dsonar.host.url=${SONAR_HOST_URL} \
                -Dsonar.token=${SONAR_AUTH_TOKEN} \
                -Dsonar.scm.revision=${GIT_COMMIT} \
                -Dsonar.branch.name=${BRANCH_NAME} \
                -Dsonar.working.directory=.sonarqube
            echo "SonarQube analysis completed"
        '''
    }
}

def generateSonarProperties() {
    def languages = env.DETECTED_LANGUAGES.split(',')
    def props = """# SonarQube Project Configuration
# Generated automatically by Jenkins Pipeline

# Project Information
sonar.projectKey=${env.PROJECT_KEY}
sonar.projectName=${env.PROJECT_NAME}
sonar.projectVersion=1.0-${env.BUILD_NUMBER}

# Source Code Settings
sonar.sources=.
sonar.sourceEncoding=UTF-8

# Git Information
sonar.scm.provider=git
sonar.scm.revision=${env.GIT_COMMIT}

# General Exclusions
sonar.exclusions=**/node_modules/**,**/target/**,**/build/**,**/.git/**,**/vendor/**,**/Pods/**,**/.sonar/**,**/.sonarqube/**,**/coverage/**,**/dist/**,**/bin/**,**/obj/**

# Test Exclusions  
sonar.test.exclusions=**/test/**,**/tests/**,**/*Test.java,**/*Test.kt,**/*Test.swift,**/*.test.js,**/*.test.ts,**/*.spec.js,**/*.spec.ts

"""

    // Add language-specific configurations
    if (languages.contains('java')) {
        props += """
# Java Configuration
sonar.java.source=8
sonar.java.target=8
sonar.java.binaries=target/classes,build/classes/java/main,build/classes/kotlin/main
sonar.java.test.binaries=target/test-classes,build/classes/java/test,build/classes/kotlin/test
sonar.java.libraries=target/dependency/*.jar,build/libs/*.jar
sonar.junit.reportPaths=target/surefire-reports/*.xml,build/test-results/test/*.xml

"""
    }
    
    if (languages.contains('javascript') || languages.contains('typescript') || languages.contains('angular')) {
        props += """
# JavaScript/TypeScript Configuration
sonar.javascript.lcov.reportPaths=coverage/lcov.info,coverage/lcov.dat
sonar.typescript.lcov.reportPaths=coverage/lcov.info,coverage/lcov.dat
sonar.javascript.file.suffixes=.js,.jsx
sonar.typescript.file.suffixes=.ts,.tsx
sonar.typescript.tsconfigPath=tsconfig.json

"""
    }
    
    if (languages.contains('kotlin')) {
        props += """
# Kotlin Configuration
sonar.kotlin.source.version=1.8
sonar.kotlin.binaries=build/classes/kotlin/main
sonar.kotlin.test.binaries=build/classes/kotlin/test

"""
    }
    
    if (languages.contains('swift')) {
        props += """
# Swift Configuration
sonar.swift.coverage.reportPaths=coverage.xml,sonarqube-generic-coverage.xml
sonar.swift.file.suffixes=.swift

"""
    }
    
    if (languages.contains('python')) {
        props += """
# Python Configuration
sonar.python.coverage.reportPaths=coverage.xml,coverage-reports/coverage.xml
sonar.python.xunit.reportPath=test-reports/*.xml

"""
    }
    
    if (languages.contains('csharp')) {
        props += """
# C# Configuration
sonar.cs.dotcover.reportsPaths=coverage.xml
sonar.cs.nunit.reportsPaths=TestResults/*.xml

"""
    }
    
    if (languages.contains('go')) {
        props += """
# Go Configuration
sonar.go.coverage.reportPaths=coverage.out,coverage.xml

"""
    }
    
    if (languages.contains('php')) {
        props += """
# PHP Configuration
sonar.php.coverage.reportPaths=coverage.xml,clover.xml
sonar.php.tests.reportPath=phpunit.xml

"""
    }
    
    if (languages.contains('ruby')) {
        props += """
# Ruby Configuration
sonar.ruby.coverage.reportPath=coverage/.resultset.json

"""
    }
    
    return props
}

def checkQualityGate() {
    timeout(time: 10, unit: 'MINUTES') {
        script {
            def qg = waitForQualityGate()
            
            echo "Quality Gate Status: ${qg.status}"
            
            if (qg.status != 'OK') {
                echo "❌ Quality Gate failed with status: ${qg.status}"
                
                if (params.FAIL_ON_QUALITY_GATE) {
                    error("Quality Gate failed: ${qg.status}")
                } else {
                    echo "⚠️ Continuing despite Quality Gate failure (FAIL_ON_QUALITY_GATE=false)"
                    currentBuild.result = 'UNSTABLE'
                }
            } else {
                echo "✅ Quality Gate passed successfully"
            }
        }
    }
}

def exportAnalysisResults() {
    script {
        echo "Exporting SonarQube analysis results..."
        
        def projectKey = env.PROJECT_KEY
        def sonarHostUrl = env.SONAR_HOST_URL
        def sonarToken = env.SONAR_AUTH_TOKEN
        
        // Export comprehensive results
        sh """
            # Create results directory
            mkdir -p sonar-exports
            
            # Export issues
            echo "Exporting issues..."
            curl -s -u "${sonarToken}:" \
                "${sonarHostUrl}/api/issues/search?componentKeys=${projectKey}&ps=500&format=json" \
                -o sonar-exports/issues.json
            
            # Export measures
            echo "Exporting measures..."
            curl -s -u "${sonarToken}:" \
                "${sonarHostUrl}/api/measures/component?component=${projectKey}&metricKeys=ncloc,complexity,violations,coverage,duplicated_lines_density,reliability_rating,security_rating,maintainability_rating,bugs,vulnerabilities,code_smells,sqale_index,sqale_rating" \
                -o sonar-exports/measures.json
            
            # Export project status
            echo "Exporting project status..."
            curl -s -u "${sonarToken}:" \
                "${sonarHostUrl}/api/qualitygates/project_status?projectKey=${projectKey}" \
                -o sonar-exports/quality-gate.json
                
            # Export hotspots
            echo "Exporting security hotspots..."
            curl -s -u "${sonarToken}:" \
                "${sonarHostUrl}/api/hotspots/search?projectKey=${projectKey}&ps=500" \
                -o sonar-exports/hotspots.json || echo "Failed to export hotspots"
        """
        
        // Convert to XML format
        convertResultsToXML()
    }
}

def convertResultsToXML() {
    sh '''
        echo "Converting results to XML format..."
        
        cat > sonar-results.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<sonarqube-analysis>
    <metadata>
        <project-key>''' + env.PROJECT_KEY + '''</project-key>
        <project-name>''' + env.PROJECT_NAME + '''</project-name>
        <analysis-date>''' + new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'") + '''</analysis-date>
        <repository-url>''' + params.REPO_URL + '''</repository-url>
        <branch>''' + params.BRANCH + '''</branch>
        <commit>''' + env.GIT_COMMIT + '''</commit>
        <build-number>''' + env.BUILD_NUMBER + '''</build-number>
        <detected-languages>''' + env.DETECTED_LANGUAGES + '''</detected-languages>
    </metadata>
EOF
        
        # Process measures
        if [ -f sonar-exports/measures.json ]; then
            echo "    <measures>" >> sonar-results.xml
            
            # Extract key metrics using jq if available, otherwise use basic parsing
            if command -v jq >/dev/null 2>&1; then
                jq -r '.component.measures[] | "        <metric key=\\"\\(.metric)\\" value=\\"\\(.value // "N/A")\\" />"' sonar-exports/measures.json >> sonar-results.xml 2>/dev/null || echo "        <error>Failed to parse measures</error>" >> sonar-results.xml
            else
                echo "        <error>jq not available for JSON parsing</error>" >> sonar-results.xml
            fi
            
            echo "    </measures>" >> sonar-results.xml
        fi
        
        # Process issues
        if [ -f sonar-exports/issues.json ]; then
            echo "    <issues>" >> sonar-results.xml
            
            if command -v jq >/dev/null 2>&1; then
                jq -r '.issues[] | "        <issue><key>\\(.key)</key><rule>\\(.rule)</rule><severity>\\(.severity)</severity><component>\\(.component)</component><message>\\(.message | gsub("&"; "&amp;") | gsub("<"; "&lt;") | gsub(">"; "&gt;"))</message><line>\\(.textRange.startLine // "N/A")</line><type>\\(.type)</type></issue>"' sonar-exports/issues.json >> sonar-results.xml 2>/dev/null || echo "        <error>Failed to parse issues</error>" >> sonar-results.xml
            else
                echo "        <error>jq not available for JSON parsing</error>" >> sonar-results.xml
            fi
            
            echo "    </issues>" >> sonar-results.xml
        fi
        
        # Process quality gate
        if [ -f sonar-exports/quality-gate.json ]; then
            echo "    <quality-gate>" >> sonar-results.xml
            
            if command -v jq >/dev/null 2>&1; then
                QG_STATUS=$(jq -r '.projectStatus.status' sonar-exports/quality-gate.json 2>/dev/null || echo "UNKNOWN")
                echo "        <status>${QG_STATUS}</status>" >> sonar-results.xml
                
                jq -r '.projectStatus.conditions[]? | "        <condition><metric>\\(.metricKey)</metric><operator>\\(.comparator)</operator><threshold>\\(.errorThreshold // .warningThreshold // "N/A")</threshold><status>\\(.status)</status><value>\\(.actualValue // "N/A")</value></condition>"' sonar-exports/quality-gate.json >> sonar-results.xml 2>/dev/null || echo "        <error>Failed to parse quality gate conditions</error>" >> sonar-results.xml
            else
                echo "        <error>jq not available for JSON parsing</error>" >> sonar-results.xml
            fi
            
            echo "    </quality-gate>" >> sonar-results.xml
        fi
        
        # Process hotspots
        if [ -f sonar-exports/hotspots.json ]; then
            echo "    <security-hotspots>" >> sonar-results.xml
            
            if command -v jq >/dev/null 2>&1; then
                jq -r '.hotspots[]? | "        <hotspot><key>\\(.key)</key><component>\\(.component)</component><securityCategory>\\(.securityCategory)</securityCategory><vulnerabilityProbability>\\(.vulnerabilityProbability)</vulnerabilityProbability><status>\\(.status)</status><line>\\(.textRange.startLine // "N/A")</line></hotspot>"' sonar-exports/hotspots.json >> sonar-results.xml 2>/dev/null || echo "        <error>Failed to parse hotspots</error>" >> sonar-results.xml
            else
                echo "        <error>jq not available for JSON parsing</error>" >> sonar-results.xml
            fi
            
            echo "    </security-hotspots>" >> sonar-results.xml
        fi
        
        echo "</sonarqube-analysis>" >> sonar-results.xml
        
        echo "✅ Results exported to sonar-results.xml"
    '''
}

def generateDetailedReports() {
    sh '''
        echo "Generating detailed HTML report..."
        
        cat > sonar-detailed-report.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="data:text/xsl;base64,PHhzbDpzdHlsZXNoZWV0IHZlcnNpb249IjEuMCIgeG1sbnM6eHNsPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L1hTTC9UcmFuc2Zvcm0iPgo8eHNsOm91dHB1dCBtZXRob2Q9Imh0bWwiLz4KICA8eHNsOnRlbXBsYXRlIG1hdGNoPSIvIj4KICAgIDxodG1sPgogICAgICA8aGVhZD4KICAgICAgICA8dGl0bGU+U29uYXJRdWJlIEFuYWx5c2lzIFJlcG9ydDwvdGl0bGU+CiAgICAgICAgPHN0eWxlPgogICAgICAgICAgYm9keSB7IGZvbnQtZmFtaWx5OiBBcmlhbCwgc2Fucy1zZXJpZjsgbWFyZ2luOiAyMHB4OyB9CiAgICAgICAgICAuaGVhZGVyIHsgYmFja2dyb3VuZC1jb2xvcjogIzAwN2NiYTsgY29sb3I6IHdoaXRlOyBwYWRkaW5nOiAyMHB4OyB9CiAgICAgICAgICAubWV0cmljIHsgYmFja2dyb3VuZC1jb2xvcjogI2Y5ZjlmOTsgcGFkZGluZzogMTBweDsgbWFyZ2luOiA1cHg7IGJvcmRlci1sZWZ0OiA0cHggc29saWQgIzAwN2NiYTsgfQogICAgICAgICAgLmlzc3VlIHsgYmFja2dyb3VuZC1jb2xvcjogI2ZmZjNjZDsgcGFkZGluZzogMTBweDsgbWFyZ2luOiA1cHg7IGJvcmRlci1sZWZ0OiA0cHggc29saWQgI2ZmOTgwMDsgfQogICAgICAgIDwvc3R5bGU+CiAgICAgIDwvaGVhZD4KICAgICAgPGJvZHk+CiAgICAgICAgPGRpdiBjbGFzcz0iaGVhZGVyIj4KICA8aDE+U29uYXJRdWJlIEFuYWx5c2lzIFJlcG9ydDwvaDE+CiAgPHA+UHJvamVjdDogPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Ii8vc29uYXJxdWJlLWFuYWx5c2lzL21ldGFkYXRhL3Byb2plY3QtbmFtZSIvPjwvcD4KICA8cD5EYXRlOiA8eHNsOnZhbHVlLW9mIHNlbGVjdD0iLy9zb25hcnF1YmUtYW5hbHlzaXMvbWV0YWRhdGEvYW5hbHlzaXMtZGF0ZSIvPjwvcD4KPC9kaXY+CjxoMj5NZXRyaWNzPC9oMj4KPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Ii8vc29uYXJxdWJlLWFuYWx5c2lzL21lYXN1cmVzL21ldHJpYyI+CiAgPGRpdiBjbGFzcz0ibWV0cmljIj4KICA8c3Ryb25nPjx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJAa2V5Ii8+PC9zdHJvbmc+OiA8eHNsOnZhbHVlLW9mIHNlbGVjdD0iQHZhbHVlIi8+CiAgPC9kaXY+CjwveHNsOmZvci1lYWNoPgo8aDI+SXNzdWVzPC9oMj4KPHhzbDpmb3ItZWFjaCBzZWxlY3Q9Ii8vc29uYXJxdWJlLWFuYWx5c2lzL2lzc3Vlcy9pc3N1ZSI+CiAgPGRpdiBjbGFzcz0iaXNzdWUiPgogIDxzdHJvbmc+PHhzbDp2YWx1ZS1vZiBzZWxlY3Q9InNldmVyaXR5Ii8+PC9zdHJvbmc+IC0gPHhzbDp2YWx1ZS1vZiBzZWxlY3Q9Im1lc3NhZ2UiLz4KICA8YnIvPkZpbGU6IDx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJjb21wb25lbnQiLz4gKExpbmU6IDx4c2w6dmFsdWUtb2Ygc2VsZWN0PSJsaW5lIi8+KQogIDwvZGl2Pgo8L3hzbDpmb3ItZWFjaD4KPC9ib2R5PjwvaHRtbD48L3hzbDp0ZW1wbGF0ZT48L3hzbDpzdHlsZXNoZWV0Pg=="?>
<sonarqube-analysis-report>
EOF
        
        # Copy content from main results file
        if [ -f sonar-results.xml ]; then
            tail -n +2 sonar-results.xml | head -n -1 >> sonar-detailed-report.xml
        fi
        
        echo "</sonarqube-analysis-report>" >> sonar-detailed-report.xml
        
        echo "✅ Detailed report generated"
    '''
}

// ===== UTILITY FUNCTIONS =====

def getRepositoryInfo() {
    return [
        url: params.REPO_URL,
        branch: params.BRANCH,
        commit: env.GIT_COMMIT,
        author: env.GIT_AUTHOR
    ]
}

def getSonarQubeUrl() {
    return "${env.SONAR_HOST_URL}/dashboard?id=${env.PROJECT_KEY}"
}

def notifyResults(String status) {
    def repoInfo = getRepositoryInfo()
    def sonarUrl = getSonarQubeUrl()
    
    def message = """
SonarQube Analysis ${status}
Project: ${env.PROJECT_NAME}
Repository: ${repoInfo.url}
Branch: ${repoInfo.branch}
Commit: ${repoInfo.commit}
Languages: ${env.DETECTED_LANGUAGES}
SonarQube Dashboard: ${sonarUrl}
Build: ${env.BUILD_URL}
    """.trim()
    
    echo message
    
    // Add your notification logic here (Slack, email, etc.)
    // Example:
    // slackSend(message: message, color: status == 'SUCCESS' ? 'good' : 'danger')
}