/*
 * Copyright [2017] [Andy Moncsek]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jacpfx.other;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by amo on 17.01.17.
 */
public class GetPidTest {

  public static String getPID() {
    String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    if (processName != null && processName.length() > 0) {
      try {
        return processName.split("@")[0];
      } catch (Exception e) {
        return "0";
      }
    }

    return "0";
  }

  @Test
  @Ignore
  public void testGetPid() {
    System.out.println("test: " + getPID());
  }
}
