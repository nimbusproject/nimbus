The service API and implementation have no GT dependency or knowledge.  The
messaging bridges only compile to the service API, they have no knowledge of
what implementation you'd like to use.

So from "outside," this module puts everything together for a GT4.0.x
services deployment into one GAR package (the mode of installation into the
GT container).

This is the default set of choices.  Other choices can be put together easily.

To facilitate client-only installations, there are base GARs that must be
installed as well.  The schemas GAR(s) delivers the WSDL and XSD files.  The
stubs GAR(s) delivers the auto-generated Java code.  Both the client GAR and
this services GAR need those deployed to GLOBUS_LOCATION in order to run.
