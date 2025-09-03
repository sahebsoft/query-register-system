# Oracle XE 11g on Kubernetes

## Quick Start

Deploy Oracle XE 11g with HR schema:
```bash
kubectl apply -f oracle-xe-complete.yaml
```

## Connection Details

After deployment, connect using:
- **Host**: Your Kubernetes node IP
- **Port**: 31521 (NodePort)
- **SID**: XE
- **HR Schema**: 
  - Username: `hr`
  - Password: `hr`
- **System Admin**:
  - Username: `system`
  - Password: `oracle`

## SQL*Plus Connection Examples

From outside the cluster:
```bash
sqlplus hr/hr@//NODE_IP:31521/XE
```

From within pod:
```bash
kubectl exec -it deployment/oracle-xe-11g -- bash
sqlplus hr/hr@localhost:1521/XE
```

## Check Deployment Status

```bash
# Check if pod is running
kubectl get pods -l app=oracle-xe

# View logs
kubectl logs deployment/oracle-xe-11g

# Check service
kubectl get service oracle-xe-service
```

## Cleanup

```bash
kubectl delete -f oracle-xe-complete.yaml
```

## Notes
- Initial startup takes 3-5 minutes
- HR schema is automatically unlocked with password 'hr'
- Data persists in PVC even after pod restarts
- APEX is available on port 31080