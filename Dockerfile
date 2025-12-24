FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

COPY target/nfs-demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
