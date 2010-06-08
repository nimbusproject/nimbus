
MAINTAINERCLEANFILES = \
        $(srcdir)/INSTALL \
        $(srcdir)/aclocal.m4 \
        $(srcdir)/autoscan.log \
        $(srcdir)/compile \
        $(srcdir)/config.guess \
        $(srcdir)/config.h.in \
        $(srcdir)/config.sub \
        $(srcdir)/configure \
        $(srcdir)/configure.scan \
        $(srcdir)/depcomp \
        $(srcdir)/install-sh \
        $(srcdir)/ltmain.sh \
        $(srcdir)/missing \
        $(srcdir)/mkinstalldirs 

version=0.1
cumulus_src_dir=$(srcdir)/src
cumulus_pydist_dir=$(cumulus_src_dir)/dist/cumulus-$(version)
cumulus_dst_dir=$(prefix)
authz_bk_up=$(cumulus_dst_dir)/etc/authz.db.bak

clean:
	rm -rf $(srcdir)/dist
	rm -rf $(srcdir)/cumulus.egg-info
	rm -rf $(srcdir)/cumulus-$(version)
	rm -rf $(cumulus_pydist_dir)

installclean:
	rm -rf $(cumulus_dst_dir)

predest:
	mkdir -p $(cumulus_dst_dir)
	mkdir -p $(cumulus_dst_dir)/log

virtenv: 
	$(PYTHON) $(cumulus_src_dir)/virtualenv.py -p $(PYTHON) --no-site-packages $(cumulus_dst_dir)

pydist: virtenv
	(cd $(cumulus_src_dir) && $(cumulus_dst_dir)/bin/python ./setup.py sdist)
	(cd $(cumulus_src_dir)/dist && tar -zxvf cumulus-$(version).tar.gz)

tests: 
	(cd $(cumulus_dst_dir)/tests && ./run-all.sh)

all: clean predest virtenv pydist 
	echo "done"

install: all
	cp $(cumulus_src_dir)/etc/cumulus.ini ~/.nimbus
	cp -r $(cumulus_src_dir)/tests $(cumulus_dst_dir)
	cp -r $(cumulus_src_dir)/docs $(cumulus_dst_dir)
	cp -r $(cumulus_src_dir)/etc $(cumulus_dst_dir)
	cp -r $(cumulus_src_dir)/etc/env.sh $(cumulus_dst_dir)
	(cd $(cumulus_pydist_dir) && $(cumulus_dst_dir)/bin/python ./setup.py install)
	$(SQLITE) -batch $(cumulus_dst_dir)/etc/authz.db < $(cumulus_src_dir)/etc/acl.sql
	echo "done"

