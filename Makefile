# Some inspiration: https://github.com/git/git/blob/master/Makefile
# More inspiration: https://clarkgrubb.com/makefile-style-guide
SHELL = /bin/bash

target = ./target
jar-file = $(target)/braid-$(VERSION).jar
pom-file = $(target)/braid-$(VERSION).pom.xml
javascripts = resources/public/js/prod/base.js resources/public/js/prod/desktop.js resources/public/js/prod/gateway.js
dev-javascripts = resources/public/js/dev/desktop.js

# This is the default target because it is the first real target in this Makefile
.PHONY: default # Same as "make docker-build"
default: test

# https://github.com/git/git/blob/9b88fcef7dd6327cc3aba3927e56fef6f6c4d628/GIT-VERSION-GEN
# NB: since the following recipe name matches the following include, the recipe is *always* run and VERSION is always set. Thank you, make.
# NB: the FORCE dependency here is critical.
.PHONY: FORCE
.make.git-version-file: FORCE
	@$(SHELL) ./bin/vgit $@
-include .make.git-version-file
export VERSION

.PHONY: version # Report the git version used to tag artifacts
version:
	@echo $(VERSION)

$(target)/:
	mkdir -p $@

.PHONY: test # Run the entire test suite
test:
	clj -M:test:project/test-clj
	clj -M:test:project/test-cljs

# A grouped target (recent GNU make required) for all the production javascript assets:
$(javascripts) &: cljs.release.edn deps.edn $(shell find src/ -type f -or -name '*.cljs' -name '*.cljc')
	clj -M:cljs-release

# A grouped target (recent GNU make required) for all the development javascript assets:
$(dev-javascripts) &: cljs.desktop.edn deps.edn $(shell find src/ -type f -or -name '*.cljs' -name '*.cljc')
	clj -M:cljs-desktop

$(pom-file): deps.edn | $(target)/
	clj -M:project/pom --force-version $(VERSION)
	mv pom.xml $(DESTDIR)$@

$(jar-file): deps.edn $(pom-file) $(javascripts) $(shell find src/ -type f -or -name '*.clj' -name '*.cljc') | $(target)/
	rm -rf resources/public/js/dev
	TAOENSSO_TIMBRE_MIN_LEVEL_EDN=':info' clj -X:project/uberjar uberjar :pom-file \"$(DESTDIR)$(pom-file)\" :jar \"$@\"


.PHONY: jar # Build the jar file
jar: $(jar-file)

.PHONY: pom # Build the pom file
pom: $(pom-file)

.PHONY: run # Run the project directly from Clojure
run: ENVIRONMENT ?= development
run: $(dev-javascripts)
	ENVIRONMENT=$(ENVIRONMENT) clj -X:server

.PHONY: run-jar # Run the project from the jar file
run-jar: ENVIRONMENT ?= staging
run-jar: $(jar-file)
	java -server -Xmx1228m -Djava.io.tmpdir=./tmp -Dfile.encoding=UTF8 -DENVIRONMENT=$(ENVIRONMENT) \
	-Dclojure.server.repl="{:port 6000 :address \"0.0.0.0\" :accept clojure.core.server/repl}" \
	-jar $(jar-file)

.PHONY: clean # Clean all temp files and revert working directory to virgin state
clean:
	rm -f .make.*
	rm -rf resources/public/js/prod/out/
	rm -f $(javascripts) $(dev-javascripts)
	rm -rf $(target)/*

# Copied from: https://github.com/jeffsp/makefile_help/blob/master/Makefile
# Tab nonesense resolved with help from StackOverflow... need a literal instead of the \t escape on MacOS
help: # Generate list of targets with descriptions
	@grep '^.PHONY: .* #' Makefile | sed 's/\.PHONY: \(.*\) # \(.*\)/\1	\2/' | expand -t20
