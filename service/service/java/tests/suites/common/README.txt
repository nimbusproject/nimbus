====================================
RM API and Workspace Service Testing
====================================


This testing infrastructure allows tests from the RM-API level down.  An entire Spring context
is instantiated with real components.

The "service/service/java/tests/suites/" directory is broken up into separate directories,
each one (except "common") is a test suite.

A "test suite" is simply a collection of tests (currently TestNG) that run in the same Spring
context and can all run with one setup of the system.

You would want to change the Spring context around in order to:

	* write tests that need alternative configuration settings (there are many configurations
	  that are only set and validated at initialization time)

	* introduce an alternative class implementation for a Spring bean

	* introduce mocks

	* develop code to begin with (use the test system as a way to quickly see your changes)


A Spring context is loaded by finding a mock NIMBUS_HOME.

	* Each suite has its own home directory which has a copy (but modified where necessary)
	  of the Nimbus 'services/etc' and 'services/share' directories that show up in a
	  deployment.

	* NOTE: The environment variable NIMBUS_HOME should *not* be set in your testing
	  environment, it is determined and set programmatically for each suite.


Setup

	* When each suite is set up, a new var directory is created under the NIMBUS_HOME and
	  the databases are created.

	* Ant is required to be on your PATH in order for the setup routine to work, it is calling
	  a shell script which needs ant ("full-reset.sh").


Teardown

	* The entire var directory is removed on teardown.  You can disable that behavior via JVM
	  system property if you want to analyze something etc. (see the code).


Fake mode

	* Most of the suites are probably configured to be in "fake mode" which is where the
	  workspace service "pretends" to tell the VMM to do things but actually only logs
	  what "would have" happened at various points in time.
	  (See: "services/etc/nimbus/workspace-service/other/common.conf")

	* During development (or if you are dealing with 'real live' acceptance tests), you can
	  turn off your fake mode and drive real events which could be very useful for testing
	  how your changes are working with other Nimbus components.


Running the test suites in IntelliJ IDEA
---------------------------------------

	To get started quickly, find the "BasicSuite" class in the "basic" module.  Right-click
	it and choose the "Run BasicSuite" option.

	That works because the "src" directory is labelled as a "test classes" directory in the
	module configuration.  It creates a run configuration called "BasicSuite" on the fly
	which (if you "edit configurations" in the Run/Debug configuration drop-down) is just
	running this one class in the package.

	But in general the intent is that anything in the whole suite package that has a @Test
	annotation should be run.

		* To fix, choose the "All in package" radio button instead of "Class" and make sure
		  it is the right value ("org.globus.workspace.testing.suites.basic" now instead of
		  "org.globus.workspace.testing.suites.basic.BasicSuite").
		  
		* While you are there, make sure "Use classpath and JDK of module" is set to the
		  current module (e.g. "basic").

	*Any* manual configurations should not be necessary in the future, there will be testng.xml
	files, for example, which will let scripts drive the tests (as well as making it more
	convenient to "just do the right thing" for other IDEs).


Creating a new suite of your own
--------------------------------

To start, just copy the structure of a current suite.

	* See BasicSuite.getNimbusHome() for what to do.

	* To discover how this all works and how to be flexible, look at the code (later!).



Adding a new IntelliJ IDEA module
---------------------------------

	Note that the .idea/ directory has been updated with a new group of modules, the
	"service-suites" group.  Each suite should be its own module (e.g. "basic") so that it can
	house its own Spring facet and in general to keep things organized cleanly:

		* One unique Java package that only relies on the "tests-common" module (and that
		  module's own dependencies).

		* One module directory per suite so that we can expand the module in the hierarchical
		  view and see the "home" and "src" directories... and then make them go away. 

	To create a new module for your suite, make it look like the others.  The "dependencies"
	tab should only need to rely on the other module "tests-common" which "exports" all the
	necessary 3rd party libraries and source code that are in play.

	Anything touching the .idea directory is reviewed closely before it can be merged into
	master, so don't worry too much...


