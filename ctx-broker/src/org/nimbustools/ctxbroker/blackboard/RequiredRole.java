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

package org.nimbustools.ctxbroker.blackboard;

import org.nimbustools.ctxbroker.Identity;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeRole;
import org.nimbustools.ctxbroker.generated.gt4_0.description.Requires_TypeIdentity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * The generic ProvidedRole class cannot be used to express host and key
 * requirements because these requirements may be different in each
 * of the workspace/group's provided contextualization document's
 * requires section (unlikely but possible).
 */
public class RequiredRole {

    // -------------------------------------------------------------------------
    // STATIC VARIABLES
    // -------------------------------------------------------------------------
    
    private static final Log logger =
            LogFactory.getLog(Blackboard.class.getName());


    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    // cached response, only non-null if filled
    private Requires_TypeRole[] roleResponse = null;

    private final ArrayList providers = new ArrayList(); // list of Identity

    private final String name;
    private final boolean hostnameRequired;
    private final boolean pubkeyRequired;

    // number filled at last getResponsePieces call, even if it was incomplete
    private short numFilled = 0;


    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    RequiredRole(String name,
                 boolean hostnameRequired,
                 boolean pubkeyRequired) {
        this.name = name;
        this.hostnameRequired = hostnameRequired;
        this.pubkeyRequired = pubkeyRequired;
    }

    
    // -------------------------------------------------------------------------
    // MUTATE
    // -------------------------------------------------------------------------

    void clearProviders() {
        synchronized(this.providers) {
            this.roleResponse = null; // invalidate cache
            this.providers.clear();
        }
    }

    void addProvider(Identity identity) {
        if (identity == null) {
            return;
        }
        synchronized(this.providers) {
            this.roleResponse = null; // invalidate cache
            this.providers.add(identity);
        }
    }


    // -------------------------------------------------------------------------
    // COMPARISON etc
    // -------------------------------------------------------------------------

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequiredRole that = (RequiredRole) o;

        if (hostnameRequired != that.hostnameRequired) return false;
        if (pubkeyRequired != that.pubkeyRequired) return false;

        // name can not be null
        return name.equals(that.name);
    }

    public int hashCode() {
        // name can not be null
        int result = 16 * name.hashCode();
        result = 16 + result + (hostnameRequired ? 1 : 0);
        result = 16 + result + (pubkeyRequired ? 1 : 0);
        return result;
    }

    public String toString() {
        return "RequiredRole{" +
                "name='" + name + '\'' +
                ", hostnameRequired=" + hostnameRequired +
                ", pubkeyRequired=" + pubkeyRequired +
                '}';
    }

    
    // -------------------------------------------------------------------------
    // ACCESS
    // -------------------------------------------------------------------------

    String getName() {
        return name;
    }

    short getFilledNum() {
        synchronized (this.providers) {
            return this.numFilled;
        }
    }

    short getProviderNum() {
        synchronized(this.providers) {
            return (short) this.providers.size();
        }
    }
    
    boolean isHostnameRequired() {
        return this.hostnameRequired;
    }

    boolean isPubkeyRequired() {
        return this.pubkeyRequired;
    }

    ResponsePieces getResponsePieces(boolean suppressIncomplete,
                                     boolean identitiesNeeded) {
        synchronized (this.providers) {

            StringBuffer tracebuf = null;
            if (logger.isTraceEnabled()) {
                tracebuf = new StringBuffer("\n\ngetResponsePieces response ");
                tracebuf.append("for RequiredRole object ")
                        .append(super.toString())
                        .append(", ")
                        .append(this)
                        .append("\n");
            }

            if (this.roleResponse != null && !identitiesNeeded) {

                if (tracebuf != null) {
                    tracebuf.append("  - returning cached roleResponse\n")
                            .append("  - identities not needded\n");
                    logger.trace(tracebuf.toString());
                }

                // not strictly necessary to update this here
                this.numFilled = (short) this.roleResponse.length;
                
                return new ResponsePieces(this.roleResponse, null);
            }
            
            if (this.providers.isEmpty()) {
                if (tracebuf != null) {
                    tracebuf.append("  - returning null\n")
                            .append("  - providers empty\n");
                    logger.trace(tracebuf.toString());
                }

                // not strictly necessary to update this here
                this.numFilled = 0;

                return null;
            }

            final ArrayList rolePieces = new ArrayList();

            ArrayList idPieces = null;
            if (identitiesNeeded) {
                idPieces = new ArrayList();
                if (tracebuf != null) {
                    tracebuf.append("  - identities needed, made idPieces\n");
                }
            }

            boolean allFilled = true;
            final Iterator iter = this.providers.iterator();
            
            while (iter.hasNext()) {

                final Identity id = (Identity) iter.next();
                if (tracebuf != null) {
                    tracebuf.append("  - examining identity: ")
                            .append(id.toString())
                            .append("\n");
                }
                boolean filled = true;
                
                if (id.getIp() == null) {
                    if (tracebuf != null) {
                        tracebuf.append("  - IP null, setting filled false\n");
                    }
                    filled = false;
                }
                if (this.hostnameRequired && id.getHostname() == null) {
                    if (tracebuf != null) {
                        tracebuf.append("  - hostname required but was null,")
                                .append(" setting filled false\n");
                    }
                    filled = false;
                }
                if (this.pubkeyRequired && id.getPubkey() == null) {
                    if (tracebuf != null) {
                        tracebuf.append("  - pubkey required but was null,")
                                .append(" setting filled false\n");
                    }
                    filled = false;
                }

                if (filled) {

                    final Requires_TypeRole piece = new Requires_TypeRole();
                    piece.setName(this.name);
                    piece.set_value(id.getIp());
                    rolePieces.add(piece);

                    if (tracebuf != null) {
                        tracebuf.append("  - added new Requires_TypeRole, '")
                                .append(this.name)
                                .append("', IP = ")
                                .append(id.getIp())
                                .append("\n");
                    }
                    
                    if (idPieces != null) {
                        final Requires_TypeIdentity idpiece =
                                                new Requires_TypeIdentity();
                        idpiece.setIp(id.getIp());
                        idpiece.setHostname(id.getHostname());
                        idpiece.setPubkey(id.getPubkey());
                        idPieces.add(idpiece);
                    }
                    
                } else {

                    allFilled = false;

                    if (suppressIncomplete) {
                        break;
                    }
                }
            }

            this.numFilled = (short) rolePieces.size();

            if (!allFilled && suppressIncomplete) {
                if (tracebuf != null) {
                    tracebuf.append("  - returning null\n")
                            .append("  - all were not filled and ")
                            .append("suppressIncomplete is true\n");
                    logger.trace(tracebuf.toString());
                }
                return null;
            }

            if (rolePieces.isEmpty()) {
                if (tracebuf != null) {
                    tracebuf.append("  - returning null\n")
                            .append("  - rolePieces is empty\n");
                    logger.trace(tracebuf.toString());
                }
                return null;
            }

            this.roleResponse = (Requires_TypeRole[])
                  rolePieces.toArray(new Requires_TypeRole[rolePieces.size()]);

            if (idPieces == null) {
                if (tracebuf != null) {
                    tracebuf.append("  - returning pieces, size: ")
                            .append(this.roleResponse.length)
                            .append("\n");
                    logger.trace(tracebuf.toString());
                }
                return new ResponsePieces(this.roleResponse, null);
            }

            final Requires_TypeIdentity[] ids =
                    (Requires_TypeIdentity[]) idPieces.toArray(
                            new Requires_TypeIdentity[idPieces.size()]);

            if (tracebuf != null) {
                tracebuf.append("  - returning pieces, size: ")
                        .append(this.roleResponse.length)
                        .append("\n")
                        .append("  - with identities, size: ")
                        .append(ids.length)
                        .append("\n");
                logger.trace(tracebuf.toString());
            }

            return new ResponsePieces(this.roleResponse, ids);
        }
    }
}
