def call(Map config = [:]) {
    def sonarServer = config.get('sonarServer', 'sq1')
    def projectKey = config.get('projectKey', '')
    def projectName = config.get('projectName', projectKey)
    def projectVersion = config.get('projectVersion', '1.0.0')

    withSonarQubeEnv(sonarServer) {
        script {
            env.SONAR_SCANNER = tool 'SonarScanner'
            
            if (!env.SONAR_SCANNER?.trim()) {
                error "SonarScanner tool path is empty. Check Jenkins tool configuration."
            }

            echo "DEBUG - SONAR_SCANNER path: ${env.SONAR_SCANNER}"
            echo "DEBUG - PROJECT_KEY: ${projectKey}"
            echo "DEBUG - PROJECT_NAME: ${projectName}"
            echo "DEBUG - PROJECT_VERSION: ${projectVersion}"

            
            def defaultExclusions = [
                '**/node_modules/**',
                '**/target/**',
                '**/build/**',
                '**/dist/**',
                '**/out/**',
                '**/bin/**',
                '**/.git/**',
                '**/.svn/**',
                '**/vendor/**',
                '**/coverage/**',
                '**/__pycache__/**',
                '**/*.pyc',
                '**/*.min.js',
                '**/logs/**',
                '**/.env',
                '**/.DS_Store',
                '**/thumbs.db'
            ].join(',')

            // type of project detection
            // def isMavenProject = fileExists('pom.xml')
            // def isGradleProject = fileExists('build.gradle') || fileExists('build.gradle.kts')
            // def hasJavaFiles = sh(
            //     script: "find . -name '*.java' -type f | head -1",
            //     returnStdout: true
            // ).trim()

            // echo "INFO - Project Detection:"
            // echo "INFO - Maven: ${isMavenProject}"
            // echo "INFO - Gradle: ${isGradleProject}"
            // echo "INFO - Java files: ${hasJavaFiles ? 'Si' : 'No'}"

            // if (isMavenProject) {
            //     echo "INFO - Progetto Maven detected, compile before analysis"
                
            //     // Compile
            //     sh "mvn clean compile -DskipTests=true"
                
            //     // find binaries
            //     def targetDirs = sh(
            //         script: "find . -name target -type d -exec find {} -name '*.class' -type f \\; | head -1 | xargs dirname 2>/dev/null || echo ''",
            //         returnStdout: true
            //     ).trim()
                
            //     def binaryPaths = targetDirs ? targetDirs : "./target/classes"
                
            //     sh """
            //         '${env.SONAR_SCANNER}/bin/sonar-scanner' \\
            //         -Dsonar.projectKey='${projectKey}' \\
            //         -Dsonar.projectName='${projectName}' \\
            //         -Dsonar.projectVersion='${projectVersion}' \\
            //         -Dsonar.sources=src/main \\
            //         -Dsonar.tests=src/test \\
            //         -Dsonar.java.binaries='${binaryPaths}' \\
            //         -Dsonar.exclusions='${defaultExclusions}' \\
            //         -Dsonar.host.url='${env.SONAR_HOST_URL}' \\
            //         -Dsonar.login='${env.SONAR_AUTH_TOKEN}' \\
            //         -Dsonar.scm.provider=git \\
            //         -Dsonar.sourceEncoding=UTF-8
            //     """
                
            // } else if (isGradleProject) {
            //     echo "INFO - Gradle Project detected, compiling"
                
            //     // Compila il progetto Gradle
            //     sh "./gradlew compileJava -x test || gradle compileJava -x test"
                
            //     // Trova i binari compilati
            //     def buildDirs = sh(
            //         script: "find . -path '*/build/classes' -type d | head -1 || echo './build/classes/java/main'",
            //         returnStdout: true
            //     ).trim()
                
            //     sh """
            //         '${env.SONAR_SCANNER}/bin/sonar-scanner' \\
            //         -Dsonar.projectKey='${projectKey}' \\
            //         -Dsonar.projectName='${projectName}' \\
            //         -Dsonar.projectVersion='${projectVersion}' \\
            //         -Dsonar.sources=src/main \\
            //         -Dsonar.tests=src/test \\
            //         -Dsonar.java.binaries='${buildDirs}' \\
            //         -Dsonar.exclusions='${defaultExclusions}' \\
            //         -Dsonar.host.url='${env.SONAR_HOST_URL}' \\
            //         -Dsonar.login='${env.SONAR_AUTH_TOKEN}' \\
            //         -Dsonar.scm.provider=git \\
            //         -Dsonar.sourceEncoding=UTF-8
            //     """
                
            // } else if (hasJavaFiles) {
            //     echo "INFO - File Java rilevati senza build tool, escludo dall'analisi"
                
            //     sh """
            //         '${env.SONAR_SCANNER}/bin/sonar-scanner' \\
            //         -Dsonar.projectKey='${projectKey}' \\
            //         -Dsonar.projectName='${projectName}' \\
            //         -Dsonar.projectVersion='${projectVersion}' \\
            //         -Dsonar.sources=. \\
            //         -Dsonar.java.binaries=. \\
            //         -Dsonar.exclusions='${defaultExclusions}' \\
            //         -Dsonar.host.url='${env.SONAR_HOST_URL}' \\
            //         -Dsonar.login='${env.SONAR_AUTH_TOKEN}' \\
            //         -Dsonar.scm.provider=git \\
            //         -Dsonar.sourceEncoding=UTF-8
            //     """
                
            // } else {
                echo "INFO - Generic analysis"
                
                
                sh """
                    '${env.SONAR_SCANNER}/bin/sonar-scanner' \\
                    -Dsonar.projectKey='${projectKey}' \\
                    -Dsonar.projectName='${projectName}' \\
                    -Dsonar.projectVersion='${projectVersion}' \\
                    -Dsonar.sources=. \\
                    -Dsonar.exclusions='${defaultExclusions}' \\
                    -Dsonar.host.url='${env.SONAR_HOST_URL}' \\
                    -Dsonar.login='${env.SONAR_AUTH_TOKEN}' \\ 
                    -Dsonar.scm.provider=git \\
                    -Dsonar.sourceEncoding=UTF-8
                """

                //sonar.login deprecato, si usa sonar.token
            }
        }
    }
}
// def call(Map config = [:]) {
//     def sonarServer = config.get('sonarServer', 'sq1')
//     def projectKey = config.get('projectKey', '')

//     withSonarQubeEnv(sonarServer) {
//         script {
//             env.SONAR_SCANNER = tool 'SonarScanner'
            
//             if (!env.SONAR_SCANNER?.trim()) {
//                 error "SonarScanner tool path is empty. Check Jenkins tool configuration."
//             }

//             echo "DEBUG - SONAR_SCANNER path: ${env.SONAR_SCANNER}"
//             echo "DEBUG - PROJECT_KEY: ${projectKey}"

//             // Trova pom.xml se presente
//             def pomPath = sh(
//                 script: "find . -name pom.xml | head -n 1",
//                 returnStdout: true
//             ).trim()

//             if (pomPath) {
//                 def pomDir = sh(
//                     script: "dirname '${pomPath}'",
//                     returnStdout: true
//                 ).trim()

//                 echo "Trovato pom.xml in: ${pomDir}"
//                 dir(pomDir) {
//                     sh """
//                         mvn clean verify sonar:sonar \\
//                         -Dsonar.projectKey='${projectKey}' \\
//                         -Dsonar.host.url='${env.SONAR_HOST_URL}' \\
//                         -Dsonar.login='${env.SONAR_AUTH_TOKEN}'
//                     """
//                 }
//             } else {
//                 echo "Nessun pom.xml trovato. Uso sonar-scanner CLI."
                
//                 // Use multi-line string and proper escaping
//                 sh """
//                     '${env.SONAR_SCANNER}/bin/sonar-scanner' \\
//                     -Dsonar.projectKey='${projectKey}' \\
//                     -Dsonar.sources=. \\
//                     -Dsonar.host.url='${env.SONAR_HOST_URL}' \\
//                     -Dsonar.login='${env.SONAR_AUTH_TOKEN}'
//                 """
//             }
//         }
//     }
// }

// def detectProjectTypes() {
//     def projectTypes = []
    
//     // Java detection
//     if (fileExists('pom.xml') || fileExists('build.gradle') || fileExists('build.gradle.kts')) {
//         projectTypes.add('java')
//         echo "Detected Java project"
//     }
    
//     // Node.js/Angular detection
//     if (fileExists('package.json')) {
//         projectTypes.add('nodejs')
//         echo "Detected Node.js project"
        
//         def packageJson = readJSON file: 'package.json'
//         if (packageJson.dependencies && (packageJson.dependencies['@angular/core'] || packageJson.devDependencies['@angular/core'])) {
//             projectTypes.add('angular')
//             echo "Detected Angular project"
//         }
//     }
    
//     // Kotlin detection
//     if (fileExists('build.gradle.kts') || sh(script: 'find . -name "*.kt" -type f | head -1', returnStdout: true).trim()) {
//         if (!projectTypes.contains('java')) {
//             projectTypes.add('kotlin')
//         }
//         echo "Detected Kotlin files"
//     }
    
//     // Swift detection
//     if (fileExists('Package.swift') || fileExists('*.xcodeproj') || fileExists('*.xcworkspace') || 
//         sh(script: 'find . -name "*.swift" -type f | head -1', returnStdout: true).trim()) {
//         projectTypes.add('swift')
//         echo "Detected Swift project"
//     }
    
//     // Python detection
//     if (fileExists('requirements.txt') || fileExists('setup.py') || fileExists('pyproject.toml') ||
//         sh(script: 'find . -name "*.py" -type f | head -1', returnStdout: true).trim()) {
//         projectTypes.add('python')
//         echo "Detected Python project"
//     }
    
//     // C# detection
//     if (fileExists('*.sln') || fileExists('*.csproj') || 
//         sh(script: 'find . -name "*.cs" -type f | head -1', returnStdout: true).trim()) {
//         projectTypes.add('csharp')
//         echo "Detected C# project"
//     }
    
//     // Go detection
//     if (fileExists('go.mod') || fileExists('go.sum') ||
//         sh(script: 'find . -name "*.go" -type f | head -1', returnStdout: true).trim()) {
//         projectTypes.add('go')
//         echo "Detected Go project"
//     }
    
//     // PHP detection
//     if (fileExists('composer.json') || 
//         sh(script: 'find . -name "*.php" -type f | head -1', returnStdout: true).trim()) {
//         projectTypes.add('php')
//         echo "Detected PHP project"
//     }
    
//     // Ruby detection
//     if (fileExists('Gemfile') || 
//         sh(script: 'find . -name "*.rb" -type f | head -1', returnStdout: true).trim()) {
//         projectTypes.add('ruby')
//         echo "Detected Ruby project"
//     }
    
//     if (projectTypes.isEmpty()) {
//         projectTypes.add('generic')
//         echo "No specific project type detected, using generic scan"
//     }
    
//     return projectTypes.join(',')
// }

// def setupBuildEnvironment(String projectTypes) {
//     def types = projectTypes.split(',')
    
//     types.each { type ->
//         switch(type) {
//             case 'java':
//                 sh '''
//                     if command -v java >/dev/null 2>&1; then
//                         echo "Java already installed: $(java -version)"
//                     else
//                         echo "Installing Java..."
//                         # Add Java installation commands here
//                     fi
//                 '''
//                 break
//             case 'nodejs':
//             case 'angular':
//                 sh '''
//                     if command -v node >/dev/null 2>&1; then
//                         echo "Node.js already installed: $(node --version)"
//                         echo "npm version: $(npm --version)"
//                     else
//                         echo "Installing Node.js..."
//                         # Add Node.js installation commands here
//                     fi
//                 '''
//                 break
//             case 'swift':
//                 sh '''
//                     if command -v swift >/dev/null 2>&1; then
//                         echo "Swift already installed: $(swift --version)"
//                     else
//                         echo "Swift not found. Please ensure Xcode or Swift toolchain is installed."
//                     fi
//                 '''
//                 break
//             case 'python':
//                 sh '''
//                     if command -v python3 >/dev/null 2>&1; then
//                         echo "Python already installed: $(python3 --version)"
//                     else
//                         echo "Installing Python..."
//                         # Add Python installation commands here
//                     fi
//                 '''
//                 break
//         }
//     }
// }

// def buildProject(String projectTypes) {
//     def types = projectTypes.split(',')
    
//     types.each { type ->
//         switch(type) {
//             case 'java':
//                 if (fileExists('pom.xml')) {
//                     sh 'mvn clean compile test-compile'
//                 } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
//                     sh './gradlew clean compileJava compileTestJava || gradle clean compileJava compileTestJava'
//                 }
//                 break
//             case 'nodejs':
//             case 'angular':
//                 sh '''
//                     npm ci || npm install
//                     if npm run | grep -q "build"; then
//                         npm run build || echo "Build script not found or failed"
//                     fi
//                 '''
//                 break
//             case 'swift':
//                 if (fileExists('Package.swift')) {
//                     sh 'swift build || echo "Swift build failed"'
//                 } else {
//                     echo "Xcode project detected, skipping command line build"
//                 }
//                 break
//             case 'python':
//                 sh '''
//                     if [ -f requirements.txt ]; then
//                         pip3 install -r requirements.txt || echo "Failed to install requirements"
//                     fi
//                     python3 -m py_compile $(find . -name "*.py") || echo "Python compilation check completed"
//                 '''
//                 break
//             case 'csharp':
//                 sh 'dotnet build || echo "C# build failed"'
//                 break
//             case 'go':
//                 sh 'go build ./... || echo "Go build failed"'
//                 break
//             case 'php':
//                 if (fileExists('composer.json')) {
//                     sh 'composer install || echo "Composer install failed"'
//                 }
//                 break
//             case 'ruby':
//                 if (fileExists('Gemfile')) {
//                     sh 'bundle install || echo "Bundle install failed"'
//                 }
//                 break
//         }
//     }
// }
