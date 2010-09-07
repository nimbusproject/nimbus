package org.globus.workspace.testing;

import org.apache.commons.logging.Log;

/**
 * The NimbusTestBase logger should be used only after suite setup, this class prevents
 * NPEs beforehand.
 */
public class FakeLog implements Log {

    public boolean isDebugEnabled() {return false;}
    public boolean isErrorEnabled() {return false;}
    public boolean isFatalEnabled() {return false;}
    public boolean isInfoEnabled() {return false;}
    public boolean isTraceEnabled() {return false;}
    public boolean isWarnEnabled() {return false;}
    public void trace(Object o) {dontuse();}
    public void trace(Object o, Throwable throwable) {dontuse();}
    public void debug(Object o) {dontuse();}
    public void debug(Object o, Throwable throwable) {dontuse();}
    public void info(Object o) {dontuse();}
    public void info(Object o, Throwable throwable) {dontuse();}
    public void warn(Object o) {dontuse();}
    public void warn(Object o, Throwable throwable) {dontuse();}
    public void error(Object o) {dontuse();}
    public void error(Object o, Throwable throwable) {dontuse();}
    public void fatal(Object o) {dontuse();}
    public void fatal(Object o, Throwable throwable) {dontuse();}

    private static void dontuse() {
        System.err.println("\n* Don't use this logger.\n");
    }
}
