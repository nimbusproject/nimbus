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

package org.nimbustools.auto_config;

import java.util.Scanner;

public class UserQuestions {

    public boolean getUserYesNo() throws Exception {
        Scanner in = new Scanner(System.in);
        int count = 7;
        while (count > 0) {
            final String answer = in.nextLine();
            if (answer != null && answer.trim().length() != 0) {
                final char cmp = answer.trim().toCharArray()[0];
                if ('y' == cmp || 'Y' == cmp) {
                    return true;
                }
                if ('n' == cmp || 'N' == cmp) {
                    return false;
                }
            }
            count = count -1;
            if (count > 0) {
                System.out.println("\nPlease enter 'y' or 'n':");
            }
        }

        throw new Exception("Can not make progress.");
    }

    public int getInt(String prompt, int min, int max) throws Exception {
        Scanner in = new Scanner(System.in);
        int count = 7;
        while (count > 0) {
            System.out.println(prompt);
            final int answer;
            try {
                answer = in.nextInt();
                if (answer < min) {
                    throw new Exception();
                }
                if (answer > max) {
                    throw new Exception();
                }
                return answer;
            } catch (Throwable t) {
                count = count - 1;
            }
        }

        throw new Exception("Can not make progress.");
    }
}
