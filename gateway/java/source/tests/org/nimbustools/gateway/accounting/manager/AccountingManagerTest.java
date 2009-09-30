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

package org.nimbustools.gateway.accounting.manager;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import org.nimbustools.api.services.rm.*;
import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.repr.CreateRequest;
import org.nimbustools.api.repr.vm.ResourceAllocation;
import org.nimbustools.gateway.accounting.manager.defaults.DefaultAccount;
import org.hibernate.SessionFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;


public class AccountingManagerTest {

    @DataProvider(name = "createExceptions")
    public Object[][] dataProvider_createExceptions() throws Exception {

        // this was a waste of time; just wanted to play with Testng and
        // reflection

        // reflect on Manager.create(..) and grab instances of each possible
        // thrown exception
        ArrayList<Exception> exceptions = new ArrayList<Exception>();
        final Method createMethod = Manager.class.getDeclaredMethod("create",
            CreateRequest.class,
            Caller.class);
        for (Class exceptionClass : createMethod.getExceptionTypes()) {
            exceptions.add((Exception) exceptionClass.newInstance());
        }

        int[] rates = new int[] {1,2,3};
        int[] counts = new int[] {1,2,5};

        int runs = rates.length * counts.length * exceptions.size();
        Object[][] data = new Object[runs][3];
        int dataIndex = 0;

        for (int rateIndex=0; rateIndex < rates.length; rateIndex++) {
            for (int countIndex=0; countIndex < counts.length; countIndex++) {
                for (int expIndex=0; expIndex < exceptions.size(); expIndex++) {
                    data[dataIndex++] = new Object[] {
                        rates[rateIndex],
                        counts[countIndex],
                        exceptions.get(expIndex)
                    };
                }
            }
        }
        return data;
    }

    @Test(dataProvider = "createExceptions")
    public void testCreate_ensureRefund(int rate,
                                        int nodeCount,
                                        Exception createException)
        throws Exception {

        // ensures all manager.Create() exceptions are correctly handled. user
        // should be refunded

        final int charge = rate * nodeCount;

        Caller caller = mock(Caller.class);
        when(caller.getIdentity()).thenReturn("fake identity");

        Accountant accountant = mock(Accountant.class);
        when(
            accountant.getHourlyRate(any(ResourceAllocation.class))
        ).thenReturn(rate);
        when(accountant.isValidUser(caller)).thenReturn(true);

        ResourceAllocation ra = mock(ResourceAllocation.class);
        when(ra.getNodeNumber()).thenReturn(nodeCount);

        CreateRequest request = mock(CreateRequest.class);
        when(request.getRequestedRA()).thenReturn(ra);

        Manager mockManager = mock(Manager.class);
        when(mockManager.create(request, caller)).thenThrow(createException);

        SessionFactory sessionFactory = mock(SessionFactory.class);

        AccountingManager am = new AccountingManager(mockManager, accountant, sessionFactory);
        am.setUseScheduledCharging(false);
        Exception expected = null;
        try {
            am.create(request, caller);
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull(expected);
        assertEquals(expected, createException);

        verify(accountant).chargeUser(caller, charge);
        verify(accountant).creditUser(caller, charge);
    }

    @Test
    public void testCreate_notEnoughCredit() throws Exception {

        int nodeCount = 5;

        Caller caller = mock(Caller.class);
        when(caller.getIdentity()).thenReturn("fake identity");

        Accountant accountant = mock(Accountant.class);
        when(
                accountant.getHourlyRate(any(ResourceAllocation.class))
        ).thenReturn(1);
        when(accountant.isValidUser(any(Caller.class))).thenReturn(true);
        doThrow(new InsufficientCreditException()).
                when(accountant).chargeUser(any(Caller.class), anyInt());

        ResourceAllocation ra = mock(ResourceAllocation.class);
        when(ra.getNodeNumber()).thenReturn(nodeCount);

        CreateRequest request = mock(CreateRequest.class);
        when(request.getRequestedRA()).thenReturn(ra);

        Manager mockManager = mock(Manager.class);

        SessionFactory sessionFactory = mock(SessionFactory.class);


        AccountingManager am = new AccountingManager(mockManager, accountant, sessionFactory);
        am.setUseScheduledCharging(false);

        ResourceRequestDeniedException expected = null;
        try {
            am.create(request, caller);
        } catch (ResourceRequestDeniedException e) {
            expected = e;
        }
        assertNotNull(expected);

        verify(accountant).chargeUser(caller, 5);
        verify(mockManager, never()).create(request, caller);
    }

}
