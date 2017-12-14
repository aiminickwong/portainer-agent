FROM openjdk:jre-alpine

COPY . /usr/src/myapp
WORKDIR /usr/src/myapp

CMD ["java", "-jar","portainer-agent-1.0-SNAPSHOT.jar"]