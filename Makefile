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

setup_nginx: .FORCE
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile nginx --profile itc --profile mongo up --no-deps nginx -d ; cd -
	sleep 10
	@read -p "Domain name? " DOMAIN; cd $(CONF_DIR) ;$(DOCKER_COMP_CMD) run --rm certbot certonly --webroot --webroot-path /var/www/certbot/ -d $$DOMAIN ; cd -
	sleep 5
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile nginx --profile itc --profile mongo stop nginx ; cd -
	mv $(CONF_DIR)/nginx/configuration/default.conf $(CONF_DIR)/nginx/configuration/default.conf.withoutssl
	cp $(CONF_DIR)/nginx/configuration/default.conf-withssl $(CONF_DIR)/nginx/configuration/default.conf

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
	-sleep 20
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
	@if [ -d $(CONF_DIR)/nginx/ssl/accounts ]; then \
    		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile nginx up  -d ; cd - ;\
    else \
    		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc up -d ; cd - ;\
    fi

start-clean:
	$(DOCKER_CMD) network prune
	@if [ -d $(CONF_DIR)/nginx/ssl/accounts ]; then \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile nginx up --force-recreate -d ; cd - ;\
    else \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc up --force-recreate -d ; cd - ;\
    fi

stop:
	@if [ -d $(CONF_DIR)/nginx/ssl/accounts ]; then \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile nginx stop ; cd - ;\
	else \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc stop ; cd - ;\
    fi

