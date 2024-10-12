# Video-downloader
### Install jenkins
```
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo yum upgrade
sudo yum install fontconfig java-17-openjdk
sudo yum install jenkins
sudo systemctl daemon-reload
```
### Run sonarqube in docker

docker run -d -p 9000:900 sonarqube:lts-community

### Install trivy
```
$ sudo vim /etc/yum.repos.d/trivy.repo
[trivy]
name=Trivy repository
baseurl=https://aquasecurity.github.io/trivy-repo/rpm/releases/$releasever/$basearch/
gpgcheck=0
enabled=1
$ sudo yum -y update
$ sudo yum -y install trivy
```

### Install docker scout
```
docker login       `Give Dockerhub credentials here`
curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh -s -- -b /usr/local/bin
```
### Build Stage
``` Git Checkout-->Maven Build-->Sonarqube Analysis-->Quality Gate-->OWASP FS SCAN-->Trivy File Scan-->Build Docker Image-->Tag & Push to DockerHub-->Docker Scout Image-->Deploy to Comtainer ```
### Jenkins Complete pipeline
```
pipeline {
    agent any
    tools {
        jdk 'jdk17'
        maven 'maven'
    }
    environment {
        SCANNER_HOME=tool 'sonarqube'
    }
    stages {
        stage("Clean Workspace") {
            steps {
                cleanWs()
            }
        }
        stage("Git Checkout") {
            steps {
                git branch: 'main', url: 'https://github.com/ronny4049/Video-downloader.git'
            }
        }
        stage("Maven Build") {
            steps {
                sh 'mvn clean install'
            }
        }
        stage("Sonarqube Analysis") {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                    $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=Video-downloader \
                    -Dsonar.projectKey=Video-downloader \
                    -Dsonar.java.binaries=target/classes
                    '''
                }
            }
        }
        stage("Quality Gate") {
            steps {
                script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonarqube'
                }
            }
        }
        stage('OWASP FS SCAN') {
            steps {
                dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage("Trivy File Scan") {
            steps {
                sh 'trivy fs . > trivy.txt'
            }
        }
        stage("Build Docker Image") {
            steps {
                sh 'docker build -t video-downloader .'
            }
        }
        stage("Tag & Push to DockerHub") {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker-cred') {
                        sh 'docker tag video-downloader ranjandalai4049/video-downloader:v1'
                        sh 'docker push ranjandalai4049/video-downloader:v1'
                    }
                }
            }
        }
        stage("Docker Scout Image") {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker-cred', toolName: 'docker') {
                        sh 'docker-scout quickview ranjandalai4049/video-downloader:v1'
                        sh 'docker-scout cves ranjandalai4049/video-downloader:v1'
                        sh 'docker-scout recommendations ranjandalai4049/video-downloader:v1'
                    }
                }
            }
        }
        stage("Deploy to Comtainer") {
            steps {
                sh 'docker run -d --name video-downloader -p 8080:8080 ranjandalai4049/video-downloader:v1'
            }
        }
    }
}
```

