def call(Map config){
    def QA_RESULT = config.QA_RESULT
    def APPLICATION_VERSION = config.APPLICATION_VERSION
    def GITLAB_REPORTS_PROJECT_ID = config.GITLAB_REPORTS_PROJECT_ID

    
    if (QA_RESULT.get("sonarqube.esito") == "KO"){
        QA_RESULT.put("quality-assurance.esito", "KO")
    } else QA_RESULT.put("quality-assurance.esito", "OK")



    def metadataContent = null

    if (!APPLICATION_VERSION.equals("dev")) {

        def metadataFile = readFile "tmp/metadata.xml"
        
        // Crea e serializza immediatamente senza salvare l'oggetto XML
        writeFile file: "tmp/metadataFile.xml", text: groovy.xml.XmlUtil.serialize(
            new XmlSlurper(false, false).parseText(metadataFile).with { xml ->
                xml.'quality-assurance'.esito = QA_RESULT.get("quality-assurance.esito")
                xml.'quality-assurance'.sonarqube.esito = QA_RESULT.get("sonarqube.esito")
                xml.'quality-assurance'.sonarqube.'report-url' = QA_RESULT.get("sonarqube.report-url")
                xml.'quality-assurance'.sonarqube.description = QA_RESULT.get("sonarqube.description")
                return xml
            }
        )
        
        metadataContent = readFile("tmp/metadataFile.xml")
        
    }    


    def nameFile = env.gitlabGroup + "/" + env.gitlabPproject + "/" + APPLICATION_VERSION + "/QG" + QA_RESULT.get("quality-assurance.esito") + "_report"
    
    def payload = """
    {
        "branch": "master",
        "commit_message": "Adding file ${nameFile}.xml",
        "actions": [
        {
            "action": "create",
            "file_path": "${nameFile}.xml",
            "content": ${groovy.json.JsonOutput.toJson(metadataContent)}
        }
        ]
    }
    """


    sh """
        curl -k --request POST --header "PRIVATE-TOKEN: $GITLAB_TOKEN" --header "Content-Type: application/json" \
        --data '${payload}' "https://gitlab.com/api/v4/projects/${GITLAB_REPORTS_PROJECT_ID}/repository/commits"
    """
    // sh """
    //     curl -k --request POST --header "PRIVATE-TOKEN: $GITLAB_TOKEN" --header "Content-Type: application/json" \
    //     --data '${payload}' "https://gitlab-bper.gbbper.priv/api/v4/projects/${GITLAB_REPORTS_PROJECT_ID}/repository/commits"
    // """


    
}