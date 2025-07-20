## IoTowerControl

This project is an open-source IoT platform made to support industrial and scalable project.
The current status is "in progress" and the project is not yet ready for being used.
It's currently published for sharing the documentation aspects, concepts and the architecture.

### Installation

```bash
# eventualy setup the data directory root by editing Makefile __CONF_DIR__ variable  
# deploy the project data tree 
$ make install

# edit the $(CONF_DIR)/docker_compose.yml file to set the project environment variables
# then if you want to use nginx as a container / setup domain name in DOMAIN_NAME and SECONDARY_NAME when multiple
$ make setup_nginx

# The system is ready to start
$ make start
```