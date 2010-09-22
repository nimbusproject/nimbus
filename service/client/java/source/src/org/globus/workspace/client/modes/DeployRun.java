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

package org.globus.workspace.client.modes;

import org.globus.workspace.client_common.CommonStrings;

import org.globus.workspace.client_core.ParameterProblem;
import org.globus.workspace.client_core.ExecutionProblem;
import org.globus.workspace.client_core.ExitNow;
import org.globus.workspace.client_core.utils.FileUtils;
import org.globus.workspace.client_core.utils.WSUtils;
import org.globus.workspace.client_core.utils.StringUtils;
import org.globus.workspace.client_core.utils.ScheduleUtils;
import org.globus.workspace.client_core.utils.NetUtils;
import org.globus.workspace.client_core.repr.Workspace;
import org.globus.workspace.client_core.repr.Group;
import org.globus.workspace.client_core.repr.Schedule;
import org.globus.workspace.client_core.repr.Networking;
import org.globus.workspace.client_core.repr.Nic;
import org.globus.workspace.client_core.print.PrCodes;
import org.globus.workspace.common.print.Print;
import org.globus.workspace.client_core.actions.Create;
import org.globus.workspace.client_core.actions.Create_Group;
import org.globus.workspace.client_core.actions.Delegate;

import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceResourceRequestDeniedFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceSchedulingFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceMetadataFault;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceCreationFault;
import org.nimbustools.messaging.gt4_0.generated.types.OptionalParameters_Type;
import org.nimbustools.messaging.gt4_0.generated.types.WorkspaceContextualizationFault;
import org.nimbustools.messaging.gt4_0.generated.ensemble.WorkspaceEnsembleFault;
import org.nimbustools.messaging.gt4_0.common.CommonUtil;

import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;
import org.globus.wsrf.utils.AddressingUtils;
import org.globus.wsrf.encoding.SerializationException;
import org.globus.delegation.DelegationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.oasis.wsrf.faults.BaseFaultType;

import javax.xml.namespace.QName;
import java.security.cert.X509Certificate;
import java.text.NumberFormat;
import java.io.File;
import java.io.IOException;

public class DeployRun {

    
    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------

    private static final Log logger =
            LogFactory.getLog(DeployRun.class.getName());
    

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    private final Deploy d;
    private final Print pr;

    private final Create eitherCreate;
    private final Create singleCreate;
    private final Create_Group groupCreate;

    private boolean wasBestEffort;


    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    DeployRun(Deploy deploy, Print print) {

        if (deploy == null) {
            throw new IllegalArgumentException("deploy may not be null");
        }

        if (print == null) {
            throw new IllegalArgumentException("print may not be null");
        }
        
        this.d = deploy;
        this.pr = print;

        if (this.d.isGroupRequest) {
            this.singleCreate = null;
            this.groupCreate = new Create_Group(this.d.factoryEPR,
                                                this.d.stubConfigurator,
                                                this.pr);
            this.eitherCreate = this.groupCreate;
        } else {
            this.singleCreate = new Create(this.d.factoryEPR,
                                           this.d.stubConfigurator,
                                           this.pr);
            this.groupCreate = null;
            this.eitherCreate = this.singleCreate;
        }
    }


    // -------------------------------------------------------------------------
    // ENTRY
    // -------------------------------------------------------------------------
    
    void run() throws ParameterProblem, ExecutionProblem, ExitNow {

        this.populateCreate(this.eitherCreate);

        this.eitherCreate.validateAll();

        // dryrun cutoff in this:
        this.delegateIfNecessary();

        // delegate may have changed opt params
        this.eitherCreate.validateOptionalParameters();

        if (this.d.dryrun) {

            if (this.pr.enabled()) {
                final String msg = "Dryrun, done.";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        if (this.d.subscribeAfterDeployment && this.d.subscribeLaunch == null) {
            throw new IllegalStateException("internal inconsistency: " +
                    "subscribe after deployment needed but no launch object " +
                    "was prepared");
        }

        if (this.singleCreate != null) {

            final Workspace workspace = this.single();
            if (this.d.shortName != null) {
                workspace.setDisplayName(this.d.shortName);
            }
            if (this.pr.enabled() && this.pr.useThis()) {
                this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
            }
            if (this.d.subscribeAfterDeployment) {
                final Workspace[] warray = {workspace};
                this.subscribe(warray);
            }

        } else {

            final Group group = this.group();

            final NumberFormat format = NumberFormat.getInstance();
            format.setMinimumIntegerDigits(this.groupCreate.getSettings().
                                                getGroupSuffixMinCharacters());

            // only override nameToPrint if no short, display name was given
            if (this.d.shortName == null) {
                this.d.nameToPrint = group.getGroupID();
                if (this.d.subscribeLaunch != null) {
                    this.d.subscribeLaunch.setName(this.d.nameToPrint);
                }
            }

            // optional write of each EPR is triggered by groupBaseName
            final String baseName = this.d.groupBaseName;
            final Workspace[] warray = group.getWorkspaces();
            if (baseName != null) {
                
                final String elem =
                   this.eitherCreate.getSettings().getGeneratedEprElementName();

                // suppress console prints of individual EPR writes if > 10
                final Print print;
                boolean suppressed = false;
                if (warray.length > 10) {
                    print = new Print();
                    suppressed = true;
                } else {
                    print = this.pr;
                }
                
                for (int i = 0; i < warray.length; i++) {
                    final int key = warray[i].getID().intValue();
                    final String name = baseName + "-" + format.format(key);
                    warray[i].setDisplayName(name);
                    final String path = name + ".epr";
                    writeOneWorkspaceEprPossibly(warray[i], path, elem, print);
                    writeOneWorkspaceEprAndIpPossibly(warray[i],
                                                      this.d.args.eprIdDir);

                }

                if (suppressed && this.pr.enabled()) {
                    final String msg = "Wrote " + warray.length +
                            " individual EPR files (base name: '" +
                            baseName + "'";
                    if (this.pr.useThis()) {
                        this.pr.infoln(PrCodes.CREATE__INSTANCE_EPRFILE_WRITES_OVERFLOW,
                                       msg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                    }
                }
                
            } else {
                for (int i = 0; i < warray.length; i++) {
                    final int key = warray[i].getID().intValue();
                    final String name = this.d.nameToPrint + "-" + format.format(key);
                    warray[i].setDisplayName(name);
                }
            }

            if (this.pr.enabled() && this.pr.useThis()) {
                this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
            }
            if (this.d.subscribeAfterDeployment) {
                this.subscribe(group.getWorkspaces());
            }
        }
    }

    private void subscribe(Workspace[] workspaces)
            throws ExecutionProblem, ExitNow, ParameterProblem {

        this.d.subscribeLaunch.subscribe(workspaces,
                                         this.d.exitState,
                                         this.d.veryTerseNotifyState,
                                         this.d.autodestroy,
                                         true,
                                         this.wasBestEffort);
    }

    // -------------------------------------------------------------------------
    // DELEGATION
    // -------------------------------------------------------------------------

    private void delegateIfNecessary() throws ParameterProblem,
                                              ExecutionProblem {
        
        if (this.d.numberOfDelegationsNeeded > 0) {
            try {
                this.delegate();
            } catch (ParameterProblem e) {
                throw e;
            } catch (ExecutionProblem e) {
                throw e;
            } catch (Exception e) {
                throw new ExecutionProblem(e.getMessage(), e);
            }
        }
    }

    private void delegate() throws Exception {

        final GlobusCredential credential =
                GlobusCredential.getDefaultCredential();

        final ClientSecurityDescriptor csd =
                WSUtils.getClientSecDesc(this.d.delegationSecMechanism,
                                         this.d.delegationProtection,
                                         this.d.delegationAuthorization);

        final EndpointReferenceType delegEpr = AddressingUtils.
                  createEndpointReference(this.d.delegationFactoryUrl, null);

        final X509Certificate[] certsToDelegateOn =
                DelegationUtil.getCertificateChainRP(delegEpr, csd);

        final X509Certificate certToSign = certsToDelegateOn[0];

        if (this.pr.enabled()) {
            final String msg = "Delegating for staging credential(s).";
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.DELEGATE__ALLMESSAGES,
                               msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }

        if (this.pr.enabled()) {
            final StringBuffer buf = new StringBuffer(512);

            buf.append("\nAbout to call delegation.\n  - Client credential: ")
               .append(credential.getIdentity())
               .append("\n  - Factory URL: ")
               .append(this.d.delegationFactoryUrl)
               .append("\n  - Security mechanism: ")
               .append(this.d.delegationSecMechanism)
               .append("\n  - Protection mechanism: ")
               .append(this.d.delegationProtection)
               .append("\n  - Authorization: ")
               .append(this.d.delegationAuthorization.getClass().getName())
               .append("\n  - Cert to sign: ")
               .append(certToSign.getSubjectDN().getName());

            final String dbg = buf.toString();
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }

        final Delegate delegate = new Delegate(credential,
                                               csd,
                                               this.d.delegationFactoryUrl,
                                               certToSign,
                                               this.d.delegationLifetime,
                                               true);

        delegate.validateAll();

        if (this.d.dryrun) {
            if (this.pr.enabled()) {
                final String msg = "Dryrun, not calling delegation service.";
                if (this.pr.useThis()) {
                    // part of PRCODE_CREATE__DRYRUN as a whole
                    this.pr.infoln(PrCodes.CREATE__DRYRUN, msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

            return; // *** EARLY RETURN ***
        }

        final EndpointReferenceType epr = delegate.delegate();
        
        //this.delegationWasPerformed = true;

        final OptionalParameters_Type opt = this.d.optionalParameters;
        if (opt == null) {
            throw new ParameterProblem(
                    "(?) optional parameters is missing, but delegation " +
                            "was performed?");
        }

        if (opt.getStageIn() != null
                && opt.getStageIn().getStagingCredential() == null) {

            opt.getStageIn().setStagingCredential(epr);

            if (this.d.delegationXferCredToo) {
                opt.getStageIn().setTransferCredential(epr);
            }
        }

        if (opt.getStageOut() != null
                && opt.getStageOut().getStagingCredential() == null) {

            opt.getStageOut().setStagingCredential(epr);

            if (this.d.delegationXferCredToo) {
                opt.getStageOut().setTransferCredential(epr);
            }
        }

        // TODO: fish out delegation resource key for printing
        //if (this.pr.enabled()) {
        //    final String uri = EPRUtils.getServiceURIAsString(epr);
        //    final String key = ________;
        //    final String msg =
        //            "Delegation performed, EPR: '" + key + "' @ '" + uri + "'";
        //    if (this.pr.useThis()) {
        //        this.pr.infoln(PrCodes.PRCODE_DELEGATE__ALLMESSAGES,
        //                       msg);
        //    } else if (this.pr.useLogging()) {
        //        logger.info(msg);
        //    }
        //}

        if (this.pr.enabled()) {

            final StringBuffer buf = new StringBuffer("\nDelegated. New " +
                    "optional parameters:\n");


                // serialized version, for severe problems
                //StringWriter writer = null;
                //try {
                //    writer = new StringWriter();
                //    final QName qName = new QName("", "optionalParameters");
                //    writer.write(ObjectSerializer.toString(opt, qName));
                //    buf.append(writer.toString());
                //    buf.append("\n\n");
                //} finally {
                //    if (writer != null) {
                //        writer.close();
                //    }
                //}

            buf.append(StringUtils.debugDumpOptional(opt));

            final String dbg = buf.toString();
            if (this.pr.useThis()) {
                this.pr.dbg(dbg);
            } else if (this.pr.useLogging()) {
                logger.debug(dbg);
            }
        }
    }
    
    
    // -------------------------------------------------------------------------
    // RUN SINGLE
    // -------------------------------------------------------------------------

    private Workspace single() throws ParameterProblem,
                                      ExecutionProblem,
                                      ExitNow {

        if (this.pr.enabled() && this.pr.useThis()) {
            this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
            final String msg =
                    "Creating workspace \"" + this.d.nameToPrint  + "\"...";
            this.pr.info(PrCodes.CREATE__INSTANCE_CREATING_PRINT_WAITING_DOTS, msg);
            this.pr.flush();
        }

        final Workspace w;
        try {
            w = this.singleCreate.create();
        } catch (WorkspaceResourceRequestDeniedFault e) {
            final String err =
                    "Resource request denied: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceSchedulingFault e) {
            final String err =
                    "Scheduling problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceMetadataFault e) {
            final String err =
                    "Metadata problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceContextualizationFault e) {
            final String err =
                    "Context broker related problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceEnsembleFault e) {
            final String err =
                    "Ensemble related problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceCreationFault e) {
            final String err =
                    "General: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (BaseFaultType e) {
            final String err = CommonStrings.faultStringOrCommonCause(e);
            throw new ExecutionProblem(err, e);
        }

        if (this.pr.enabled() && this.pr.useThis()) {
            final String msg = " done.";
            this.pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_PRINT_WAITING_DOTS, msg);
            if (!this.d.printLikeGroup) {
                this.pr.infoln(PrCodes.CREATE__INSTANCE_CREATING_PRINT_WAITING_DOTS, "\n");
            }
        }

        if (this.pr.enabled()) {
            final String msg = "Workspace created: id " + w.getID();
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
                this.pr.infoln(PrCodes.CREATE__INSTANCE_ID_PRINT, msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }

        final Schedule currentSchedule = w.getCurrentSchedule();
        if (currentSchedule != null) {
            if (currentSchedule.getActualInstantiationTime() == null) {
                this.wasBestEffort = true;
            }
        }

        if (this.d.printLikeGroup) {
            final String netStr = NetUtils.oneLineNetString(w);
            if (netStr != null) {
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__GROUP_CREATING_NET_ONELINE,
                                   "  - " + netStr);
                } else if (this.pr.useLogging()) {
                    logger.info(netStr);
                }
            }
        } else {
            NetUtils.instanceCreateResultNetPrint(this.pr, w);
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
        }

        ScheduleUtils.instanceCreateResultSchedulePrint(this.pr, w);

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
        }

        if (this.d.createEnsemble) {
            if (this.pr.enabled()) {
                final String msg = "Ensemble created: " + w.getEnsembleID();
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
                    this.pr.infoln(PrCodes.CREATE__ENSEMBLE_ID_PRINT,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }
        }

        final String elem =
                this.eitherCreate.getSettings().getGeneratedEprElementName();
        writeOneWorkspaceEprPossibly(w, this.d.instanceEprPath, elem, this.pr);
        writeOneWorkspaceEprAndIpPossibly(w, this.d.args.eprIdDir);

        this.writeEnsembleEprPossibly(w.getEnsembleMemberEPR());

        return w;
    }

    
    // -------------------------------------------------------------------------
    // RUN GROUP
    // -------------------------------------------------------------------------

    private Group group() throws ParameterProblem, ExecutionProblem, ExitNow {

        if (this.pr.enabled() && this.pr.useThis()) {
            this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
            final String msg =
                    "Creating group \"" + this.d.nameToPrint  + "\"...";
            this.pr.info(PrCodes.CREATE__GROUP_CREATING_PRINT_WAITING_DOTS, msg);
            this.pr.flush();
        }

        final Group group;
        try {
            group = this.groupCreate.createGroup();
        } catch (WorkspaceResourceRequestDeniedFault e) {
            final String err =
                    "Resource request denied: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceSchedulingFault e) {
            final String err =
                    "Scheduling problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceMetadataFault e) {
            final String err =
                    "Metadata problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceEnsembleFault e) {
            final String err =
                    "Ensemble related problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceContextualizationFault e) {
            final String err =
                    "Context broker related problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (WorkspaceCreationFault e) {
            final String err = "General problem: " + CommonUtil.faultString(e);
            throw new ExecutionProblem(err, e);
        } catch (BaseFaultType e) {
            final String err = CommonStrings.faultStringOrCommonCause(e, "group");
            throw new ExecutionProblem(err, e);
        }

        if (this.pr.enabled() && this.pr.useThis()) {
            final String msg = " done.";
            this.pr.infoln(PrCodes.CREATE__GROUP_CREATING_PRINT_WAITING_DOTS, msg);
        }

        if (this.pr.enabled()) {
            final String msg = "Group created: " + group.getGroupID();
            if (this.pr.useThis()) {
                this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
                this.pr.infoln(PrCodes.CREATE__GROUP_ID_PRINT, msg);
            } else if (this.pr.useLogging()) {
                logger.info(msg);
            }
        }

        final Workspace[] workspaces = group.getWorkspaces();
        if (workspaces != null && workspaces.length > 0) {
            final Schedule currentSchedule = workspaces[0].getCurrentSchedule();
            if (currentSchedule != null) {
                if (currentSchedule.getActualInstantiationTime() == null) {
                    this.wasBestEffort = true;
                }
            }
        }

        if (workspaces != null) {
            for (int i = 0; i < workspaces.length; i++) {
                final String netStr = NetUtils.oneLineNetString(workspaces[i]);
                if (netStr == null) {
                    continue;
                }
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__GROUP_CREATING_NET_ONELINE,
                                   "  - " + netStr);
                } else if (this.pr.useLogging()) {
                    logger.info(netStr);
                }
            }
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
        }

        if (workspaces != null && workspaces.length > 0) {
            ScheduleUtils.instanceCreateResultSchedulePrint(this.pr,
                                                            workspaces[0]);
        }

        if (this.pr.useThis()) {
            this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
        }

        if (this.d.createEnsemble) {
            if (this.pr.enabled()) {
                final String msg = "Ensemble created: " + group.getEnsembleID();
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
                    this.pr.infoln(PrCodes.CREATE__ENSEMBLE_ID_PRINT,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }
        }

        if (this.d.groupEprPath != null) {

            final QName eprQName =
                new QName("", this.eitherCreate.getSettings().
                                        getGeneratedGroupEprElementName());

            try {
                FileUtils.writeEprToFile(group.getGroupEPR(),
                                         this.d.groupEprPath,
                                         eprQName);

                if (this.pr.enabled()) {
                    final String msg = "Wrote group EPR to \"" +
                                                this.d.groupEprPath + "\"";
                    if (this.pr.useThis()) {
                        this.pr.infoln(PrCodes.CREATE__EXTRALINES, "");
                        this.pr.infoln(PrCodes.CREATE__EPRFILE_WRITES,
                                       msg);
                    } else if (this.pr.useLogging()) {
                        logger.info(msg);
                    }
                }

            } catch (Exception e) {
                final String err = "Problem writing group EPR to file: ";
                throw new ExecutionProblem(err + e.getMessage(), e);
            }
        }

        this.writeEnsembleEprPossibly(group.getEnsembleMemberEPR());

        return group;
    }


    // -------------------------------------------------------------------------
    // MISC
    // -------------------------------------------------------------------------

    private void populateCreate(Create create) {

        create.setVw(this.d.vw);
        create.setReq(this.d.dep);
        create.setOptionalParameters(this.d.optionalParameters);

        create.setCreateEnsemble(this.d.createEnsemble);
        create.setJoinEnsembleEPR(this.d.joinEnsembleEPR);
        create.setLastInEnsemble(this.d.lastInEnsemble);
    }

    private static void writeOneWorkspaceEprAndIpPossibly(Workspace w,
                                                          String dir)
            throws ExecutionProblem {
        if (dir == null || w == null) {
            return; // *** EARLY RETURN ***
        }

        final Integer id = w.getID();
        if (id == null) {
            return; // *** EARLY RETURN ***
        }

        final File writedir = new File(dir);

        final Networking net = w.getCurrentNetworking();
        if (net != null) {
            final Nic[] nics = net.nics();
            if (nics != null) {
                for (Nic nic : nics) {
                    final String ip = nic.getIpAddress();
                    if (ip != null) {
                        final String fileName = id + "-" + ip;
                        final File writeBlank = new File(writedir, fileName);
                        try {
                            FileUtils.writeStringToFile(id + "\n" + ip,
                                                        writeBlank.getAbsolutePath());
                        } catch (Exception e) {
                            throw new ExecutionProblem("Problem writing " +
                                    "id/ip file: " + e.getMessage(), e);
                        }
                    }
                }
            }

        }
    }

    private static void writeOneWorkspaceEprPossibly(Workspace w,
                                                     String path,
                                                     String eprElementName,
                                                     Print print)
            throws ExecutionProblem {

        if (w == null || path == null) {
            return; // *** EARLY RETURN ***
        }
        
        final QName eprQName = new QName("", eprElementName);

        try {
            FileUtils.writeEprToFile(w.getEpr(),
                                     path,
                                     eprQName);

            if (print.enabled()) {
                final String msg = "Wrote EPR to \"" + path + "\"";
                if (print.useThis()) {
                    print.infoln(PrCodes.CREATE__EPRFILE_WRITES,
                                   msg);
                } else if (print.useLogging()) {
                    logger.info(msg);
                }
            }

        } catch (Exception e) {
            final String err = "Problem writing EPR to file: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }

    private void writeEnsembleEprPossibly(EndpointReferenceType epr)

            throws ExecutionProblem {

        if (epr == null) {
            return; // *** EARLY RETURN ***
        }

        if (this.d.newEnsembleEprPath == null) {
            return; // *** EARLY RETURN ***
        }
        
        final QName eprQName =
            new QName("", this.eitherCreate.getSettings().
                                    getGeneratedEnsembleEprElementName());

        try {
            FileUtils.writeEprToFile(epr,
                                     this.d.newEnsembleEprPath,
                                     eprQName);

            if (this.pr.enabled()) {
                this.pr.infoln(PrCodes.CREATE__EPRFILE_WRITES, "");
                final String msg = "Wrote new ensemble EPR to \"" +
                        this.d.newEnsembleEprPath + "\"";
                if (this.pr.useThis()) {
                    this.pr.infoln(PrCodes.CREATE__EPRFILE_WRITES,
                                   msg);
                } else if (this.pr.useLogging()) {
                    logger.info(msg);
                }
            }

        } catch (Exception e) {
            final String err = "Problem writing EPR to file: ";
            throw new ExecutionProblem(err + e.getMessage(), e);
        }
    }
}
