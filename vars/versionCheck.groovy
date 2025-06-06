def call (Map config){

    def GITLAB_REPORTS_PROJECT_ID = config.GITLAB_REPORTS_PROJECT_ID

    def tag = sh(
            script: 'git describe --exact-match --tags $(git rev-parse HEAD) || echo ""', 
            returnStdout: true
    ).trim()

    if (tag) {
        echo "The current commit has a tag: ${tag}"
        if (tag ==~ /^\d+\.\d+\.\d+-release$/) {
            echo "Tag matches the format x.y.z-release."
            APPLICATION_VERSION = tag.split("-")[0]
        } else { 
            echo "Tag does NOT match the format x.y.z-release."
            APPLICATION_VERSION = 'dev'
        }
    } else {
        echo "There is no tag."
        APPLICATION_VERSION = 'dev'
    }

    if (APPLICATION_VERSION != "dev") {
        echo "Controllo se la versione ${APPLICATION_VERSION} è presente nei nomi delle cartelle."
        // Chiamata API GitLab per ottenere la lista dei file e delle cartelle
        def apiUrl = "https://gitlab-bper.gbbper.priv/api/v4/projects/${GITLAB_REPORTS_PROJECT_ID}/repository/tree?path=${env.gitlabGroup}/${env.gitlabPproject}/&ref=master"

        // Effettua la chiamata all'API per verificare la presenza della versione
        def response = sh(
                script: """curl -k -s -H "PRIVATE-TOKEN: $GITLAB_TOKEN" "${apiUrl}" """,
                returnStdout: true
        ).trim()


        try {
            def jsonResponse = new groovy.json.JsonSlurper().parseText(response)
            def versionExists = jsonResponse.any { it.type == "tree" && it.name == APPLICATION_VERSION }
            if (versionExists) {
                error "La versione ${APPLICATION_VERSION} è stata già buildata in precedenza. E' necessario aggiornarla."
            } else {
                echo "Versione '${APPLICATION_VERSION}."
            }
        } catch (Exception e) {
            echo "Errore durante l'analisi della risposta JSON: ${e.message}"
            error("Interrotto a causa di un errore nell'elaborazione della risposta JSON.")
        }

        return APPLICATION_VERSION
    }
}