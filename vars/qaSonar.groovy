import groovy.json.JsonSlurper

def call(Map config){
    def QA_RESULT = [:]
    def SONAR_URL = env.SONAR_HOST_URL //"http://172.16.63.242:9000"
    def scannerHome = tool 'sonarQubeScanner4'
    def QA_WD = pwd()
    def gitlabUrl = config.gitlabUrl
    def gitlabPproject = config.gitlabPproject
    def gitlabGroup = config.gitlabGroup
    def branch = config.gitlabBranch.replace('/', '-').replace('*', '')
    def javaHome = tool 'JDK_21.0.3'
    def APPLICATION_VERSION = config.APPLICATION_VERSION


    def defaultExclusions = [
        '**/node_modules/**',
        //'**/target/**',
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


    withSonarQubeEnv('SonarQubeNumera') {
        script {
            // def QA_WD = pwd()
            // def SONAR_URL = env.SONAR_HOST_URL
            // def scannerHome = tool 'sonarQubeScanner4'
            // def javaHome = tool 'JDK_21.0.3'
            // def branch = env.gitlabBranch
            // branch = branch.replace('/', '-')
            // branch = branch.replace('*', '')
            if (env.SONAR_IS_UP == 'true') {
                
                sh """
                    cd //tmp && \\
                    JAVA_HOME=${javaHome} \\
                    '${scannerHome}/bin/sonar-scanner' \\
                    -Dsonar.projectBaseDir=${QA_WD} \\
                    -Dsonar.projectKey=${gitlabPproject}-${branch} \\
                    -Dsonar.projectName=${gitlabPproject}-${branch} \\
                    -Dsonar.projectVersion='${APPLICATION_VERSION}' \\
                    -Dsonar.sources=. \\
                    -Dsonar.exclusions='${defaultExclusions}' \\
                    -Dsonar.host.url='${SONAR_URL}' \\
                    -Dsonar.java.binaries=.
                """
            }
        }
    }

    sleep(30) //....
    timeout(time: 360, unit: 'SECONDS') {
        if (env.SONAR_IS_UP == 'true') {
            def qg = waitForQualityGate() 
            // def branch = env.gitlabBranch
            // branch = branch.replace('/', '-')
            // branch = branch.replace('*', '')
            
            withCredentials([usernameColonPassword(credentialsId: 'sonarqube-api-token', variable: 'USERPASS')]) {
                //sh "curl -X POST -u $USERPASS -i '" + SONAR_URL + "/api/permissions/apply_template?templateName=Template-Sonarqube_" + gitlabGroup + "&projectKey=" + gitlabPproject + "-" + branch + "'"
                sh "curl -X POST -u $USERPASS -i '" + SONAR_URL + "/api/permissions/apply_template?templateName=Default%20template&projectKey=" + gitlabPproject + "-" + branch + "'"
                echo 'Configurato permission template per il project sonar'


                //estrazione coverage da sonar
                sh "curl --fail -s -L -X GET -u $USERPASS '$SONAR_URL/api/measures/component?component=${gitlabPproject}-${branch}&metricKeys=coverage,sqale_debt_ratio' -o sonar_measures.json"
                def coverageFile = readFile "sonar_measures.json"
                def coverageJson = new JsonSlurper().parseText(coverageFile)
                Float coverage = 0
                Float techDeb = 0
                Float securityRating = 0

                coverageJson.component.measures.each {
                    if (it.metric == "coverage" && it.value) coverage = it.value.toFloat()
                    if (it.metric == "sqale_debt_ratio" && it.value) techDeb = it.value.toFloat()
                    if (it.metric == "security_rating" && it.value) securityRating = it.value.toFloat()
                }

                //aggiungo l'url della dashboard sonar alla mappa
                QA_RESULT.put("sonarqube.report-url", "$SONAR_URL/dashboard?id=" + gitlabPproject + "-" + branch)
                //aggiungo l'esito dell'analisi Sonar
                //rendo sonarQUBE non bloccante, il primo KO diventa OK
                QA_RESULT.put("sonarqube.esito", env.SONAR_IS_BLOCKING != 'false' && qg.status != 'OK' && qg.status != 'WARN' ? "KO" : "OK")
                //aggiungo la descrizione per sonar
                QA_RESULT.put("sonarqube.description", "Coverage del progetto: " + coverage + "\ndebito tecnico: " + techDeb + "\nsecurity rating: " + securityRating)
            }

            if (qg.status != 'OK' && qg.status != 'WARN') {
                echo("Pipeline in errore a causa del Quality Gate impostato su Sonar. Status: ${qg.status}. Controllare la link $SONAR_URL/dashboard?id=" + gitlabPproject + "-" + branch)
                echo(QA_RESULT["sonarqube.description"])
                currentBuild.result = 'UNSTABLE'
            }
        } else {
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'

            echo 'ATTENZIONE VERIFICA QUALITY GATE SONAR NON ATTIVO Variabile ambiente env.SONAR_IS_UP ' + env.SONAR_IS_UP

            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
            echo 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'
        }

        return QA_RESULT
    }
}