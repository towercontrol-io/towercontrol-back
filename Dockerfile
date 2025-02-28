FROM openjdk:21
COPY ./build/libs/iot-0.0.1-SNAPSHOT.jar itc.jar
ENTRYPOINT ["java","-jar","/itc.jar"]
