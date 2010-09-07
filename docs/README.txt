Docs are in a state of transition right now. We are writing docs in HTML with
the aid of some M4 macros. Hopefully this will soon be replaced with
something like Sphinx.

Directory structure:

src/     - source html files containing M4 references
html/    - directory for html output. contents are gitignored
m4/      - macro definition files
scripts/ - shell scripts to do useful things with docs

How to do stuff:

To locally view docs, run build-and-serve-locally.sh and then open your web
browser to the URL printed out.

To push docs to the live site, run push_to_mcs_web.sh. You must be on an MCS
shell (with access to /mcs/ filesystem). By default this script will drop
docs in the dev/ subdirectory of the docs section of the site. You can
supply an argument to have it go somewhere else. Like:
     push_to_mcs_web.sh foo/

