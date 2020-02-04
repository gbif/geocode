FROM java:8

RUN mkdir -p /usr/src/app/conf
WORKDIR /usr/src/app
COPY docker_application.properties /usr/src/app/application.properties
COPY docker_logback.xml /usr/src/app/conf/logback.xml

RUN curl -# -o /usr/src/app/geocode-ws.jar https://repository.gbif.org/repository/releases/org/gbif/geocode/geocode-ws/0.15/geocode-ws-0.15.jar

CMD [ "/usr/bin/java", "-jar", "geocode-ws.jar", "-conf", "application.properties", "-zkHost", "", "-stopSecret", "stop", "-httpPort", "4444", "-zkPath", "", "-httpAdminPort", "4445" ]
