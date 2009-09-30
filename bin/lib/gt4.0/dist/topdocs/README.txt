Nimbus

See the documentation at http://workspace.globus.org

Under the "bin" directory:

* all-build-and-install.sh - The main installation script
* clients-only-build-and-install.sh - Main client-only installation script

* all-uninstall.sh - Remove anything installed except persistence directory
* delete-persistence-directory.sh - Remove persistence directory (also, there
                     are persistence mgmt scripts in the installation, see the
                     "share/nimbus" directory)

Other:

* all-clean.sh - Clean up any locally generated build artifacts
* all-build.sh - Just build ('dist')
* all-install.sh - Just install (no build recheck before install)
* clients-only-build.sh - Just build clients ('dist')
* clients-only-install.sh - Just install clients (no build recheck before install)
