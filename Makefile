.PHONY:	env tests

ARCHIVA_USERNAME = $(shell grep access_key ~/.s3cfg | head -n1 | awk -F ' = ' '{print $$2 }')
ARCHIVA_PASSPHRASE = $(shell grep secret_key ~/.s3cfg | head -n1 | awk -F ' = ' '{print $$2}')

LEIN = HTTP_CLIENT="curl --insecure -f -L -o" lein

LEIN_ENV=ARCHIVA_USERNAME="${ARCHIVA_USERNAME}" ARCHIVA_PASSPHRASE="${ARCHIVA_PASSPHRASE}"

all: deps lein-deps bin/lein-classpath force-config-edn log-dirs checkouts/wonko-client

distclean:
	rm -rf ./.m2

clean:
	$(LEIN_ENV) $(LEIN) clean

lein-deps:
	$(LEIN_ENV) $(LEIN) deps
	rm -f bin/lein-classpath # required for end2end tests on ci

download-lein-libs: target/wonko-1.0.0.jar

target/wonko-1.0.0.jar: project.clj
	$(LEIN_ENV) $(LEIN) do clean, deps

env:
	@echo "ARCHIVA_USERNAME=$(ARCHIVA_USERNAME)"
	@echo "ARCHIVA_PASSPHRASE=$(ARCHIVA_PASSPHRASE)"

bin/lein-classpath: project.clj
	$(LEIN_ENV) ./bin/gen-lein-classpath $@.tmp && mv -f $@.tmp $@

force-config-edn:
	rm -f resources/config.edn
	make resources/config.edn

resources/config.edn: resources/config.edn.dev
	cp $< $@

start-ci-services:
	./bin/deps start zookeeper &
	./bin/deps start kafka &

stop-ci-services:
	./bin/deps stop zookeeper || true
	./bin/deps stop kafka || true

ci: distclean force-config-edn
	make start-ci-services
	make tests

tests: download-lein-libs
	$(LEIN_ENV) $(LEIN) test

deps:
	./bin/deps install all

deps-check:
	./bin/deps check all

deps-pull:
	git submodule update --init

/var/log/wonko:
	sudo mkdir -p /var/log/wonko
	sudo chown -R "${USER}" /var/log/wonko/

log-dirs: /var/log/wonko

checkouts:
	mkdir checkouts

../wonko-client:
	git clone git@github.com:StaplesLabs/wonko-client.git ../wonko-client

checkouts/wonko-client: ../wonko-client checkouts
	ln -fs ../../wonko-client $@
