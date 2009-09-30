package org.nimbustools.gateway.ec2.monitoring;

import org.nimbustools.gateway.accounting.manager.defaults.DefaultInstance;
import org.nimbustools.gateway.accounting.manager.Account;
import org.nimbustools.gateway.accounting.manager.InsufficientCreditException;
import org.nimbustools.api.repr.Caller;

import java.util.List;
import java.util.Collection;
import java.util.Date;
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

public interface InstanceTracker {

    void addInstance(EC2Instance instance);

    EC2Instance getInstanceByID(String id);

    List<EC2Instance> getInstancesByCaller(Caller caller);
}
