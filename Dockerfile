FROM eclipse-temurin:24-jre
COPY ./build/libs/iot-0.0.1-SNAPSHOT.jar itc.jar
ENTRYPOINT ["java","-jar","/itc.jar"]
