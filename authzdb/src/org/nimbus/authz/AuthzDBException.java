package org.nimbus.authz;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: Jun 14, 2010
 * Time: 1:47:33 PM
 * <p/>
 * org.nimbus.authz
 */
public class AuthzDBException extends Exception
{
    public AuthzDBException()
    {
        super();
    }

    public AuthzDBException(String message)
    {
        super(message);
    }

    public AuthzDBException(String message, Exception e)
    {
        super(message, e);
    }

    public AuthzDBException(String message, Throwable e)
    {
        super(message, e);
    }

    public AuthzDBException(Exception e)
    {
        super("", e);
    }
}
