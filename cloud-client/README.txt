This directory is for building the cloud client.

The scripts and directory layout that makes the cloud client work is all contained in
"nimbus-cloud-client-src".  However, this is missing the embedded Globus directory.

If you have checked this out of version control in order to build a releasable cloud client, you
will need to run this script:

  bash ./builder/get-wscore.sh

But only if the Nimbus installer hasn't put the wscore tarball into ../tmp/ already.  If so, then the next script (./builder/dist.sh) will pick up that instead.

That script will retrieve the necessary wscore binary tarball and place it in the same directory
as this README file.

Now that you have retrieved the tarball, you can run the following script to make a release:

  bash ./builder/dist.sh

This will do the following:

  1) Remove any previous dist directory completely: something basically equivalent to
     "rm -rf ./nimbus-cloud-client-011".
     *** Never hand-alter any files in this dist directory.

  2) Create a fresh dist directory (e.g. "nimbus-cloud-client-011").

  3) Copy the entire contents of "nimbus-cloud-client-src" into the dist directory.

  4) Checksum the wscore tarball that was downloaded and compare it to the expected value.

  5) Expand that tarball into the dist directory, e.g. "nimbus-cloud-client-011/lib/globus"
     It expects that this directory does not exist yet.

  6) Set up that directory as GLOBUS_LOCATION for the next step

  7) Call the "../bin/clients-only-build-and-install.sh" script in the Nimbus source tree.
     Since the embedded Globus directory is set up as GLOBUS_LOCATION, it will install the
     client libraries there.

  8) Tar/gz the dist directory as e.g. "nimbus-cloud-client-011.tar.gz"


Notes:

  - You need to have the Nimbus source tree, in particular "scripts", "messaging", and the
    "service/client" directories.

  - Once you have downloaded the wscore binary tarball, you can make a fresh release over
    and over without more downloads.  This is intentional, this is why downloading that is
    a first step.

  - The wscore tarball, cloud client dist directory, and cloud client tarball should never be
    checked into version control.

  - To update names, wscore tarball URL, expected checksum, etc., see "./builder/environment.sh"
