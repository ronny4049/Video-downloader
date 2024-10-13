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

##Install promitheus
```
sudo useradd --system --no-create-home --shell /bin/false prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.54.1/prometheus-2.54.1.linux-amd64.tar.gz
```
## extract and move the file
```
tar -xvf prometheus-2.47.1.linux-amd64.tar.gz
cd prometheus-2.47.1.linux-amd64/
sudo mkdir -p /data /etc/prometheus
sudo mv prometheus promtool /usr/local/bin/
sudo mv consoles/ console_libraries/ /etc/prometheus/
sudo mv prometheus.yml /etc/prometheus/prometheus.yml
```

## Change the ownership
``` sudo chown -R prometheus:prometheus /etc/prometheus/ /data/ ```
## edit the file 
``` /etc/systemd/system/prometheus.service ```
``` sudo nano /etc/systemd/system/prometheus.service ```
### add below
```
Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

StartLimitIntervalSec=500
StartLimitBurst=5

[Service]
User=prometheus
Group=prometheus
Type=simple
Restart=on-failure
RestartSec=5s
ExecStart=/usr/local/bin/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/data \
[Install]
WantedBy=multi-user.target
  ```
## enable and Check the status 
```
sudo systemctl enable prometheus
sudo systemctl start prometheus
sudo systemctl statuus prometheus
```
### Add the below in the same file
```
--web.console.templates=/etc/prometheus/consoles \
  --web.console.libraries=/etc/prometheus/console_libraries \
  --web.listen-address=0.0.0.0:9090 \
  --web.enable-lifecycle
```
## Installing Node Exporter:

   Create a system user for Node Exporter and download Node Exporter:

   ```bash
   sudo useradd --system --no-create-home --shell /bin/false node_exporter
   wget https://github.com/prometheus/node_exporter/releases/download/v1.6.1/node_exporter-1.6.1.linux-amd64.tar.gz
   ```

   Extract Node Exporter files, move the binary, and clean up:

   ```bash
   tar -xvf node_exporter-1.6.1.linux-amd64.tar.gz
   sudo mv node_exporter-1.6.1.linux-amd64/node_exporter /usr/local/bin/
   rm -rf node_exporter*
   ```
Create a systemd unit configuration file for Node Exporter:

   ```bash
   sudo nano /etc/systemd/system/node_exporter.service
   ```

   Add the following content to the `node_exporter.service` file:

   ```plaintext
   [Unit]
   Description=Node Exporter
   Wants=network-online.target
   After=network-online.target

   StartLimitIntervalSec=500
   StartLimitBurst=5

   [Service]
   User=node_exporter
   Group=node_exporter
  Type=simple
   Restart=on-failure
   RestartSec=5s
   ExecStart=/usr/local/bin/node_exporter --collector.logind

   [Install]
   WantedBy=multi-user.target
   ```

   Replace `--collector.logind` with any additional flags as needed.

   Enable and start Node Exporter:

   ```bash
   sudo systemctl enable node_exporter
   sudo systemctl start node_exporter
   ```

   Verify the Node Exporter's status:

   ```bash
   sudo systemctl status node_exporter
   ```

   You can access Node Exporter metrics in Prometheus.

2. **Configure Prometheus Plugin Integration:**

   Integrate Jenkins with Prometheus to monitor the CI/CD pipeline.

   **Prometheus Configuration:**

   To configure Prometheus to scrape metrics from Node Exporter and Jenkins, you need to modify the `prometheus.yml` file. Here is an example `prometheus.yml` configuration for your setup:

   ```yaml
   global:
     scrape_interval: 15s

   scrape_configs:
     - job_name: 'node_exporter'
       static_configs:
          - targets: ['localhost:9100']

     - job_name: 'jenkins'
       metrics_path: '/prometheus'
       static_configs:
         - targets: ['<your-jenkins-ip>:<your-jenkins-port>']
   ```

   Make sure to replace `<your-jenkins-ip>` and `<your-jenkins-port>` with the appropriate values for your Jenkins setup.

   Check the validity of the configuration file:

   ```bash
   promtool check config /etc/prometheus/prometheus.yml
   ```

   Reload the Prometheus configuration without restarting:
### Install grafana
``` docker run --name grafana-test -d -p 3000:3000 grafana/grafana-oss:latest ```
### Configure the grafana with promitheus
```
And check in prometheus UI in Status/Targets 
After that login to grafana and to connection/add new coonection 
Search for prometheus and give the url and save it 
Now go to dashboard and select new and give jenkins code 1860 and load and select the prometheus and save it : 9964 
Node exprorter : 1860
```
### Now create Eks cluster in aws
```
--> Install kubectl, aws cli and eksctl 
---> Configure AWS 
---> Create EKS cluster using below command
```
### Create cluster
```
eksctl create cluster --name=ran-demo --region=us-east-1 --zones=us-east-1a,us-east-1b --without-nodegroup
```
### Create OIDC
```
eksctl utils associate-iam-oidc-provider --region us-east-1 --cluster ran-demo --approve
```
### Create node for the cluster
```
eksctl create nodegroup --cluster=ran-demo \
   --region=us-east-1 \
   --name=ran-demo-ng-public1 \
   --node-type=t3.medium \
   --nodes=2 \
   --nodes-min=2 \
   --nodes-max=4 \
   --node-volume-size=20 \
   --ssh-access \
   --ssh-public-key=My-key \
   --managed \
   --asg-access \
   --external-dns-access \
   --full-ecr-access \
   --appmesh-access \
   --alb-ingress-access

```
### Install Argocd
```
kubectl create namespace argocd kubectl  

apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```
### For exposing to the out side use below command 
``` kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}} ```
#### wait for 2 min and run below command
```
export ARGOCD_SERVER=`kubectl get svc argocd-server -n argocd -o json | jq --raw-output '.status.loadBalancer.ingress[0].hostname'
```
#### Get the external ip using
``` kubectl get svc –n argocd ```
### For access the argocd application 
``` copy the ec2 ip with port, e.g: https://18.208.216.37:31529/ ```
#### For initial password use below command
``` kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d ```

```
--> Login to the argocd and click on the new app 

--> Create a new app after giving all the details like git hub and all 

--> Then argo will take the code from git hub and it will deploy automatically all the pod and everyting
```
###  Install Node Exporter using Helm
To begin monitoring your Kubernetes cluster, you'll install the Prometheus Node Exporter. This component allows you to collect system-level metrics from your cluster nodes. Here are the steps to install the Node Exporter using Helm:

1. Add the Prometheus Community Helm repository:

    ```bash
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    ```

2. Create a Kubernetes namespace for the Node Exporter:

    ```bash
    kubectl create namespace prometheus-node-exporter
    ```

3. Install the Node Exporter using Helm:

    ```bash
    helm install prometheus-node-exporter prometheus-community/prometheus-node-exporter --namespace prometheus-node-exporter
    ```

Add a Job to Scrape Metrics on nodeip:9001/metrics in prometheus.yml:

Update your Prometheus configuration (prometheus.yml) to add a new job for scraping metrics from nodeip:9001/metrics. You can do this by adding the following configuration to your prometheus.yml file:


```
  - job_name: 'k8s'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['node1Ip:9100']
```

Replace 'your-job-name' with a descriptive name for your job. The static_configs section specifies the targets to scrape metrics from, and in this case, it's set to nodeip:9001.

Don't forget to reload or restart Prometheus to apply these changes to your configuration.

To deploy an application with ArgoCD, you can follow these steps, which I'll outline in Markdown format:

### Deploy Application with ArgoCD

1. **Install ArgoCD:**

   You can install ArgoCD on your Kubernetes cluster by following the instructions provided in the [EKS Workshop](https://archive.eksworkshop.com/intermediate/290_argocd/install/) documentation.

2. **Set Your GitHub Repository as a Source:**

   After installing ArgoCD, you need to set up your GitHub repository as a source for your application deployment. This typically involves configuring the connection to your repository and defining the source for your ArgoCD application. The specific steps will depend on your setup and requirements.

3. **Create an ArgoCD Application:**
   - `name`: Set the name for your application.
   - `destination`: Define the destination where your application should be deployed.
   - `project`: Specify the project the application belongs to.
   - `source`: Set the source of your application, including the GitHub repository URL, revision, and the path to the application within the repository.
   - `syncPolicy`: Configure the sync policy, including automatic syncing, pruning, and self-healing.

4. **Access your Application**
   - To Access the app make sure port 30007 is open in your security group and then open a new tab paste your NodeIP:30007, your app should be running.

**Phase 7: Cleanup**

1. **Cleanup AWS EC2 Instances:**
    - Terminate AWS EC2 instances that are no longer needed.


---> We can access the application using aws cluster ip address with mentioned port in service file
```



