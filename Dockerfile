FROM johnnyjayjay/leiningen:openjdk11 AS build
WORKDIR /usr/src/xkcdiscord
COPY . .
RUN apt-get update && apt-get -y --no-install-recommends install libsodium-dev && lein uberjar

FROM openjdk:11
ARG version
ARG jar=xkcdiscord-$version-standalone.jar
WORKDIR /usr/app/xkcdiscord
COPY --from=build /usr/src/xkcdiscord/target/uberjar/$jar .
RUN apt-get update && apt-get -y --no-install-recommends install libsodium-dev
ENV jar=$jar
CMD java -jar $jar
