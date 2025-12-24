# Spring Boot + NFS + K3s Setup

---

## NFS SERVER

- **Server:** nfs-server, x86_64, Ubuntu Version 20.04 - Focal Fossa  
- **Specs:** SMALL 2 vCPUs / 4GB / 100GB Disk  
- **SSH:** `ssh onecloud-user@9.114.114.215`

```bash
sudo -i
apt update
```

## Disk Info
```
df -hT
lsblk
```
- Disk is 100 GB
- Only 47 GB is partitioned as sda3
- Out of that 47 GB: 23.5 GB is used by Logical Volume /
- Remaining ~23.5 GB is FREE inside LVM
- ~53 GB is completely unpartitioned

## Extend sda3 to full disk
```
sudo growpart /dev/sda 3
lsblk  # Verify sda3 ~97G
sudo pvresize /dev/sda3
sudo lvextend -l +100%FREE /dev/ubuntu-vg/ubuntu-lv
sudo resize2fs /dev/ubuntu-vg/ubuntu-lv
df -h /  # Final verification

```

## Install Java
```
git clone https://github.com/vijay2181/springboot-nfs-k3s.git
cd springboot-nfs-k3s
mvn clean package

Output in target/:

classes
generated-sources
generated-test-sources
maven-archiver
maven-status
nfs-demo-0.0.1-SNAPSHOT.jar
nfs-demo-0.0.1-SNAPSHOT.jar.original
surefire-reports
test-classes

```

## Build Docker Image
```
pwd  # /root/springboot-nfs-kind
apt install docker.io
docker build -t vijay2181/springboot-nfs-demo .
docker login docker.io
docker push vijay2181/springboot-nfs-demo

```

## Setup NFS Server (Host Machine)
```
sudo apt update
sudo apt install -y nfs-kernel-server

sudo systemctl enable nfs-server
sudo systemctl start nfs-server
sudo systemctl status nfs-server

#Create NFS Export Directory
sudo mkdir -p /srv/nfs/kind-data
sudo chown nobody:nogroup /srv/nfs/kind-data
sudo chmod 777 /srv/nfs/kind-data

#Configure Exports
echo "/srv/nfs/kind-data *(rw,sync,no_subtree_check,no_root_squash)" | sudo tee -a /etc/exports
sudo exportfs -ra
sudo exportfs -v
sudo systemctl restart nfs-server

#Test Local Mount (NFS Client Test)
sudo apt install -y nfs-common
sudo mkdir -p /mnt/nfs-test
sudo mount -t nfs localhost:/srv/nfs/kind-data /mnt/nfs-test
touch /mnt/nfs-test/hello.txt
ls -l /mnt/nfs-test

```

## Get Host IP (for K3s nodes to reach NFS)
```
HOST_IP=$(ip route get 8.8.8.8 | awk '{print $7; exit}')
echo "NFS Server IP: $HOST_IP"
# Update k8s/nfs-pv-pvc.yaml → spec.nfs.server: $HOST_IP

```

# K3s Installation

## Install kubectl
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
kubectl version --client

```

## Firewall Rules
```
ufw disable
ufw allow 6443/tcp # apiserver
ufw allow from 10.42.0.0/16 to any # pods
ufw allow from 10.43.0.0/16 to any # services
```

## Install K3s
```
curl -sfL https://get.k3s.io | sh -
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
systemctl status k3s
kubectl get nodes
kubectl get all -n kube-system

# Make permanent
echo "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml" >> ~/.bashrc
source ~/.bashrc
```

## Updates
```
git clone https://github.com/vijay2181/springboot-nfs-k3s.git
cd springboot-nfs-k3s

# Update your image tag in k8s/deployment.yaml
# Update k8s/nfs-pv-pvc.yaml → spec.nfs.server: $HOST_IP
```

## Deploy
```
apt install docker.io git -y
sudo systemctl enable docker
sudo systemctl start docker
sudo systemctl status docker

# Create K3s cluster
kind create cluster --name nfs-demo

# Configure Docker secret
kubectl create secret docker-registry mydockerhub \
  --docker-username=vijay2181 \
  --docker-password=xxxxxxxx \
  --docker-server=https://index.docker.io/v1/

# Deploy NFS storage & app
kubectl apply -f k8s/nfs-pv-pvc.yaml
kubectl apply -f k8s/deployment.yaml

# Check status
kubectl get pods,pv,pvc
```

## Test End-to-End
```
# Port-forward
kubectl port-forward svc/nfs-demo 8080:80 &

# Write file via REST API
curl -X POST "http://localhost:8080/files?name=demo.txt&content=Hello%20NFS%20Kubernetes!"

# Read file back
curl "http://localhost:8080/files/demo.txt"
```

| Method | Endpoint      | Description | Example                                                                  |
| ------ | ------------- | ----------- | ------------------------------------------------------------------------ |
| POST   | /files        | Write file  | `curl -X POST "http://localhost:8080/files?name=test.txt&content=hello"` |
| GET    | /files/{name} | Read file   | `curl "http://localhost:8080/files/test.txt"`                            |


```
Spring Boot NFS app is working exactly as expected:
POST to /files writes content to the NFS volume (/data).
GET from /files/<filename> reads it back.
```

## Verify on Host (Proof NFS Works)
```
cat /srv/nfs/kind-data/demo.txt
# Output: Hello NFS Kubernetes!
```






















