The spotinstances test suites require conf files that are different in some minor ways from
the default configurations in the "service/service/java/source" directory.

1) group-authz is disabled. The "authz-callout-ACTIVE.xml" file contains the disabled module.

2) There are more "network-pools/public" IP addresses to make sure the tests have enough IPs
   for all of the allocated VMs.

3) In the "other/common.conf" file, the "fake.lag.ms" setting is 50 instead of the usual 1000.

4) main.xml #1
>         <!-- Force this to be false in the unit tests, the backfill code is exercised directly.
>              Thus, the async manager needs to think backfill is enabled but this Backfill class
>              should not be submitting backfill requests. -->
564c567
<                   value="$ASYNC{backfill.enabled}" />
---
>                   value="false" />

5) main.xml #2, The unix domain socket based server cannot be used in the context of multiple
tests because it creates conflicts.
697a701,702
>     <!-- DISABLED: this throws an error in the tests when multiple Spring contexts
>                    are instantiated: the RMI registry is per JVM...
708a714
>     -->

6) The backfill and SI are enabled in "async.conf", they are normally disabled by default.

7) The "async.policies.minreservedmem" setting in "async.conf" is 256, this is expected
   by all of the calculations in the test suites.
