/*
 * Copyright 1999-2010 University of Chicago
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

package org.nimbustools.messaging.gt4_0.common;

public class OKJVM {

	/**
	 * It is up to the caller to determine what message to print in each case.
	 *
	 * Current exit codes:
	 *   1 - empty java.vm.name property
	 *   2 - libgcj
	 *   0 - all other JVMs
	 * 
	 * @param args no args
	 */
	public static void main(String[] args) {
		final String vmName = System.getProperty("java.vm.name");
		if (vmName == null || vmName.trim().length() == 0) {
			System.exit(1);
		}
		if (vmName.indexOf("libgcj") != -1) {
			System.exit(2);
		}

		System.exit(0);
	}
}
