pipeline {
    agent node

    parameters {
        choice(name:'buildMoudle',choices:['web'],description:'请选择要构建的子模块')

        choice(name:'profile',choices:['test','production'],description:'请选择要构建的环境配置')

        choice(name:'toServer',choices:['10.3.45.201'],description:'请选择要目标服务器')

        choice(name:'operateType',choices:['deploy','start','stop','restart'],description:'请选择要执行的操作类型')
    }

    options {
        skipDefaultCheckout()
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('print-choice') {
            steps {
                echo "打包模块：【${params.buildMoudle}】,环境：【${params.profile}】,目标服务器：【${params.toServer}】,操作类型：【${params.operateType}】"
            }
        }

        stage('checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('build') {
            when {
                equals expected: 'deploy', actual: params.operateType
            }
            steps {
                script {
                    withMaven(gradle: 'gradle-2.8',jdk: 'jdk') {
                        echo 'building...'
                        sh "gradlew build -xTest"
                        echo 'building finished.'
                    }
                }
            }
        }

        stage('deploy') {
            when {
                equals expected: 'deploy', actual: params.operateType
                expression {
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            }
            parallel {
                stage('test-deploy') {
                    when {
                        equals expected: 'test', actual: params.profile
                        equals expected: '10.3.45.201', actual: params.toServer
                    }
                    steps {
                        sh "echo copy songkai-gradle-0.1.0.jar to 10.3.45.201 ..."
                        sh "scp ./build/libs/songkai-gradle-0.1.0.jar  pms2@10.3.45.201:/JavaWeb/pms2.wltest.com/songkai-gradle-0.1.0.jar"
                        sh "chmod +x ./deploy.sh"
                        sh "ssh pms2@10.3.45.201 bash -s < ./deploy.sh deploy ${params.buildMoudle} ${params.profile} /JavaWeb/pms2.wltest.com"
                    }
                }
            }

        }

        stage('other') {
            when {
                expression {
                    params.operateType ==~ /(start|stop|restart)/
                }
            }
            parallel {
                stage('test-other') {
                    when {
                        equals expected: 'test', actual: params.profile
                        equals expected: '10.3.45.201', actual: params.toServer
                    }
                    steps {
                        sh "echo ${params.operateType} project on 10.3.45.201 ..."
                        sh "chmod +x ./deploy.sh"
                        sh "ssh pms2@10.3.45.201 bash -s < ./deploy.sh ${params.operateType} ${params.buildMoudle} ${params.profile} /JavaWeb/pms2.wltest.com"
                    }
                }
            }
        }

    }
    post {
        always {
            script {
                wrap([$class: 'BuildUser']) {
                    emailext (
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                        subject: "【Jenkins-" + '${BUILD_STATUS}' + "】${params.buildMoudle}[${params.profile}] ${params.operateType} on ${params.toServer}",
                        mimeType: 'text/html',
                        body: "操作人ID：【${BUILD_USER_ID}】 ,操作人姓名：【${BUILD_USER}】" + '<br/>${FILE,path="email.html"}',
                        attachLog: true,
                        from: 'rfd@rufengda.com',
                        to: 'songkai@rufengdao.com'
                    )
                }
            }
            deleteDir()
        }
    }
}