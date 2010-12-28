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

package org.globus.workspace.persistence.impls;

import org.globus.workspace.ProgrammingError;
import org.globus.workspace.persistence.PersistenceAdapterConstants;
import org.globus.workspace.persistence.WorkspaceDatabaseException;
import org.globus.workspace.service.InstanceResource;
import org.globus.workspace.service.binding.vm.FileCopyNeed;
import org.globus.workspace.service.binding.vm.VirtualMachine;
import org.globus.workspace.service.binding.vm.VirtualMachineDeployment;
import org.globus.workspace.service.binding.vm.VirtualMachinePartition;
import org.nimbustools.api.services.rm.ManageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

public class VirtualMachinePersistenceUtil
                                implements PersistenceAdapterConstants {

    public static PreparedStatement[] getInsertVM(InstanceResource resource,
                                                  int id,
                                                  Connection c)
            throws ManageException, SQLException {

        final VirtualMachine vm = resource.getVM();
        if (vm == null) {
            throw new ProgrammingError("vm is null");
        }

        final PreparedStatement pstmt = c.prepareStatement(SQL_INSERT_VM);

        pstmt.setInt(1, id);
        pstmt.setString(2, vm.getName());

        if (vm.getNode() != null) {
            pstmt.setString(3, vm.getNode());
        } else {
            pstmt.setNull(3, Types.VARCHAR);
        }

        if (vm.isPropagateRequired()) {
            pstmt.setInt(4, 1);
        } else {
            pstmt.setInt(4, 0);
        }

        if (vm.isUnPropagateRequired()) {
            pstmt.setInt(5, 1);
        } else {
            pstmt.setInt(5, 0);
        }

        if (vm.getNetwork() != null) {
            pstmt.setString(6, vm.getNetwork());
        } else {
            pstmt.setNull(6, Types.VARCHAR);
        }

        if (vm.getKernelParameters() != null) {
            pstmt.setString(7, vm.getKernelParameters());
        } else {
            pstmt.setNull(7, Types.VARCHAR);
        }

        if (vm.getVmm() != null) {
            pstmt.setString(8, vm.getVmm());
        } else {
            pstmt.setNull(8, Types.VARCHAR);
        }

        if (vm.getVmmVersion() != null) {
            pstmt.setString(9, vm.getVmmVersion());
        } else {
            pstmt.setNull(9, Types.VARCHAR);
        }

        if (vm.getAssociationsNeeded() != null) {
            pstmt.setString(10, vm.getAssociationsNeeded());
        } else {
            pstmt.setNull(10, Types.VARCHAR);
        }

        if (vm.getMdUserData() != null) {
            pstmt.setString(11, vm.getMdUserData());
        } else {
            pstmt.setNull(11, Types.VARCHAR);
        }
        
        pstmt.setBoolean(12, vm.isPreemptable());  

        if (vm.getCredentialName() != null) {
            pstmt.setString(12, vm.getCredentialName());
        } else {
            pstmt.setNull(12, Types.VARCHAR);
        }

        PreparedStatement pstmt2 = null;

        VirtualMachineDeployment dep = vm.getDeployment();
        if (dep != null) {
            pstmt2 = c.prepareStatement(SQL_INSERT_VM_DEPLOYMENT);
            pstmt2.setInt(1, id);
            // can be -1
            pstmt2.setInt(2, dep.getRequestedState());
            pstmt2.setInt(3, dep.getRequestedShutdown());
            // can be -1
            pstmt2.setInt(4, dep.getMinDuration());
            // can be -1 (but binding will reject that)
            pstmt2.setInt(5, dep.getIndividualPhysicalMemory());
            pstmt2.setInt(6, dep.getIndividualCPUCount());
        }

        final ArrayList inserts = new ArrayList(16);
        inserts.add(pstmt);
        if (pstmt2 != null) {
            inserts.add(pstmt2);
        }
        
        final VirtualMachinePartition[] partitions = vm.getPartitions();
        if (partitions != null) {
            for (int i = 0; i < partitions.length; i++) {
                final PreparedStatement partStmt =
                            c.prepareStatement(SQL_INSERT_VM_PARTITION);
                partStmt.setInt(1, id);

                final String image = partitions[i].getImage();
                if (image != null) {
                    partStmt.setString(2, image);
                } else {
                    partStmt.setNull(2, Types.VARCHAR);
                }

                final String imageMount = partitions[i].getImagemount();
                if (imageMount != null) {
                    partStmt.setString(3, imageMount);
                } else {
                    partStmt.setNull(3, Types.VARCHAR);
                }

                if (partitions[i].isReadwrite()) {
                    partStmt.setInt(4, 1);
                } else {
                    partStmt.setInt(4, 0);
                }

                if (partitions[i].isRootdisk()) {
                    partStmt.setInt(5, 1);
                } else {
                    partStmt.setInt(5, 0);
                }

                partStmt.setInt(6, partitions[i].getBlankspace());

                if (partitions[i].isPropRequired()) {
                    partStmt.setInt(7, 1);
                } else {
                    partStmt.setInt(7, 0);
                }

                if (partitions[i].isUnPropRequired()) {
                    partStmt.setInt(8, 1);
                } else {
                    partStmt.setInt(8, 0);
                }

                final String alt = partitions[i].getAlternateUnpropTarget();
                if (alt != null) {
                    partStmt.setString(9, alt);
                } else {
                    partStmt.setNull(9, Types.VARCHAR);
                }

                inserts.add(partStmt);

            }
        }

        final FileCopyNeed[] needs = vm.getFileCopyNeeds();
        if (needs != null) {
            for (int i = 0; i < needs.length; i++) {
                final PreparedStatement custStmt =
                            c.prepareStatement(SQL_INSERT_FILE_COPY);
                custStmt.setInt(1, id);
                custStmt.setString(2, needs[i].sourcePath);
                custStmt.setString(3, needs[i].destPath);
                if (needs[i].onImage()) {
                    custStmt.setInt(4, 1);
                } else {
                    custStmt.setInt(4, 0);
                }
                inserts.add(custStmt);
            }
        }

        return (PreparedStatement[]) inserts.toArray(
                                new PreparedStatement[inserts.size()]);
    }


    /**
     * @param vm VirtualMachine
     * @param id vm ID
     * @param c connection
     * @return delete queries
     * @throws SQLException if problem with preparestmt
     */
    public static PreparedStatement[] getRemoveVM(
                            VirtualMachine vm,
                            int id,
                            Connection c) throws SQLException {

        if (vm == null) {
            throw new IllegalArgumentException("vm is null");
        }

        final ArrayList deletes = new ArrayList();
        
        final PreparedStatement pstmt = c.prepareStatement(SQL_DELETE_VM);
        pstmt.setInt(1, id);
        deletes.add(pstmt);

        if (vm.getDeployment() != null) {
            final PreparedStatement pstmt2 =
                    c.prepareStatement(SQL_DELETE_VM_DEPLOYMENT);
            pstmt2.setInt(1, id);
            deletes.add(pstmt2);
        }

        final PreparedStatement pstmt3 =
                c.prepareStatement(SQL_DELETE_VM_PARTITIONS);
        pstmt3.setInt(1, id);
        deletes.add(pstmt3);

        final PreparedStatement pstmt4 =
                c.prepareStatement(SQL_DELETE_FILE_COPY);
        pstmt4.setInt(1, id);
        deletes.add(pstmt4);

        return (PreparedStatement[]) deletes.toArray(
                                new PreparedStatement[deletes.size()]);
    }


    public static PreparedStatement[] getVMQuery(int id, Connection c)
            throws SQLException {

        final PreparedStatement pstmt =
                c.prepareStatement(SQL_LOAD_VM);
        pstmt.setInt(1, id);

        final PreparedStatement pstmt2 =
                c.prepareStatement(SQL_LOAD_VM_DEPLOYMENT);
        pstmt2.setInt(1, id);

        final PreparedStatement pstmt3 =
                c.prepareStatement(SQL_LOAD_VM_PARTITIONS);
        pstmt3.setInt(1, id);

        final PreparedStatement pstmt4 =
                c.prepareStatement(SQL_LOAD_FILE_COPY);
        pstmt4.setInt(1, id);

        final PreparedStatement[] selects = new PreparedStatement[4];
        selects[0] = pstmt;
        selects[1] = pstmt2;
        selects[2] = pstmt3;
        selects[3] = pstmt4;
        return selects;
    }


    public static VirtualMachine newVM(int id, ResultSet rs)
                                                throws SQLException {

        VirtualMachine vm = new VirtualMachine();
        vm.setID(id);
        vm.setName(rs.getString(1));
        vm.setNode(rs.getString(2));
        boolean propagateRequired = rs.getBoolean(3);
        vm.setPropagateRequired(propagateRequired);
        boolean unPropagateRequired = rs.getBoolean(4);
        vm.setUnPropagateRequired(unPropagateRequired);
        vm.setNetwork(rs.getString(5));
        vm.setKernelParameters(rs.getString(6));
        vm.setVmm(rs.getString(7));
        vm.setVmmVersion(rs.getString(8));
        vm.setAssociationsNeeded(rs.getString(9));
        vm.setMdUserData(rs.getString(10));
        vm.setPreemptable(rs.getBoolean(11));
        vm.setCredentialName(rs.getString(12));
        return vm;
    }

    public static void addDeployment(VirtualMachine vm, ResultSet rs)
                                                    throws SQLException {

        final VirtualMachineDeployment dep = new VirtualMachineDeployment();
        dep.setRequestedState(rs.getInt(1));
        dep.setRequestedShutdown(rs.getInt(2));
        dep.setMinDuration(rs.getInt(3));
        dep.setIndividualPhysicalMemory(rs.getInt(4));
        dep.setIndividualCPUCount(rs.getInt(5));
        vm.setDeployment(dep);
    }

    public static VirtualMachinePartition getPartition(ResultSet rs)
                                                        throws SQLException {

        final VirtualMachinePartition partition =
                new VirtualMachinePartition();
        
        partition.setImage(rs.getString(1));
        partition.setImagemount(rs.getString(2));
        partition.setReadwrite(rs.getBoolean(3));
        partition.setRootdisk(rs.getBoolean(4));
        partition.setBlankspace(rs.getInt(5));
        partition.setPropRequired(rs.getBoolean(6));
        partition.setUnPropRequired(rs.getBoolean(7));
        partition.setAlternateUnpropTarget(rs.getString(8));
        return partition;
    }

    public static FileCopyNeed getNeed(ResultSet rs)
            throws WorkspaceDatabaseException {

        try {
            final String src = rs.getString(1);
            final String dst = rs.getString(2);
            final boolean sent = rs.getBoolean(3);
            return new FileCopyNeed(src, dst, sent);
        } catch (Exception e) {
            throw new WorkspaceDatabaseException(e.getMessage(), e);
        }
    }
}
