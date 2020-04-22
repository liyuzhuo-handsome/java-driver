kubectl create -f cass-operator-manifests.yml
kubectl create -f storage.yml
kubectl -n cass-operator create -f example-cassdc-minimal.yml
kubectl apply -f cass-operator-manifests.yml

# get operator
kubectl -n cass-operator get pods --selector name=cass-operator

# create storage
kubectl apply -f storage.yml

# create datacenter
kubectl -n cass-operator apply -f example-cassdc-minimal.yml

#get nodes
kubectl -n cass-operator get pods --selector cassandra.datastax.com/cluster=cluster1

# wait for ready
kubectl -n cass-operator get cassdc/dc1 -o "jsonpath={.status.cassandraOperatorProgress}"

# get token ring
kubectl -n cass-operator exec -it -c cassandra cluster1-dc1-default-sts-0 -- nodetool status


# get pod status
kubectl -n cass-operator describe pod cluster1-dc1-default-sts-0
