CONF_DIR=./itc_run
DOCKER_CMD=docker
DOCKER_COMP_CMD=docker compose

.FORCE:

back: .FORCE
	./gradlew build -x test && docker build -t disk91/itc .

check-tag:
ifndef TAG
	$(error TAG is required. Usage: make push TAG=1.2.3)
endif

push-nce: check-tag .FORCE
	# Use make push-nce VERSION=1.2.3 to push the image with the tag 1.2.3 and latest
	$(DOCKER_CMD) login
	./gradlew build -x test && $(DOCKER_CMD) buildx build --platform linux/amd64,linux/arm64 -t disk91/itc-nce:$(TAG) .
	$(DOCKER_CMD) tag disk91/itc-nce:$(TAG) disk91/itc-nce:latest
	$(DOCKER_CMD) push disk91/itc-nce:$(TAG)
	$(DOCKER_CMD) push disk91/itc-nce:latest

push-ce: check-tag .FORCE
	# Use make push-ce VERSION=1.2.3 to push the image with the tag 1.2.3 and latest
	$(DOCKER_CMD) login
	./gradlew build -x test && $(DOCKER_CMD) buildx build --platform linux/amd64,linux/arm64 -t disk91/itc-back:$(TAG) .
	$(DOCKER_CMD) tag disk91/itc-back:$(TAG) disk91/itc-back:latest
	$(DOCKER_CMD) push disk91/itc-back:$(TAG)
	$(DOCKER_CMD) push disk91/itc-back:latest

setup_base: .FORCE
	mkdir -p $(CONF_DIR)
	cp -R ./itc/* $(CONF_DIR)
	cp ./itc/.env $(CONF_DIR)/
	rm $(CONF_DIR)/postgresql/data/.empty
	rm $(CONF_DIR)/prometheus/data/.empty
	-sudo chown -R nobody:nogroup $(CONF_DIR)/prometheus
	-sudo chown -R 472:root $(CONF_DIR)/grafana

setup_nginx: .FORCE
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile nginx --profile itc --profile mongo up --no-deps nginx -d ; cd -
	sleep 10
	@read -p "Domain name? " DOMAIN; cd $(CONF_DIR) ;$(DOCKER_COMP_CMD) run --rm certbot certonly --webroot --webroot-path /var/www/certbot/ -d $$DOMAIN ; cd -
	sleep 5
	cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile nginx --profile itc --profile mongo stop nginx ; cd -
	mv $(CONF_DIR)/nginx/configuration/default.conf.template $(CONF_DIR)/nginx/configuration/default.conf.template.withoutssl
	cp $(CONF_DIR)/nginx/configuration/default.conf.template.withssl $(CONF_DIR)/nginx/configuration/default.conf.template

# If you have a problem running setup with missing X11 ...
# this is related to docker login
# run gpg2 --full-generate-key
setup_sharded: setup_base .FORCE
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

setup: setup_sharded .FORCE

clear-setup: stop
	echo "Are you sure, this will delete all mongodb data ?"
	read response
	rm -rf $(CONF_DIR)/mongo/data
	rm -rf $(CONF_DIR)/mongo/configuration

build: back

install: setup_sharded back

start:
	@if [ -d $(CONF_DIR)/nginx/ssl/accounts ]; then \
    		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile nginx --profile monitoring up -d ; cd - ;\
    else \
    		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile monitoring up -d ; cd - ;\
    fi

start-clean:
	$(DOCKER_CMD) network prune
	@if [ -d $(CONF_DIR)/nginx/ssl/accounts ]; then \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile nginx --profile monitoring up --force-recreate -d ; cd - ;\
    else \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile monitoring up --force-recreate -d ; cd - ;\
    fi

stop:
	@if [ -d $(CONF_DIR)/nginx/ssl/accounts ]; then \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile nginx --profile monitoring -t 600 stop ; cd - ;\
	else \
		cd $(CONF_DIR) ; $(DOCKER_COMP_CMD) --profile mongo --profile itc --profile monitoring stop -t 600 ; cd - ;\
    fi

