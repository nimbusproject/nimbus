package org.nimbustools.gateway.ec2.monitoring;

import org.nimbustools.api.repr.Caller;
import org.nimbustools.api.services.rm.DestructionCallback;
import org.nimbustools.api.services.rm.StateChangeCallback;
import org.nimbustools.api._repr.vm._VM;

import java.util.Date;
import java.util.Calendar;
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

public interface EC2Instance {

    String getId();

    String getCallerIdentity();

    Caller getCaller();

    String getAccessKey();

    _VM getVM();

    boolean isLaunched();

    boolean isTerminated();

    Date getLaunchTime();

    Date getTerminationTime();

    void markLaunched(Calendar launchTime);

    void markTerminated(Calendar termTime);

    void registerDestructionListener(DestructionCallback listener);

    void registerStateChangeListener(StateChangeCallback listener);

}
