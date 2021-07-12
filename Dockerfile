FROM johnnyjayjay/leiningen:openjdk11 AS build
WORKDIR /usr/src/xkcdiscord
COPY . .
RUN lein uberjar

FROM openjdk:11
ARG jar=xkcdiscord-*-standalone.jar
WORKDIR /usr/app/xkcdiscord
COPY --from=build /usr/src/xkcdiscord/target/uberjar/$jar .
ENV jar=$jar
CMD java -jar $jar
