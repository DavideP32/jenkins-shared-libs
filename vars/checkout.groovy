def call(Map config){
    def checkout_url = config.checkout_url

    checkout([
        $class                           : 'GitSCM',
        branches                         : [[name: "master"]],
        doGenerateSubmoduleConfigurations: false,
        extensions                       : [],
        submoduleCfg                     : [],
        userRemoteConfigs                : [
                [
                        credentialsId: '0ce2d803-fcc5-4faf-b45f-5fca9f59ffd8',
                        url          : checkout_url
                ]
        ]
    ])
}