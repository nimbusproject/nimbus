package org.nimbus.authz;

import java.sql.Timestamp;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: Jun 14, 2010
 * Time: 10:33:58 PM
 * <p/>
 * org.nimbus.authz
 */
public class ObjectWrapper
{
    private String name;
    private int id;
    private long size;
    private long timestamp;

    public void setName(String n)
    {
        this.name = n;
    }

    public long getSize()
    {
        return this.size;
    }

    public void setSize(long s)
    {
        this.size = s;
    }

    public String getName()
    {
        return this.name;
    }

    public void setId(int i)
    {
        this.id = i;
    }

    public int getId()
    {
        return this.id;
    }

    public void setTime(long tm)
    {
        this.timestamp = tm;
    }

    public long getTime()
    {
        return this.timestamp;
    }
}
