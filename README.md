# portainer-agent

A agent for portainer, add endpoint without expose docker tcp port.

## Features

- Expose docker unix socket to a tcp port
- Register endpoint automatically

## Quick Start

### With docker (recommended)

#### Build image

`docker build -t portainer-agent .`

#### Start container

Sample `docker-compose.yml`

```yml
version: '3.2'
services:
  portainer_agent:
    image: portainer-agent
    container_name: portainer-agent
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - /etc/hosts:/etc/hosts:ro
    restart: always
    network_mode: "host"
    environment:
      - PORTAINER_API_URL=http://127.0.0.1:9000
      - PORTAINER_USERNAME=admin
      - PORTAINER_PASSWORD=xxxxx
      - PORTAINER_AGENT_IP=127.0.0.1
      - PORTAINER_AGENT_PORT=5000
```

And then use `docker-compose up -d` to start container.

### Without docker

#### Compile

`mvn clean package -U -Dmaven.skip.test=true`

#### Start agent

`java -jar portainer-agent-1.0-SNAPSHOT.jar`

## Configuration

All configurations can be set with environment parameters.

- PORTAINER_API_URL(**Required**)
- PORTAINER_USERNAME(**Required**)
- PORTAINER_PASSWORD(**Required**)
- PORTAINER_AGENT_IP(*Optional*): Default is resolving local hostname. The hostname must be defined in `/etc/hosts`, otherwise it will throw a `java.net.UnknownHostException`
- PORTAINER_AGENT_PORT(*Optional*): Default is `5000`