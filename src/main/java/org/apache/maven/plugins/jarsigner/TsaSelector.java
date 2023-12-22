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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to select a Time Stamping Authority (TSA) server along with parameters to send. The protocol is defined
 * in RFC 3161: Internet X.509 Public Key Infrastructure Time-Stamp Protocol (TSP).
 *
 * From a jarsigner perspective there are two things that are important:
 * 1. Finding a TSA server URL
 * 2. What parameters to use for TSA server communication.
 *
 * Finding a URL can be done in two ways:
 * a) The end-user has specified an explicit URL (the most common way)
 * b) The end-user has specified a keystore alias that points to a certificate in the active keystore. From the
 *    certificate the X509v3 extension "Subject Information Access" field is examined to find the TSA server URL.
 *    Example:
 *    <pre>
 *    [vagrant@podmanhost ~]$ openssl x509 -noout -ext subjectInfoAccess -in tsa-server.crt
 *    Subject Information Access:
 *        AD Time Stamping - URI:http://timestamp.globalsign.com/tsa/r6advanced1
 *    </pre>
 *
 * Each TSA server vendor typically has defined its own OID for what "policy" to use in the timestamping process. For
 * example GlobalSign might use 1.3.6.1.4.1.4146.2.3.1.2. A DigiCert TSA server would not accept this OID. In most cases
 * there is no need for the end-user to specify this because the TSA server will choose a default.
 *
 * jarsigner will send a message digest to the TSA server along with the message digest algorithm. For example
 * {@code SHA-384}. A TSA server might reject the chosen algorithm, but typically most TSA servers supports the "common"
 * ones (like SHA-256, SHA-384 and SHA-512). In most cases there is no need for the end-user to specify this because the
 * jarsigner tool choose a good default.
 */
class TsaSelector {

    /** The current TsaServer in use (if any). One per thread */
    private final ThreadLocal<TsaServer> currentTsaServer = new ThreadLocal<>();

    /** List of TSA servers. Will at minimum contain a dummy value */
    private final List<TsaServer> tsaServers;

    TsaSelector(String[] tsa, String[] tsacert, String[] tsapolicyid, String tsadigestalg) {
        List<TsaServer> tsaServersTmp = new ArrayList<>();

        this.tsaServers = Collections.unmodifiableList(tsaServersTmp);
    }

//    private void updateTsaParameters(JarSignerSignRequest request) {
//        if (tsa != null && tsa.length > 0) {
//            request.setTsaLocation(tsa[0]);
//        }
//        if (tsacert != null && tsacert.length > 0) {
//            request.setTsaAlias(tsacert[0]);
//        }
//        request.setCertchain(certchain);
//        if (tsapolicyid != null && tsapolicyid.length > 0) {
//            request.setTsapolicyid(tsapolicyid[0]);
//        }
//        request.setTsadigestalg(tsadigestalg);
//    }

    /**
     * Gets the next "best" TSA server to use.
     */
    public TsaServer getServer() {
        // TODO Auto-generated method stub
        return null;
    }

    public void registerFailure() {
        if (currentTsaServer.get() != null) {
            currentTsaServer.get().failureCount.incrementAndGet();
        }
    }

    /** Representation of a single TSA server and the parameters to use for it */
    static class TsaServer {
        private static TsaServer EMPTY = new TsaServer(null, null, null, null);

        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final String tsaUrl;
        private final String tsaAlias;
        private final String tsaPolicyId;
        private final String tsaDigestAlt;

        private TsaServer(String tsaUrl, String tsaAlias, String tsaPolicyId, String tsaDigestAlt) {
            this.tsaUrl = tsaUrl;
            this.tsaAlias = tsaAlias;
            this.tsaPolicyId = tsaPolicyId;
            this.tsaDigestAlt = tsaDigestAlt;
        }

        public String getTsaUrl() {
            return tsaUrl;
        }

        public String getTsaAlias() {
            return tsaAlias;
        }

        public String getTsaPolicyId() {
            return tsaPolicyId;
        }

        public String getTsaDigestAlt() {
            return tsaDigestAlt;
        }
    }
}
