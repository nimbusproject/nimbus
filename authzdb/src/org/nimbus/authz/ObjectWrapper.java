package org.nimbus.authz;

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
}
