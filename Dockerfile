# After pulling, run make all
# If the submodule does not init, try: git submodule update --init
# Use docker build -t wonko . to build the docker image
# Use docker run -a STDOUT -p 12000:12000 wonko
# The config is in ./resources/config.edn.dev - you'll need to ensure Kafka
# and Zookeeper are running, and that they are the appropriate versions. See
# Trillian's docker-compose for information on how that works.

# based on https://github.com/docker-library/docs/tree/master/clojure
# using the 'straightforward' version because all dependencies are already
# available in the 'lib' directory, since project.clj has :local-repo = "lib"
FROM clojure

# create log directory
RUN mkdir /var/log/wonko
RUN chown $USER:$USER /var/log/wonko

EXPOSE 12000

COPY . /usr/src/app
WORKDIR /usr/src/app

CMD ["./bin/svc", "start", "wonko"]