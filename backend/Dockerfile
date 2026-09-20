# Image d'exécution du backend.
#
# Elle n'embarque que l'artefact : la construction se fait sur la machine, où le
# cache Maven est déjà chaud. Reconstruire l'image prend alors quelques secondes
# au lieu de plusieurs minutes, ce qui compte quand on relance la recette après
# chaque correction. Le jour où une chaîne d'intégration continue existera, elle
# construira dans une première étape ; rien d'autre ne changera ici.
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S clubify && adduser -S clubify -G clubify
USER clubify
WORKDIR /app

COPY --chown=clubify:clubify target/backend-*.jar application.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=demo

# La mémoire est bornée : sans cela, la JVM prend le quart de la machine d'Omar.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "application.jar"]
