kubectl apply -f cassandra-service.yaml
# get service
kubectl get svc cassandra

kubectl apply -f cassandra-statefulset.yaml
# get nodes
kubectl get statefulset cassandra
kubectl get pods -l="app=cassandra"
kubectl exec -it cassandra-0 -- nodetool status

# deploy load balancer
# on mac to make the load balancer expose external ip
minikube tunnel
kubectl apply -f loadBalancer.yaml

# scale down replicas to one:
kubectl edit statefulset cassandra
# set replicas: 1
# verify number of replicas
kubectl get statefulset cassandra