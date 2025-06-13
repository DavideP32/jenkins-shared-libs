def call(Map config){
    CHECKOUT_WD = pwd()

    sh "cd $CHECKOUT_WD"// && sudo rm -rf -- ..?* .[!.]* *"
    echo "$CHECKOUT_WD"
    def checkoutUrl = config.checkoutUrl

    checkout([
        $class                           : 'GitSCM',
        branches                         : [[name: "master"]],
        doGenerateSubmoduleConfigurations: false,
        extensions                       : [],
        submoduleCfg                     : [],
        userRemoteConfigs                : [
                [
                        credentialsId: '0ce2d803-fcc5-4faf-b45f-5fca9f59ffd8',
                        url          : checkoutUrl
                ]
        ]
    ])
}