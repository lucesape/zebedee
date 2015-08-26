from carboni.io/java-component


# Consul
WORKDIR /etc/consul.d
RUN echo '{"service": {"name": "zebedee-cms", "tags": ["blue"], "port": 8080, "check": {"script": "curl http://localhost:8080 >/dev/null 2>&1", "interval": "10s"}}}' > zebedee.json

# Add the repo source
WORKDIR /usr/src
ADD git_commit_id /usr/src/
ADD ./target/dependency /usr/src/target/dependency
ADD ./target/classes /usr/src/target/classes
ADD ./src/main/web /usr/src/src/main/web
#ADD ./zebedee-cms/target/*-jar-with-dependencies.jar /usr/src/target/

# Temporary: expose Elasticsearch
EXPOSE 9200

# Update the entry point script
RUN mv /usr/entrypoint/container.sh /usr/src/
RUN echo "java -Xmx2048m \
          -Drestolino.files=src/main/web \
          -Drestolino.classes=target/classes \
          -Drestolino.packageprefix=com.github.onsdigital.zebedee.api \
          -cp \"target/dependency/*:target/classes/\" \
          com.github.davidcarboni.restolino.Main" >> container.sh