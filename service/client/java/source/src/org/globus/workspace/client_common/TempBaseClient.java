/*
 * Portions of this file Copyright 1999-2005 University of Chicago
 * Portions of this file Copyright 1999-2005 The University of Southern California.
 *
 * This file or a portion of this file is licensed under the
 * terms of the Globus Toolkit Public License, found at
 * http://www.globus.org/toolkit/download/license.html.
 * If you redistribute this file, with or without
 * modifications, you must include this notice in the file.
 */
package org.globus.workspace.client_common;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.rpc.Stub;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.axis.message.addressing.Address;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.message.addressing.ReferencePropertiesType;

import org.globus.workspace.client_core.utils.NimbusCredential;
import org.ietf.jgss.GSSCredential;
import org.xml.sax.InputSource;

import org.globus.axis.gsi.GSIConstants;
import org.globus.axis.util.Util;
import org.globus.gsi.CertUtil;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.security.authentication.Constants;
import org.globus.wsrf.impl.security.authentication.encryption.EncryptionCredentials;
import org.globus.wsrf.impl.security.authorization.HostAuthorization;
import org.globus.wsrf.impl.security.authorization.IdentityAuthorization;
import org.globus.wsrf.impl.security.authorization.NoAuthorization;
import org.globus.wsrf.impl.security.authorization.SelfAuthorization;


/**
 * This is a hacky workaround for problems with
 * org.globus.wsrf.client.BaseClient. It is a raw copy of that class, but with
 * the static Option members made into members
 */

// I feel dirty
abstract class TempBaseClient {

    public static final int COMMAND_LINE_ERROR = 1;
    public static final int APPLICATION_ERROR = 2;

    protected EndpointReferenceType endpoint;
    protected boolean debugMode;
    protected String customUsage;
    protected String helpFooter;
    protected String helpHeader;
    protected Options options = new Options();

    protected String mechanism;
    protected Object protection = Constants.SIGNATURE;
    protected Object delegation;
    protected Object authorization;
    protected Object anonymous;
    protected Integer contextLifetime;
    protected String msgActor;
    protected String convActor;
    protected String publicKeyFilename;
    protected String descriptorFile;

    static final String AUTHZ_DESC =
        "Sets authorization, can be 'self', 'host', 'none' or a string " +
        "specifying the expected identity of the remote party";

    static final String MECHANISM_DESC =
        "Sets authentication mechanism: 'msg' (for GSI Secure Message), or"
        + " 'conv' (for GSI Secure Conversation)";

    static final String PROTECTION_DESC =
        "Sets protection level, can be 'sig' (for signature) "
        + " can be 'enc' (for encryption)";

    static final String ANON_DESC =
        "Use anonymous authentication (requires either -m 'conv' or"
        + " transport (https) security)";

    static final String FILENAME_DESC =
        "A file with server's certificate used for encryption. "
        + "Used in the case of GSI Secure Message encryption";

    static final String CONTEXT_DESC =
        "Lifetime of context created for GSI Secure " +
        "Conversation (requires -m 'conv')";

    static final String MSG_ACTOR_DESC =
        "Sets actor name for GSI Secure Message";

    static final String CONV_ACTOR_DESC =
        "Sets actor name for GSI Secure Conversation";

    static final String DELEG_DESC =
        "Performs delegation. Can be 'limited' or 'full'. "
        + "(requires -m 'conv')";

    static final String DESCRIPTOR_DESC =
        "Sets client security descriptor. Overrides all other security " +
        "settings";

    private final Option HELP =
        OptionBuilder.withDescription("Displays help")
        .withLongOpt("help")
        .create("h");

    public final Option EPR_FILE =
        OptionBuilder.withArgName( "file" )
        .hasArg()
        .withDescription("Loads EPR from file")
        .withLongOpt("eprFile")
        .create("e");

    public final Option SERVICE_URL =
        OptionBuilder.withArgName( "url" )
        .hasArg()
        .withDescription("Service URL")
        .withLongOpt("service")
        .create("s");

    public final Option RESOURCE_KEY =
        OptionBuilder.withArgName( "name value" )
        .hasArgs(2)
        .withDescription("Resource Key")
        .withLongOpt("key")
        .create("k");

    public final Option DEBUG =
        OptionBuilder.withDescription("Enables debug mode")
        .withLongOpt("debug")
        .create("d");

    public final Option AUTHZ =
        OptionBuilder.withArgName("type").hasArg()
        .withDescription(AUTHZ_DESC)
        .withLongOpt("authorization").create("z");

    public final Option MECHANISM =
        OptionBuilder.withArgName("type").hasArg()
        .withDescription(MECHANISM_DESC)
        .withLongOpt("securityMech").create("m");

    public final Option ANON =
        OptionBuilder.withDescription(ANON_DESC).withLongOpt("anonymous")
        .create("a");

    public final Option PROTECTION =
        OptionBuilder.withArgName("type")
        .hasArg()
        .withDescription(PROTECTION_DESC)
        .withLongOpt("protection").create("p");

    public final Option PUB_KEY_FILE =
        OptionBuilder.withArgName("file")
        .hasArg()
        .withDescription(FILENAME_DESC)
        .withLongOpt("serverCertificate").hasOptionalArg().create("c");

    public final Option CONTEXT =
        OptionBuilder.withArgName("value").hasArg()
        .withDescription(CONTEXT_DESC)
        .withLongOpt("contextLifetime").create("l");

    public final Option MSG_ACTOR =
        OptionBuilder.withArgName("actor").hasArg()
        .withDescription(MSG_ACTOR_DESC)
        .withLongOpt("gsiSecMsgActor").create("x");

    public final Option CONV_ACTOR =
        OptionBuilder.withArgName("actor").hasArg()
        .withDescription(CONV_ACTOR_DESC)
        .withLongOpt("gsiSecConvActor").create("y");

    public final Option DELEG =
        OptionBuilder.withArgName("mode").hasArg()
        .withDescription(DELEG_DESC)
        .withLongOpt("delegation").create("g");

    public final Option DESCRIPTOR =
        OptionBuilder.withDescription(DESCRIPTOR_DESC).hasArg()
        .withArgName("file")
        .withLongOpt("descriptor").create("f");

    static {
        Util.registerTransport();
    }

    protected TempBaseClient() {

        options.addOption(HELP);
        options.addOption(EPR_FILE);
        options.addOption(SERVICE_URL);
        options.addOption(RESOURCE_KEY);
        options.addOption(DEBUG);

        // security options
        options.addOption(DESCRIPTOR);
        options.addOption(MECHANISM);
        options.addOption(ANON);
        options.addOption(PROTECTION);
        options.addOption(AUTHZ);
        options.addOption(CONTEXT);
        /*
        options.addOption(MSG_ACTOR);
        options.addOption(CONV_ACTOR);
        */
        options.addOption(DELEG);
        options.addOption(PUB_KEY_FILE);
    }

    public void setCustomUsage(String customUsage) {
        this.customUsage = customUsage;
    }

    public void setHelpFooter(String msg) {
        this.helpFooter = msg;
    }

    public void setHelpHeader(String msg) {
        this.helpHeader = msg;
    }

    public void displayUsage() {
        String usage = "java " + getClass().getName() +
                       " [-h] [-s url [-k name value] | -e file] ";

        usage = (this.customUsage == null) ? usage : usage + this.customUsage;

        String header = (this.helpHeader == null) ?
                        "Options:" : this.helpHeader;
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(usage, header, options, null, false);
        if (this.helpFooter != null) {
            System.out.println(this.helpFooter);
        }
    }

    protected CommandLine parse(String [] args) throws Exception {
        return parse(args, null);
    }

    protected CommandLine parse(String [] args, Properties defaultOptions)
        throws Exception {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args, defaultOptions);

        if (defaultOptions != null) {
            Iterator iter = defaultOptions.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                Option opt = options.getOption((String)entry.getKey());
                if (opt != null) {
                    String desc = opt.getDescription();
                    desc += " (Default '" + entry.getValue() + "')";
                    opt.setDescription(desc);
                }
            }
        }

        if (line.hasOption("h")) {
            displayUsage();
            System.exit(0);
        }

        if (line.hasOption("e")) {
            if (line.hasOption("k")) {
                throw new ParseException("-e and -k arguments are exclusive");
            }
            if (line.hasOption("s")) {
                throw new ParseException("-e and -s arguments are exclusive");
            }

            FileInputStream in = null;
            try {
                in = new FileInputStream(line.getOptionValue("e"));
                this.endpoint =
                (EndpointReferenceType)ObjectDeserializer.deserialize(
                    new InputSource(in),
                    EndpointReferenceType.class);
            } finally {
                if (in != null) {
                    try { in.close(); } catch (Exception e) {}
                }
            }
        } else if (line.hasOption("s")) {
            this.endpoint = new EndpointReferenceType();
            this.endpoint.setAddress(new Address(line.getOptionValue("s")));
        } else {
            throw new ParseException("-s or -e argument is required");
        }

        if (line.hasOption("k")) {
            String [] values = line.getOptionValues("k");
            if (values.length != 2) {
                throw new ParseException("-k requires two arguments");
            }
            QName keyName = QName.valueOf(values[0]);
            ReferencePropertiesType props = new ReferencePropertiesType();
            SimpleResourceKey key = new SimpleResourceKey(keyName, values[1]);
            props.add(key.toSOAPElement());
            this.endpoint.setProperties(props);
        }

        this.debugMode = line.hasOption("d");

        // Security mechanism
        if (line.hasOption("m")) {
            String value = line.getOptionValue("m");
            if (value != null) {
                if (value.equals("msg")) {
                    this.mechanism = Constants.GSI_SEC_MSG;
                } else if (value.equals("conv")) {
                    this.mechanism = Constants.GSI_SEC_CONV;
                } else {
                    throw new ParseException(
                        "Unsupported security mechanism: " +  value);
                }
            }
        }

        // Protection
        if (line.hasOption("p")) {
            String value = line.getOptionValue("p");
            if (value != null) {
                if (value.equals("sig")) {
                    this.protection = Constants.SIGNATURE;
                } else if (value.equals("enc")) {
                    this.protection = Constants.ENCRYPTION;
                } else {
                    throw new ParseException("Unsupported protection mode: " +
                                             value);
                }
            }
        }


        // Delegation
        if (line.hasOption("g")) {
            String value = line.getOptionValue("g");
            if (value != null) {
                if (value.equals("limited")) {
                    this.delegation = GSIConstants.GSI_MODE_LIMITED_DELEG;
                } else if (value.equals("full")) {
                    this.delegation = GSIConstants.GSI_MODE_FULL_DELEG;
                } else {
                    throw new ParseException("Unsupported delegation mode: " +
                                             value);
                }
            }
        }

        // Authz
        if (line.hasOption("z")) {
            String value = line.getOptionValue("z");
            if (value != null) {
                if (value.equals("self")) {
                    this.authorization = SelfAuthorization.getInstance();
                } else if (value.equals("host")) {
                    this.authorization = HostAuthorization.getInstance();
                } else if (value.equals("none")) {
                    this.authorization = NoAuthorization.getInstance();
                } else if (authorization == null) {
                    this.authorization = new IdentityAuthorization(value);
                }
            }
        }

        // Anonymous
        if (line.hasOption("a")) {
            this.anonymous = Boolean.TRUE;
        }

        // context lifetime
        if (line.hasOption("l")) {
            String value = line.getOptionValue("l");
            if (value != null)
                this.contextLifetime = new Integer(value);
        }

        // msg actor
        if (line.hasOption("x")) {
            String value = line.getOptionValue("x");
            this.msgActor = value;
        }

        // conv actor
        if (line.hasOption("y")) {
            String value = line.getOptionValue("y");
            this.convActor = value;
        }

        // Server's public key
        if (line.hasOption("c")) {
            String value = line.getOptionValue("c");
            this.publicKeyFilename = value;
        }


        if (line.hasOption("f")) {
            String value = line.getOptionValue("f");
            this.descriptorFile = value;
        }

        return line;
    }

    public void setOptions(Stub stub) throws Exception {

        if (this.descriptorFile != null) {
            stub._setProperty(Constants.CLIENT_DESCRIPTOR_FILE,
                              this.descriptorFile);
            return;
        }

        if (this.protection != null) {
            // this means if both transport security and message security
            // are enabled both will get the same protection
            if (this.endpoint.getAddress().getScheme().equals("https")) {
                stub._setProperty(GSIConstants.GSI_TRANSPORT,
                                  this.protection);
            }
            if (this.mechanism != null) {
                stub._setProperty(this.mechanism,
                                  this.protection);
            }
        }

        if (this.convActor != null) {
            stub._setProperty("gssActor", this.convActor);
        }

        if (this.delegation != null) {
            stub._setProperty(GSIConstants.GSI_MODE, this.delegation);
        }

        if (this.authorization != null) {
            stub._setProperty(Constants.AUTHORIZATION, this.authorization);
        }

        if (this.anonymous != null) {
            stub._setProperty(Constants.GSI_ANONYMOUS, this.anonymous);
        }

        if (this.msgActor != null) {
            stub._setProperty("x509Actor", this.msgActor);
        }

        if ((Constants.GSI_SEC_MSG.equals(this.mechanism))  &&
            (Constants.ENCRYPTION.equals(this.protection))) {
            Subject subject = new Subject();
            X509Certificate serverCert =
                CertUtil.loadCertificate(publicKeyFilename);
            EncryptionCredentials encryptionCreds =
                new EncryptionCredentials(new X509Certificate[]
                    { serverCert });
            subject.getPublicCredentials().add(encryptionCreds);
            stub._setProperty(Constants.PEER_SUBJECT, subject);
        }

        if (this.contextLifetime != null) {
            stub._setProperty(Constants.CONTEXT_LIFETIME,
                              this.contextLifetime);
        }

        final GSSCredential usercred = NimbusCredential.getGSSCredential();
        if (usercred != null) {
            stub._setProperty(GSIConstants.GSI_CREDENTIALS, usercred);
        }
    }

    public EndpointReferenceType getEPR() {
        return this.endpoint;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }
}
