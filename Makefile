CONF_DIR=./itc_run
DOCKER_CMD=docker
DOCKER_COMP_CMD=docker compose

.FORCE:

back: .FORCE
	./gradlew build -x test && docker build -t disk91/itc .

setup_base: .FORCE
	-mkdir $(CONF_DIR)
	cp -R ./itc/* $(CONF_DIR)
	rm $(CONF_DIR)/postgresql/data/.empty
	-sudo chown nobody:nogroup $(CONF_DIR)/prometheus
	-sudo chown 472:root $(CONF_DIR)/grafana

# If you have a problem running setup with missing X11 ...
# this is related to docker login
# run gpg2 --full-generate-key
setup_shared: setup_base .FORCE
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo up --force-recreate -d ; cd -
	-sleep 10
	$(DOCKER_CMD) exec mongo-config-01 sh -c "mongosh < /scripts/config-server"
	$(DOCKER_CMD) exec shard-01-node-a sh -c "mongosh < /scripts/shard-01-server"
	$(DOCKER_CMD) exec shard-02-node-a sh -c "mongosh < /scripts/shard-02-server"
	$(DOCKER_CMD) exec shard-03-node-a sh -c "mongosh < /scripts/shard-03-server"
	-sleep 10
	$(DOCKER_CMD) exec mongo-router-01 sh -c "mongosh < /scripts/router-server"
	-sleep 10
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo stop ; cd -

setup: setup_shared .FORCE

clear-setup: stop
	echo "Are you sure, this will delete all mongodb data ?"
	read response
	rm -rf $(CONF_DIR)/mongo/data
	rm -rf $(CONF_DIR)/mongo/configuration

build: back

install: setup_shared back

start:
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc up -d ; cd -

start-clean:
	$(DOCKER_CMD) network prune
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc up --force-recreate -d ; cd -

stop:
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc stop ; cd -