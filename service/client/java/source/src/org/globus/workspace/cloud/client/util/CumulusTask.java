/*
 * Copyright 1999-2008 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.globus.workspace.cloud.client.util;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.cloud.client.AllArgs;
import org.globus.workspace.common.print.Print;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.ObjectUtils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

class CumulusParameterConvert implements org.apache.http.params.HttpParams
{
    private HttpParams otherPs;
    CumulusParameterConvert(HttpParams otherPs)
    {
        this.otherPs = otherPs;
    }

    public org.apache.http.params.HttpParams 	copy()
    {
        return new CumulusParameterConvert(this.otherPs);
    }

    public boolean 	getBooleanParameter(String name, boolean defaultValue)
    {
        return otherPs.getBooleanParameter(name, defaultValue);
    }

    public double 	getDoubleParameter(String name, double defaultValue)
    {
        return otherPs.getDoubleParameter(name, defaultValue);
    }

    public int 	getIntParameter(String name, int defaultValue)
    {
        return otherPs.getIntParameter(name, defaultValue);
    }

    public long 	getLongParameter(String name, long defaultValue)
    {
        return otherPs.getLongParameter(name, defaultValue);
    }

    public Object 	getParameter(String name)
    {
        return otherPs.getParameter(name);
    }

    public boolean 	isParameterFalse(String name)
    {
        return otherPs.isParameterFalse(name);
    }

    public boolean 	isParameterTrue(String name)
    {
        return otherPs.isParameterTrue(name);
    }

    public boolean 	removeParameter(String name)
    {
        boolean rc = otherPs.isParameterSet(name);
        if(rc)
        {
            otherPs.setParameter(name, null);
        }
        return rc;
    }

    public org.apache.http.params.HttpParams 	setBooleanParameter(String name, boolean value)
    {
        this.otherPs.setBooleanParameter(name, value);
        return this;
    }

    public org.apache.http.params.HttpParams 	setDoubleParameter(String name, double value)
    {
        this.otherPs.setDoubleParameter(name, value);
        return this;
    }

    public org.apache.http.params.HttpParams 	setIntParameter(String name, int value)
    {
        this.otherPs.setIntParameter(name, value);
        return this;
    }

    public org.apache.http.params.HttpParams 	setLongParameter(String name, long value)
    {
        this.otherPs.setLongParameter(name, value);
        return this;
    }

    public org.apache.http.params.HttpParams 	setParameter(String name, Object value)
    {
        this.otherPs.setParameter(name, value);
        return this;
    }

}

class CumulusAlwaysTrustStrategy implements TrustStrategy
{
    public boolean isTrusted(X509Certificate[] chain, String authType)
    {
        return true;
    }
}


class CumulusProtocolSocketFactory implements ProtocolSocketFactory
{
    protected  SSLSocketFactory psf;

    CumulusProtocolSocketFactory()
            throws Exception
    {
         //TrustStrategy ts = new TrustSelfSignedStrategy();
         TrustStrategy ts = new CumulusAlwaysTrustStrategy();
         psf = new SSLSocketFactory(ts);
    }

    public Socket createSocket(String host,
                    int port,
                    InetAddress localAddress,
                    int localPort)         
                    throws IOException,
        UnknownHostException
    {
        Socket s = psf.createSocket();        
        InetSocketAddress remote = new InetSocketAddress(host, port);
        InetSocketAddress local = new InetSocketAddress(localAddress, localPort);
        org.apache.http.params.HttpParams hp = new org.apache.http.params.BasicHttpParams();
        s = psf.connectSocket(s, remote, local, hp);
        return s;
    }


    public Socket createSocket(String host,
                    int port,
                    InetAddress localAddress,
                    int localPort,
                    HttpConnectionParams params)
                    throws IOException,
                           UnknownHostException,
            ConnectTimeoutException
    {
        Socket s = psf.createSocket();
        InetSocketAddress remote = new InetSocketAddress(host, port);
        InetSocketAddress local = new InetSocketAddress(0);
        org.apache.http.params.HttpParams hp = new CumulusParameterConvert(params);
        s = psf.connectSocket(s, remote, local, hp);
        return s; 
    }


    public Socket createSocket(String host,
                    int port)
                    throws IOException,
                           UnknownHostException
    {
        Socket s = psf.createSocket();
        InetSocketAddress local = new InetSocketAddress(0);
        InetSocketAddress remote = new InetSocketAddress(host, port);
        s = psf.connectSocket(s, remote, local, null);
        return s;         
    }

    /* The parent class overrides equals and hashCode to ensure that
    all instances are considered the same. Since we are using an
    overridden class, this breaks connection pooling within httpclient
    as no connections are ever equal. So we override them ourselves.

    From: http://hc.apache.org/httpclient-3.x/apidocs/org/apache/commons/httpclient/protocol/ProtocolSocketFactory.html

    "Both Object.equals() and Object.hashCode() should be overridden
    appropriately. Protocol socket factories are used to uniquely identify
    Protocols and HostConfigurations, and equals() and hashCode() are required
    for the correct operation of some connection managers."
    */

    public boolean equals(Object obj)
    {
        return ((obj != null) && obj.getClass().equals(CumulusProtocolSocketFactory.class));
    }

    public int hashCode()
    {
        return CumulusProtocolSocketFactory.class.hashCode();
    }
}

class CumulusInputStream
    extends InputStream
{
    private InputStream                 is;
    private PrintStream                 pr;
    private CloudProgressPrinter        progress;
    private long                        where = 0;
    private long                        marked = 0;

    public CumulusInputStream(
        long                            len,
        PrintStream                     pr,
        InputStream                     is)
    {
        super();
        this.is = is;
        this.pr = pr;   

        progress = new CloudProgressPrinter(this.pr, len);
    }
 
    public int available()
        throws java.io.IOException
    {
        return this.is.available();
    }
    
    public void   close()
        throws java.io.IOException
    {
        this.is.close();
        this.progress.flush();
        this.progress.print_done();
    }

    public void   mark(int readlimit)
    {
        marked = where;
        this.is.mark(readlimit);
    }

    public boolean    markSupported()
    {
        return this.is.markSupported();
    }

    private void updatePosition(int len)
    {
        where += len;
        progress.updateBytesTransferred(len);
    }

    public int   read()
        throws java.io.IOException
    {
        int len;
        len = this.is.read();
        updatePosition(len);
        return len;
    }
 
    public int    read(byte[] b)
        throws java.io.IOException
    {
        int len;
        len = this.is.read(b);
        updatePosition(len);
        return len;
    }

    public int    read(byte[] b, int off, int len)
        throws java.io.IOException
    {
        int lenrc;
        lenrc = this.is.read(b, off, len);
        updatePosition(lenrc);
        return lenrc;
    }

    public void   reset()
        throws java.io.IOException
    {
        long diff = marked - where;
        this.is.reset();
        updatePosition((int)diff);
    }
 
    public long   skip(long n)
        throws java.io.IOException
    {
        return this.is.skip(n);
    }
}


class CloudProgressPrinter
    extends BytesProgressWatcher
{
    private PrintStream                 pr;
    private int                         colCount = 80;
    private Date                        nextUpdate = null;
    private boolean                     hit100 = false;
    private boolean                     noprint = false;

    public CloudProgressPrinter(
        PrintStream                     pr,
        long                            len)
    {
        super(len);
        this.pr = pr;
    }

    public CloudProgressPrinter(
        PrintStream                     pr,
        long                            len,
        boolean                         noprint)
    {
        super(len);
        this.pr = pr;
        this.noprint = noprint;        
    }

    // return a string with the long value properly suffixed 
    protected String prettyCount(long tb, int maxLen)
    {
        int                             ndx = 0;
        String suffix[] = {" B", "KB", "MB", "GB"};

        if(tb < 0)
        {
            return "Unknown";
        }

        float tbF = (float) tb;
        while(tbF > 10240.0f && ndx < suffix.length - 1)
        {
            ndx++;
            tbF = tbF / 1024.0f;
        }
        tb = (int)tbF;

        String rc =  new Long(tb).toString();

        if(rc.length() > maxLen)
        {
            int endNdx = maxLen - 3;
            rc = rc.substring(0, endNdx) + "...";
        }
        else
        {
            // some silliness to get to 2 decimals
            if(rc.length() < maxLen - 2)
            {
                tb = (int)(tbF * 100.0f);
                tbF = (float)tb / 100.0f;
                rc = new Float(tbF).toString();
            }
            int spaceCount = maxLen - rc.length();
            for(int i = 0; i < spaceCount; i++)
            {
                rc = " " + rc;
            }
        }
        rc = rc + suffix[ndx];

        return rc;
    }


    protected String makeBar(long sofar, long total)
    {
        String byteString = this.prettyCount(sofar, 6);
        String                          doneCh = "X";
        String                          notDoneCh = ".";

        // there are 9 pad characters: <sp>[]<sp>PPP%<sp>
        int pad = 9;
        int pgLen = this.colCount - byteString.length() - pad;

        // this will be rounded down, but who cares?
        int percent = (int)((sofar * 100) / total);
        int xCount = (percent * pgLen) / 100;


        String bar = byteString + " [";
        for(int i = 0; i < pgLen; i++)
        {
            if(i < xCount)
            {
                bar = bar + doneCh;
            }
            else
            {
                bar = bar + notDoneCh;
            }
        }
        bar = bar + "] " + percent + "% ";


        if(sofar  == total)
        {
            this.hit100 = true;           
        }

        return bar;
    }       

    public void updateBytesTransferred(
        long                            byteCount)
    {
        super.updateBytesTransferred(byteCount);
        this.print_bar();
    }

    public void print_bar()
    {
        Calendar now = 	Calendar.getInstance();
        Date nowDt = now.getTime();

        if (this.nextUpdate != null && nowDt.before(this.nextUpdate))
        {
            return;
        }
        this.nextUpdate = new Date(nowDt.getTime() + 1000);
        flush();
    }

    public void print_done()
    {
        long total = getBytesToTransfer();
        String bar = this.makeBar(total, total);
        System.out.print("\r");
        System.out.print(bar);
        System.out.flush();
    }

    public void flush()
    {
        if(this.hit100)
        {
            return;
        }
        if(this.noprint)
        {
            return;
        }
        long total = getBytesToTransfer();
           
        long sent = getBytesTransferred();

        if(this.pr == null)
        {
            return;
        }

        String bar = this.makeBar(sent, total);
        System.out.print("\r");
        System.out.print(bar);
        System.out.flush();
    }
}

public class CumulusTask
    implements Callable
{
    public static final int DELETE_TASK = 1;
    public static final int UPLOAD_TASK = 2;
    public static final int DOWNLOAD_TASK = 3;
    public static final int LIST_TASK = 4;

    private int task = -1;

    private String                      localfile;
    private String                      vmName;
    private PrintStream                 info;
    private PrintStream                 debug;
    private AllArgs                     args;
    private Print                       print;
    private String                      useHttps;
    private boolean                     allowSelfSigned;


    public CumulusTask(
        AllArgs                         args,
        Print                           pr,
        String                          useHttps,
        boolean                         allowSelfSigned)
    {
        this.args = args;
        this.print = pr;
        this.useHttps = useHttps;
        this.allowSelfSigned = allowSelfSigned;
    }

    public void setTask(
        int                             t)
    {
        this.task = t;
    }

    private AWSCredentials getAwsCredentail()
    {
        String awsAccessKey = this.args.getXferS3ID();
        String awsSecretKey = this.args.getXferS3Key();
               
        AWSCredentials awsCredentials = 
            new AWSCredentials(awsAccessKey, awsSecretKey);

        return awsCredentials;
    }

    private String makeKey(
        String                          vmName,
        String                          ID)
    {
        if(this.args.getCommonVMSet())
        {
            ID = "common";
        }
        String baseKey = this.args.getXferS3BaseKey();
        if(ID == null)
        {
            ID = this.args.getXferCanonicalID();
        }

        return baseKey + "/" + ID + "/" + vmName;
    }

    private String stripKey(
        String                          key)
            throws ExecutionProblem
    {
        int ndx = key.lastIndexOf("/"); 
        if (ndx < 0)
        {
            this.print.debugln("\nCumulus returned a bad VM key " + 
                key);
            return null;
        }
        return key.substring(ndx+1);
    }

    private void makeBucket(
        S3Service                       s3Service,
        PrintStream                     pr,
        String                          bucketName)
    {
        try
        {
            s3Service.createBucket(bucketName);
        }
        catch (Exception ex)
        {
            if(pr != null)
            {
                pr.println(ex.toString());
            }
        }
    }

    public void uploadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        S3Service s3Service = null;
        try
        {
            String awsAccessKey = this.args.getXferS3ID();
            s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();

            PrintStream pr = null;
            if (info != null) {
                pr = info;
            } else if (debug != null) {
                pr = debug;
            }
            String key = this.makeKey(vmName, null);

            File file = new File(localfile);

            if (pr != null) {               
                pr.println("\nTransferring");
                pr.println("  - Source: " + file.getName());
                String destUrlString = "cumulus://" + baseBucketName + "/" + key;
                pr.println("  - Destination: " + destUrlString);
                pr.println();               
                pr.println("Preparing the file for transfer:");
            } 

            CloudProgressPrinter progressWatcher =
                new CloudProgressPrinter(pr, file.length());
            S3Object s3Object = ObjectUtils.createObjectForUpload(
                key, file, null, false, progressWatcher);
            progressWatcher.flush();
            s3Object.setContentType(Mimetypes.MIMETYPE_OCTET_STREAM);
            if (pr != null) {
                pr.println("\n\nTransferring the file:");
            }
            CumulusInputStream cis = new CumulusInputStream(
                file.length(), pr, s3Object.getDataInputStream());
            s3Object.setDataInputStream(cis);
            s3Service.putObject(baseBucketName, s3Object);
            progressWatcher.flush();
            s3Object.closeDataInputStream();
            cis.close();

            if (pr != null) {
                pr.println("");
                pr.println("");
                pr.println("Done.");
            }
        }
        catch(S3ServiceException s3ex)
        {
            String msg = s3ex.getS3ErrorMessage() + " cause: " + s3ex.getCause();
            throw new ExecutionProblem(msg, s3ex);
        }
        catch(Exception ex1)
        {
            String msg = ex1.toString() + " cause: " + ex1.getCause();
            throw new ExecutionProblem(msg, ex1);
        }
        finally
        {
            this.shutdownService(s3Service);
        }
    }

    public void downloadVM(
        String                          localfile,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        S3Service s3Service = null;
        try
        {
            String baseBucketName = this.args.getS3Bucket();
            String key = this.makeKey(vmName, null);
            File file = new File(localfile);

            PrintStream pr = null;
            if (info != null) {
                pr = info;
            } else if (debug != null) {
                pr = debug;
            }

            if (pr != null) {
                String srcUrlString = "cumulus://" + baseBucketName + "/" + key;
                pr.println("\nTransferring");
                pr.println("  - Source: " + srcUrlString);

                pr.println("  - Destination: " + file.getAbsolutePath());
                pr.println();

            }

            s3Service = this.getService();
            S3Object s3Object = s3Service.getObject(baseBucketName, key);

            BytesProgressWatcher progressWatcher = 
                new CloudProgressPrinter(pr, s3Object.getContentLength());
            byte b [] = new byte[1024*64];
            InputStream dis = s3Object.getDataInputStream();
            FileOutputStream fos = new FileOutputStream(file);
            int cnt = dis.read(b);
            while(cnt != -1)
            {
                fos.write(b, 0, cnt); 
                progressWatcher.updateBytesTransferred((long)cnt);
                cnt = dis.read(b);
            }
            fos.close();
        }
        catch(Exception s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }
        finally
        {
            this.shutdownService(s3Service);
        }

    }

    public void setLocalfile(
        String                          localfile)
    {
        this.localfile = localfile;
    }

    public void setVmname(
        String                          vmName)
    {
        this.vmName = vmName;
    }

    public void setInfo(
        PrintStream                     info)
    {
        this.info = info;
    }

    public void setDebug(
        PrintStream                     debug)
    {
        this.debug = debug;
    }

    public void deleteVM(
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
            throws ExecutionProblem
    {
        S3Service s3Service = null;
        try
        {
            s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();
            String keyName = this.makeKey(vmName, null);

            try
            {
                s3Service.deleteObject(baseBucketName, keyName);
            }
            catch(S3ServiceException s3ex)
            {                
                if(s3ex.getResponseCode() == 404)
                {
                    keyName = this.makeKey(vmName, "common");
                    s3Service.deleteObject(baseBucketName, keyName);
                }
                else
                {
                    throw new ExecutionProblem(s3ex.toString());
                }
            }

        }
        catch(S3ServiceException s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }
        finally
        {
            this.shutdownService(s3Service);
        }

    }

    private S3Service getService()
        throws S3ServiceException
    {
        String host = this.args.getXferHostPort();
        int ndx = host.lastIndexOf(":");
        int port = 80;
        String portS = "80";
        String httpsPortS = "443";
        int httpsPort = 443;

        if(ndx > 0)
        {
            portS = host.substring(ndx+1);
            httpsPortS = portS;
            port = new Integer(portS).intValue();
            httpsPort = new Integer(httpsPortS).intValue();
            host = host.substring(0, ndx);
        }
        
        Jets3tProperties j3p = new Jets3tProperties();

        j3p.setProperty("s3service.s3-endpoint-http-port", portS);
        j3p.setProperty("s3service.s3-endpoint-https-port", httpsPortS);
        j3p.setProperty("s3service.disable-dns-buckets", "true");
        j3p.setProperty("s3service.s3-endpoint", host);   
        j3p.setProperty("s3service.https-only", this.useHttps);
        j3p.setProperty("storage-service.internal-error-retry-max", "0");
        j3p.setProperty("httpclient.socket-timeout-ms", "0");

        HostConfiguration hc = new HostConfiguration();
        if(allowSelfSigned && this.useHttps.equalsIgnoreCase("true"))
        {
            // magic needed for jets3t to work with self signed cert.
            try
            {
                Protocol easyhttps = new Protocol("https", new CumulusProtocolSocketFactory(), 443);
                Protocol.registerProtocol("https", easyhttps);

                hc.setHost(host, httpsPort, easyhttps);
            }
            catch(Exception ex)
            {
                throw new S3ServiceException("Could not make the self signed handler " + ex.toString(), ex);
            }
        }
        AWSCredentials awsCredentials = this.getAwsCredentail();
        S3Service s3Service = new RestS3Service(
            awsCredentials,
            "cloud-client",
            null,
            j3p,
            hc);

        return s3Service;                                           
    }

    private void shutdownService(S3Service service)
    {
        // best effort cleanup
        try {
            if (service != null)
            {
                service.shutdown();
            }
        } catch (S3ServiceException ignored) {
        }
    }

    private boolean keyExists(
        S3Service                   s3Service,
        String                      baseBucketName,
        String                      keyName)
            throws S3ServiceException
    {
        boolean exists = false;
        try
        {
            exists = s3Service.isObjectInBucket(baseBucketName, keyName);
        }
        catch(S3ServiceException s3ex)
        {
            if(s3ex.getResponseCode() == 404)
            {
                exists = false;
            }
            else
            {
                throw s3ex;
            }
        }
        return exists;
    }

    public String getImagePath(
            String                      vmName)
                throws ExecutionProblem
    {
        String baseBucketName;
        String keyNameOwner;
        S3Service s3Service = null;
        try
        {
            s3Service = this.getService();
            int ndx = vmName.indexOf("cumulus://");
            if(ndx >= 0)
            {
                return vmName;
            }
            else
            {
                baseBucketName = this.args.getS3Bucket();
                // first check to see if the owner has the image
                keyNameOwner = this.makeKey(vmName, null);
            
                boolean exists = this.keyExists(s3Service, baseBucketName, keyNameOwner);
                if(exists)
                {
                    return keyNameOwner;
                }
                // if not found check to see if the image is in the common space
                String keyNameCommon = this.makeKey(vmName, "common");
                exists = this.keyExists(s3Service, baseBucketName, keyNameCommon);
                if(exists)
                {
                    return keyNameCommon;
                }
                // if the image still is not found it may be a new image, in which case return the
                // owner name
                return keyNameOwner;
            }
         }
        catch(S3ServiceException s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }
        finally
        {
            this.shutdownService(s3Service);
        }
    }

    public FileListing[] listFiles(
        PrintStream                     info,
        PrintStream                     err,
        PrintStream                     debug) 
              throws ExecutionProblem 
    {
        S3Service s3Service = null;
        try
        {
            s3Service = this.getService();

            String baseBucketName = this.args.getS3Bucket();
            String keyName = this.makeKey("", null);

            ArrayList files = new ArrayList();
            // first get all of this users objects
            S3Object[] usersVMs = s3Service.listObjects(baseBucketName, keyName, "", 1000);
            s3ObjToFileList(files, usersVMs, true);
            S3Object[] VMs = s3Service.listObjects(baseBucketName, this.makeKey("", "common"), "", 1000);
            s3ObjToFileList(files, VMs, false);

            return (FileListing[]) files.toArray(new FileListing[files.size()]);
        }
        catch(S3ServiceException s3ex)
        {
            throw new ExecutionProblem(s3ex.toString());
        }
        finally
        {
            this.shutdownService(s3Service);
        }
    }

    private void s3ObjToFileList(
        ArrayList                       files,
        S3Object []                     s3Objs,
        boolean                         rw)
          throws ExecutionProblem 
    {
        Calendar cal = Calendar.getInstance();

        for(int i = 0; i < s3Objs.length; i++)
        {
            String name = s3Objs[i].getKey();
            name = this.stripKey(name);
            if(name != null)
            {
                Date dt = s3Objs[i].getLastModifiedDate();
                cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                cal.setTimeInMillis(dt.getTime());

                FileListing fl = new FileListing();
            
                fl.setName(name);
                fl.setSize(s3Objs[i].getContentLength());

                fl.setDate(convertDate(cal));
                fl.setTime(convertTime(cal));
                fl.setDirectory(false);
                fl.setReadWrite(rw);
                fl.setOwner(s3Objs[i].getOwner().getDisplayName());

                files.add(fl);
            }
        }
    }

    private String convertDate(
        Calendar                        cal)
    {
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);
        int y = cal.get(Calendar.YEAR);

        String rc = getMonthStr(m) + " " + 
            new Integer(d).toString() + " " + new Integer(y).toString();

        return rc;
    }

    private String getMonthStr(int month) {
        switch (month) {
            case 1: return "Jan";
            case 2: return "Feb";
            case 3: return "Mar";
            case 4: return "Apr";
            case 5: return "May";
            case 6: return "Jun";
            case 7: return "Jul";
            case 8: return "Aug";
            case 9: return "Sep";
            case 10: return "Oct";
            case 11: return "Nov";
            case 12: return "Dec";
            default: return "???";
        }
    }

    private String convertTime(
        Calendar                        cal)
    {
        int hr = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        String mStr = new Integer(m).toString();
        if(mStr.length() != 2)
        {
            mStr = "0" + mStr;
        }

        return new Integer(hr).toString() + ":" + mStr;
    }

    public void chmod(
        String                          ownerId,
        String                          permissions,
        String                          vmName,
        PrintStream                     info,
        PrintStream                     debug)
              throws ExecutionProblem
    {
    }

    public String getRemoteUrl(
        String                          fname)
    {
        return "";
    }

    public Object call() throws Exception
    {
        switch(this.task)
        {
            case UPLOAD_TASK:
                this.uploadVM(
                    this.localfile,
                    this.vmName,
                    this.info,
                    this.debug);
                break;

            case DOWNLOAD_TASK:
                this.downloadVM(
                    this.localfile,
                    this.vmName,
                    this.info,
                    this.debug);
                break;

            case LIST_TASK:
            case DELETE_TASK:
        }
        return null;
    }

}
