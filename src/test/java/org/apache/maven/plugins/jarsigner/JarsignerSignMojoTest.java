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

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class JarsignerSignMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void test() throws Exception {
        PlexusConfiguration pluginConfiguration = mock(PlexusConfiguration.class);
        System.out.println(pluginConfiguration);
        
        JarsignerSignMojo mojo = new JarsignerSignMojo();
        mojo = (JarsignerSignMojo) mojoRule.configureMojo(mojo, pluginConfiguration);
        
        //JarsignerSignMojo myMojo = (JarsignerSignMojo) mojoRule.lookupMojo( "touch", pom );
        //assertNotNull( myMojo );
        mojo.execute();

//        PlexusConfiguration pluginConfiguration = null;
//        JarsignerSignMojo jarsignerSignMojo = (JarsignerSignMojo)
//                mojoRule.lookupMojo(TEST_GROUP, TEST_ARTIFACT, TEST_VERSION, SIGN_GOAL, pluginConfiguration);
    }
}
