# Spring Boot + NFS on kind Kubernetes

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot![Kubernetes](https://img.shields.io/badge/Kubernetes-1![kind](https://img.shields.io/badge/kind-local%20Complete demo showing a Spring Boot application writing files to an NFS-backed PersistentVolume on a **kind** Kubernetes cluster.

## Features

- ✅ Spring Boot REST API to write/read files
- ✅ NFS PersistentVolume (RWX mode)
- ✅ kind cluster setup
- ✅ Docker containerization
- ✅ Production-ready YAML manifests

## Prerequisites

```bash
# Required tools
- Docker
- kind (local Kubernetes)
- kubectl
- JDK 17+
- Maven (or ./mvnw wrapper)
- NFS server on host (Linux/macOS)
```

## 🚀 Quick Start

### 1. Clone & Build

```bash
git clone https://github.com/tushardashpute/springboot-nfs-kind-demo.git
cd springboot-nfs-kind-demo

# Build Spring Boot app
cd nfs-demo
./mvnw clean package
cd ..
```

### 2. Build Docker Image

```bash
docker build -t tushardashpute/nfs-demo:latest .
# docker push tushardashpute/nfs-demo:latest  # if using external registry
```

Update `k8s/deployment.yaml` with your image tag.

### 3. Setup NFS Server (Host Machine)

```bash
# Create NFS export directory
sudo mkdir -p /srv/nfs/kind-data
sudo chown nobody:nogroup /srv/nfs/kind-data
sudo chmod 777 /srv/nfs/kind-data

# Configure exports
echo "/srv/nfs/kind-data *(rw,sync,no_subtree_check,no_root_squash)" | sudo tee -a /etc/exports

# Apply & restart NFS
sudo exportfs -ra
sudo systemctl restart nfs-server
```

**Get your host IP** (for kind nodes to reach NFS):
```bash
HOST_IP=$(ip route get 8.8.8.8 | awk '{print $7; exit}')
echo "NFS Server IP: $HOST_IP"
```

Update `k8s/nfs-pv-pvc.yaml` → `spec.nfs.server: $HOST_IP`

### 4. Create kind Cluster & Deploy

```bash
# Create kind cluster
kind create cluster --name nfs-demo

# Deploy NFS storage & app
kubectl apply -f k8s/nfs-pv-pvc.yaml
kubectl apply -f k8s/deployment.yaml

# Check status
kubectl get pods,pv,pvc
```

### 5. Test End-to-End

```bash
# Port-forward
kubectl port-forward svc/nfs-demo 8080:80 &

# Write file via REST API
curl -X POST "http://localhost:8080/files?name=demo.txt&content=Hello%20NFS%20Kubernetes!"

# Read file back
curl "http://localhost:8080/files/demo.txt"
```

**Verify on host** (proof NFS works):
```bash
cat /srv/nfs/kind-data/demo.txt
# Output: "Hello NFS Kubernetes!\n"
```
<img width="1292" height="355" alt="image" src="https://github.com/user-attachments/assets/c6d72820-3f47-4d8a-971c-79daedefdf48" />
<img width="1292" height="355" alt="image" src="https://github.com/user-attachments/assets/9d6c4d45-a986-46d6-a837-941552615de4" />
<img width="1292" height="355" alt="image" src="https://github.com/user-attachments/assets/43cb2e8f-dcc3-40ff-b803-6474f1cc8f6d" />

## 🏗️ Architecture

```
Spring Boot Pod ←── /data → NFS PVC → PV → Host NFS (/srv/nfs/kind-data)
     ↓
   REST API ←── curl → Service → Deployment
```

## 📁 File Structure

```
springboot-nfs-kind-demo/
├── nfs-demo/                    # Spring Boot app
│   ├── pom.xml
│   └── src/main/java/...        # FileController.java
├── k8s/
│   ├── nfs-pv-pvc.yaml         # NFS PersistentVolume
│   └── deployment.yaml         # Deployment + Service
├── Dockerfile                   # Containerize app
└── README.md
```

## 🔧 Customization

### Change NFS Settings
Edit `k8s/nfs-pv-pvc.yaml`:
```yaml
nfs:
  server: YOUR_HOST_IP      # Host running NFS server
  path: /srv/nfs/kind-data  # NFS export path
```

### Scale Deployment
```yaml
spec:
  replicas: 3  # Multiple pods share same NFS (RWX)
```

### Use External Registry
```bash
# deployment.yaml
image: your-registry/nfs-demo:v1.0
```

## 🧹 Cleanup

```bash
# Delete resources
kubectl delete -f k8s/
kind delete cluster --name nfs-demo

# NFS (optional)
sudo umount /srv/nfs/kind-data  # if mounted
```

## 🎯 API Endpoints

| Method | Endpoint | Description | Example |
|--------|----------|-------------|---------|
| `POST` | `/files` | Write file | `curl -X POST "http://localhost:8080/files?name=test.txt&content=hello"` |
| `GET`  | `/files/{name}` | Read file | `curl "http://localhost:8080/files/test.txt"` |

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `NFS mount failed` | Check `HOST_IP` reachable from kind nodes: `kubectl run -it --rm test --image=busybox --restart=Never --rm nslookup $HOST_IP` |
| `Permission denied` | `sudo chmod 777 /srv/nfs/kind-data` |
| `Pod Pending` | `kubectl describe pvc nfs-pvc` |
| `ImagePullBackOff` | Use `imagePullPolicy: IfNotPresent` or push to registry |

## 📄 License

MIT License - see [LICENSE](LICENSE) © 2025 Tushar Dashpute

***

**⭐ Star this repo if it helped!**  
**🐛 Issues/PRs welcome** for kind + NFS improvements.

***

*Built for DevOps engineers learning Kubernetes storage patterns*[1][2]

[1](https://spring.io/guides/gs/spring-boot-kubernetes)
[2](https://github.com/dimMaryanto93/k8s-nfs-springboot-upload)
