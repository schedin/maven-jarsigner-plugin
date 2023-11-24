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

import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@MojoTest
public class JarsignerSignMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    private final DefaultPlexusConfiguration configuration = new DefaultPlexusConfiguration("configuration");
    
    @Test
    public void test() throws Exception {
        configuration.addChild("processMainArtifact", "true");

        JarsignerSignMojo mojo = new JarsignerSignMojo();
        mojo = (JarsignerSignMojo) mojoRule.configureMojo(mojo, configuration);

//        JarsignerSignMojo mojo = (JarsignerSignMojo) mojoRule.lookupMojo("mygroup",  "artifactid", "1.0.", "sign", null);


//        PlexusConfiguration pluginConfiguration = null;
//        JarsignerSignMojo mojo = (JarsignerSignMojo)
//                mojoRule.lookupMojo("testgroup", "testartifact", "10.0.2", "sign", configuration);
        
        
        mojo.execute();
        
    }
}
