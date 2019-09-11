# Microservices Orchestration with Kubernetes

## 1. Introduction

In this session we will experiment with Kubernetes, a container-orchestration system commonly used as a way to host applications developed with a microservices architectural style. Microservices is a broad concept, here we will only focus on the abstractions and tools that Kubernetes provides. 

<p align="center">
  <img src="microservices_intro.png" width="600">
</p>

The objectives of this assignment are to:

* Obtain a basic understanding of the microservices architectural style.
* Learn the basic functionality of Kubernetes.
* Implement and deploy a simple microservices-based application.

Each group will have to:

1. Tutorial: Follow a brief tutorial about how to deploy a microservice into Kubernetes.  
2. Assignment: Complete the lab assignment consisting on developing and deploying a simple microservices-based application. 
3. Write a .pdf report describing the steps taken to complete the assignment, including screenshots of the application output.

## 2. Kubernetes tutorial

NOTE: This tutorial has been tested in macOS 10.13.6 and Ubuntu 18.04.3 but, in theory, you should be able to do it over Windows too. It's even possible to do the tutorial inside a virtual machine (e.g. VirtualBox) running Ubuntu, but in that case you should configure the VM with at least 2 cores and 3GB of memory.


### 2.1. Install a toy Kubernetes cluster with Minikube

In a production environment, Kubernetes typically runs over a private computer cluster or it is managed by a cloud provider (e.g. Google GKE, Amazon EKS, etc.). In order to be able to try Kubernetes locally, we will use Minikube, a tool that runs a single-node Kubernetes cluster within a virtual machine (VM) . 

#### Prerequisites

It's recommended (and required if you're working over Linux) to have Docker installed on your machine. You can check if it's already installed this way:

    docker -v

If not, you would need to install it. In Ubuntu you can do it this way:

    sudo apt-get update
    wget -qO- https://get.docker.com/ | sh
    sudo usermod -aG docker $(whoami)
    newgrp docker

Check that Docker is installed and that you can run it without sudo executing:

    docker run hello-world

Windows and macOS installation procedures can be found [here](https://docs.docker.com/install/).

#### Install kubectl 

On Linux:

	curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl

On MacOS:

	curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/darwin/amd64/kubectl

On both:

	chmod +x ./kubectl
	sudo mv ./kubectl /usr/local/bin/kubectl

#### Install Minikube 

On Linux:

	curl -Lo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64

On MacOS:

	curl -Lo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-darwin-amd64

On both:

	chmod +x ./minikube
  	sudo mv minikube /usr/local/bin


### 2.2. Launch a (Minikube) Kubernetes cluster

On Linux (bare-metal execution, without a VM):

	sudo minikube --vm-driver=none start

On MacOS (using the hypervisor that comes with Docker):

	brew install docker-machine-driver-hyperkit
	sudo minikube start --vm-driver=hyperkit

In order to avoid the need of using "sudo" all the time, let's do the following:

	sudo chown -R $USER $HOME/.minikube
	sudo chgrp -R $USER $HOME/.minikube
	sudo chown -R $USER $HOME/.kube
	sudo chgrp -R $USER $HOME/.kube

We can see the IP address of the Minikube VM with the following command:

	minikube ip

#### Troubleshooting

You can delete an existing minikube setup with:

	minikube stop
	minikube delete

Or, in case that fails:

	rm -rf ~/.minikube

### 2.3. Containerized Hello World microservice

#### Application or microservice?

Generally speaking, microservices are a way of breaking down an application into loosely coupled sub-applications or "services". Here, we will not address how this decomposition should be applied and we will just start from an example application (a typical Hello World web app) that we will call "microservice", assuming that it could be part of a bigger application. 


#### Hello World microservice in Node.js

Kubernetes requires microservices to be containerized. A microservice may consist on serveral containers plus other resources (storage, networking), but here we will use just a single container. Let's start by creating a containerized Hello World microservice. 

In an empty directory called "src", edit a file named "server.js" with the following contents:
```
var http = require('http');

var handleRequest = function(request, response) {
  console.log('Received request for URL: ' + request.url);
  response.writeHead(200);
  response.end('Hello World!');
};
var www = http.createServer(handleRequest);
www.listen(8080);
```

Within the "src" directory edit a file named "Dockerfile" with the following contents:
```
vi Dockerfile

FROM node:6.14.2
EXPOSE 8080
COPY server.js .
CMD node server.js
```
#### Making Docker images accessible to Minikube

In a real scenario you would build your images locally and upload them to Docker Hub or any other registry. In that case, you will have an image's URL to pass to Kubernetes. However, to accelerate things a little bit, here we will skip the usage of a Docker regisry and we will tell Kubernetes to get the images from our local Docker registry.

On Ubuntu, as we used vm-driver=none, we don't need to do anything, as our single-node Kubernetes cluster it's running directly on the host machine. Your local Docker registry will be directly accessible by the Kubernetes commands.

On macOS, Minikube uses it's own built-in Docker daemon. So, if you build your images with your local Docker environment, they will not be directly accessible to Minikube. In order to overcome that problem, we will point our Docker commands directly to the Minikube built-in Docker daemon, building our images there, and making them directly accessible to Minikube. To accomplish that just do the following (only on macOS!):

	eval $(minikube docker-env)

Now the local Docker environment is pointing to the Minikube’s built-in Docker daemon. 

#### Build the container image 

Now, from within the "src" directory, le'ts build a Docker image this way:

	docker build -f Dockerfile -t helloworld:1.0 .

We are done but if you want you can try your containerized microservice by running the container:

	docker run --name helloworld -d -p 8080:8080 helloworld:1.0

Now check with your browser (localhost:8080) if the microservice is running. 

Finally, stop the container (this is important as later we will need to use port 8080 from Kubernetes):

	docker stop helloworld

### 2.4. Deploy your microservice

#### The Deployment object

Kubernetes requires a microservice to be provided as a set of container images plus an optional configuration about storage and networking. Kubernetes does not use the term "microservice" in its API methods and objects, but there's one API object that implicitly relates to it, the Deployment object. A Deployment is basically a configuration that instructs Kubernetes how to create and update instances of a microservice. You can specify the Deployment yourself or you can use a default one as we will do next.

Let's deploy our Hello World microservice. Before deploying let's check the status of the cluster:

	kubectl version
	kubectl cluster-info
	kubectl get nodes

The run command creates a new Deployment (a default one). We need to provide the deployment name and microservice image location (usually a repository url but here we will use the local image name). We want to run the microservice on a specific port so we add the --port parameter:

	kubectl run helloworld --port=8080 --image-pull-policy=Never --image=helloworld:1.0

To list your deployments use the get deployments command:

	kubectl get deployments

The READY column should show 1/1 Pods. If it shows 0/1 then something went wrong. You can get more information with:

	kubectl describe deployments/helloworld

If this information is not enough to solve the error you should check the logs of the Pod created for this deployment (see next subsection).

To obtain the related YAML file (you did not used it as you relied on default values):

	kubectl get deployments/helloworld -o=yaml

Note: You can delete a deployment with "kubectl delete deployment NAME"

#### Pods

A Pod is a single instantiation of your microservice. You can execute multiple Pods (replicas) to scale a microservice. 

When we deployed our microservice before, Kubernetes executed one Pod. We can find the name of the pod (POD_NAME) that instantiates our microservice with:

	kubectl get pods -o go-template --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}'

You can obtain information about the Pod with 
	
	kubectl describe pod POD_NAME

To see the logs relatd to the Pod:

	kubectl logs pods/POD_NAME

If there would be more than one container in the Pod, you should use this:

	kubectl logs pods/POD_NAME helloworld

Kubernetes creates an endpoint for each pod, based and the pod name. However, these endpoints are not directly visible from outside (the Kubernetes cluster is running on a private network). The kubectl command can create a proxy that will forward communications to the local port 8001 into the private network: 

	kubectl proxy

The proxy can be terminated by pressing control-C and won't show any output while its running. Now, in a different terminal we can do:

	curl http://localhost:8001/api/v1/namespaces/default/pods/POD_NAME/proxy/


#### Services

The execution of a microservice is done by one or more replicated Pods, that are dynamically created and destroyed. To isolate the microservice clients from this complexity in Kubernetes you define a Service configuration. Services allow your microservices to receive traffic. There are different types of Services, the default type is ClusterIP, that just exposes the Service on an internal IP in the cluster (not visible from outside). 

Try the following:

	kubectl get services

You will see a service called "kubernetes" that is created by default when minikube starts the cluster.

Let's create a new Service for our Hello World microservice:

	kubectl expose deployment/helloworld --type="NodePort" --port 8080

The NodePort Service type is accessible from outside the cluster. Let's get information about the new service:

	kubectl describe service helloworld

Write down the port number of the NodePort field (NODE_PORT). 

Now you can already call your microservice this way:

	curl $(minikube ip):NODE_PORT

You should see the "Hello World!" message.

### 2.5. Scale your microservice

If traffic increases, we will need more instances/replicas of the microservice (Pods) to keep up with user demand. The possibility to scale parts of an application independently is one of the advantages of microservices. The following figure, from a [James Lewis' article](https://martinfowler.com/articles/microservices.html), illustrates the idea:

<p align="center">
  <img src="microservices_scale.png" width="500">
</p>

Let's start by checking the current number of Pods:

	kubectl get deployments

The READY column shows the ratio of CURRENT to DESIRED replicas (Pods). We should have 1/1 Pods. 

Next, let’s scale the Deployment to 4 replicas:

	kubectl scale deployments/helloworld --replicas=4

Type again:

	kubectl get deployments

Now we have 4 instances of the microservice available. Next, let's check if the number of Pods changed:

	kubectl get pods -o wide


### 2.6. What about data?

The management of data in a microservices-based architecture is a very complex and controversial topic. Ideally, microservices should be independent also in terms of data, as illustrated by the following figure from the [James Lewis' article](https://martinfowler.com/articles/microservices.html):

<p align="center">
  <img src="microservices_data.png" width="500">
</p>

However, this may be difficult to accomplish in reality as usually companies store their data into dedicated systems or cloud services. But let's skip these issues here and just mention some options that Kubernetes offers regarding data storage. Regarding persistent data that can be directly stored in the filesystem (i.e. files), we cannot directly store them within a Pod's container as Pods are ephimeral. Kubernetes provides tools to deal with that such as PersistentVolumes. It's relativelly easy to use a PersistentVolume if we don't need it to scale along with our Pods (static). Otherwise, the thing becomes more complex and we will need to deal with Kubernetes' StatefulSets. Here, we are not going to experiment with none of those situations. Regarding structured data that we typically store within a database, some popular DBMS such as MongoDB provide help for running them, in a scalable way, within Kubernetes. But doing this can be quite complex, and we will not address that option either. 

The microservice in this tutorial is stateless, so we don't have to deal with data. However, [here](mongodb.md) we have extended the tutorial to make it use a MongoDB database running in a second container within the same Pod. It's not a real solution, but can be a simple shortcut in case you plan to use Kubernetes in your project. This is not part of this lab session.     

## 3. Lab assignment 

### 3.1. Creating and deploying your own microservices-based car rental Web API

#### Description

As an example microservices-based application you will create and deploy a simple car rental web API. It will consist in the same functionalities that were described in the lab session about web APIs:

- Request a new rental: An endpoint to register a new rental order. Input fields will include the car maker, car model, number of days and number of units. The total price of the rental will be returned to the user along with the data of the requested rental.
 
- Request the list of all rentals: An endpoint that will return the list of all saved rental orders (in JSON format). 

Regarding the DATA, you can directly store it within a JSON file into the disk, as if you were working locally. These data can be lost anytime, as the Pods are ephimeral, but it will be ok for this lab session. If you are interested in knowing how to do that better, you can use a Kubernete's PersistentVolume as explained [here](persistentvolume.md) (optional).

## 4.  Submission

You need to upload the following files to your BSCW's lab group folder before the next lab session:

* A tarball containing the source files.
* A .pdf with a report describing the steps taken to complete the assignment, including screenshots of the application output.   

## 4. Further reading

* [James Lewis' article about Microservices](https://martinfowler.com/articles/microservices.html).