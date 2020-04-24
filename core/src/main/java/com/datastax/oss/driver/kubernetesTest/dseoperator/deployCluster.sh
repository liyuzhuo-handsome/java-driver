kubectl create -f cass-operator-manifests.yml
kubectl create -f storage.yml
kubectl -n cass-operator create -f example-cassdc-minimal-two-nodes.yml
kubectl apply -f cass-operator-manifests.yml

# get operator
kubectl -n cass-operator get pods --selector name=cass-operator

# create storage
kubectl apply -f storage.yml

# create datacenter
kubectl -n cass-operator apply -f example-cassdc-minimal-two-nodes.yml

#get nodes
kubectl -n cass-operator get pods --selector cassandra.datastax.com/cluster=cluster1

# wait for ready
kubectl -n cass-operator get cassdc/dc1 -o "jsonpath={.status.cassandraOperatorProgress}"

# get token ring
kubectl -n cass-operator exec -it -c cassandra cluster1-dc1-default-sts-0 -- nodetool status


# get pod status
kubectl -n cass-operator describe pod cluster1-dc1-default-sts-0

# create pod that has a connection to DSE cluster
kubectl run curl --image=radial/busyboxplus:curl -i --tty
# on the node do:
nslookup cluster1-dc1-service.cass-operator

# validate that service is exposed using kubectl internal dns
#Server:    10.96.0.10
#Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local
#
#Name:      cluster1-dc1-service.cass-operator
#Address 1: 172.17.0.6 172-17-0-6.cluster1-dc1-all-pods-service.cass-operator.svc.cluster.local
#Address 2: 172.17.0.5 cluster1-dc1-default-sts-1.cluster1-dc1-service.cass-operator.svc.cluster.local

# use java driver image
# see https://github.com/riptano/java-driver-docker-application/blob/master/buildDockerApp.sh


# get username and password
kubectl -n cass-operator get secret cluster1-superuser
kubectl -n cass-operator get secret cluster1-superuser -o yaml

# decode both username and password using
echo base64_value | base64 -D

# scale down DSE instances to 1 instance
kubectl -n cass-operator apply -f example-cassdc-minimal-one-node.yml

# -----------------–-----------------–-----------------–-----------------–-----------------–-----------------–----------
# start one node cluster and scale up to 2 nodes
kubectl create -f cass-operator-manifests.yml
kubectl create -f storage.yml
kubectl -n cass-operator create -f example-cassdc-minimal-one-node.yml
kubectl apply -f cass-operator-manifests.yml

# get operator
kubectl -n cass-operator get pods --selector name=cass-operator

# create storage
kubectl apply -f storage.yml

# create datacenter
kubectl -n cass-operator apply -f example-cassdc-minimal-one-node.yml

#get nodes
kubectl -n cass-operator get pods --selector cassandra.datastax.com/cluster=cluster1

# start driver and connect to the nodes


# scale up to two nodes
kubectl -n cass-operator apply -f example-cassdc-minimal-two-nodes.yml

