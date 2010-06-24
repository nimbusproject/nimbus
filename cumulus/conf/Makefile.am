
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
cumulus_src_dir=$(srcdir)
cumulus_pydist_dir=$(cumulus_src_dir)/dist/cumulus-$(version)
cumulus_dst_dir=$(prefix)
authz_bk_up=$(cumulus_dst_dir)/etc/authz.db.bak

installclean:
	rm -rf $(cumulus_dst_dir)

predest:
	mkdir -p $(cumulus_dst_dir)
	mkdir -p $(cumulus_dst_dir)/log

tests: 
	(cd $(cumulus_dst_dir)/tests && ./run-all.sh)

install: predest
	cp $(cumulus_src_dir)/etc/cumulus.ini ~/.nimbus
	cp -r $(cumulus_src_dir)/etc $(cumulus_dst_dir)
	cp -r $(cumulus_src_dir)/etc/env.sh $(cumulus_dst_dir)
	$(SQLITE) -batch $(cumulus_dst_dir)/etc/authz.db < $(cumulus_src_dir)/etc/acl.sql
	echo "done"

