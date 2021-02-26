THIS_MKFILE_PATH := $(abspath $(lastword $(MAKEFILE_LIST)))
THIS_MKFILE_DIR := $(dir $(THIS_MKFILE_PATH))

include $(THIS_MKFILE_DIR)scripts/common/Makefile

GCP_PROJECT_ID ?= $(shell gcloud config list --format 'value(core.project)')
GKE_BASE_MACHINE_TYPE ?= n1-highmem-2

CLUSTER_NAME=portable-serverless-workshop
CLUSTER_ZONE=us-central1-a

gke-check-dependencies: check-dependencies
	@$(call check-var-defined,GCP_PROJECT_ID)
	@$(call check-dependency,gcloud)
	@$(call echo_pass,gke-base dependencies verified)

create-gke-cluster: gke-check-dependencies
	@$(call print-header,"Creating a new cluster Creating GKE")
	@$(call print-prompt)
	gcloud --quiet container --project $(GCP_PROJECT_ID) clusters create ${CLUSTER_NAME} --num-nodes 2 --machine-type $(GKE_BASE_MACHINE_TYPE) --zone ${CLUSTER_ZONE}
	export PROJECT_ID=$GCP_PROJECT_ID

scale-gke-cluster-%: gke-check-dependencies
	@$(call print-header,"Scaling my ${CLUSTER_NAME}")
	@$(call print-prompt)
	gcloud --quiet container clusters resize ${CLUSTER_NAME} --num-nodes=$* --zone ${CLUSTER_ZONE}

destroy-gke-cluster: gke-check-dependencies 
	@$(call print-header, "Delete GKE cluster")
	@$(call print-prompt)
	gcloud --quiet container --project $(GCP_PROJECT_ID) clusters delete ${CLUSTER_NAME} --zone ${CLUSTER_ZONE} 
	@$(call echo_stdout_footer_pass,GKE Cluster Deleted)

ccloud:
	@echo "Installing ccloud"
	@curl -L -s --http1.1 https://cnfl.io/ccloud-cli | sh -s -- -b .
	@sudo install -m 755 ccloud ~/bin/ccloud
	@rm -f ccloud
	@$(caller echo_stdout_footer_pass, "ccloud cli installed")

install-deps: ccloud
	@brew bundle
	@$(caller echo_stdout_footer_pass, "dependencies installed")

create-ccloud-cluster:
	@$(call print-header,"☁️ Creating ccloud Cluster...")
	@$(call print-prompt)
	./scripts/ccloud/ccloud_stack_create.sh

destroy-ccloud-cluster:
	@$(call print-header,"🧨 Destroying ccloud Cluster...")
	@$(call print-prompt)
	./scripts/ccloud/ccloud_stack_destroy.sh ${THIS_MKFILE_DIR}$(filter-out $@,$(MAKECMDGOALS))

ccloud-get-kafka-key-password:
	. ./scripts/ccloud/ccloud_key_password.sh
	@$(call ccloud-validate)

ccloud-validate:
	@$(call print-header,"🌐 environment variables...")
	@$(call print-prompt)
	@echo "CLUSTER_ID: $(CLUSTER_ID)"
	@echo "KAFKA_BOOTSTRAP_SERVERS: $(KAFKA_BOOTSTRAP_SERVERS)"
	@echo "KAFKA_USERNAME: $(KAFKA_USERNAME)"
	@echo "KAFKA_PASSWORD: $(KAFKA_PASSWORD:0:6)..."
	@echo "SCHEMA_REGISTRY_URL: $(SCHEMA_REGISTRY_URL)"
	@echo "SCHEMA_REGISTRY_ID: $(SCHEMA_REGISTRY_ID)"
	@echo "SCHEMA_REGISTRY_KEY: $(SCHEMA_REGISTRY_KEY)"
	@echo "SCHEMA_REGISTRY_PASSWORD: $(SCHEMA_REGISTRY_PASSWORD:0:6)..."
	@echo "KSQLDB_ENDPOINT: $(KSQLDB_ENDPOINT)"
	@echo "KSQLDB_USERNAME: $(KSQLDB_USERNAME)"
	@echo "KSQLDB_PASSWORD: $(KSQLDB_PASSWORD:0:6)..."
	@echo "PROJECT_ID: $(PROJECT_ID)"

kube-generate-secret: ccloud-validate
	envsubst < ws-to-kafka-app-secret-template.yaml > ws-to-kafka-app-secret.yaml
	
kube-deploy-ws-to-kafka:
	skaffold