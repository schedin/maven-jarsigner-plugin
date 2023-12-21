/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.jarsigner;

import org.apache.maven.shared.jarsigner.JarSignerSignRequest;

class TsaSelector {
    
    TsaSelector(String[] tsa, String[] tsacert, String[] tsapolicyid, String tsadigestalg) {
        
    }

    void updateTsaParameters(JarSignerSignRequest request) {
        if (tsa != null && tsa.length > 0) {
            request.setTsaLocation(tsa[0]);
        }
        if (tsacert != null && tsacert.length > 0) {
            request.setTsaAlias(tsacert[0]);
        }
        request.setCertchain(certchain);
        if (tsapolicyid != null && tsapolicyid.length > 0) {
            request.setTsapolicyid(tsapolicyid[0]);
        }
        request.setTsadigestalg(tsadigestalg);
    }
}
