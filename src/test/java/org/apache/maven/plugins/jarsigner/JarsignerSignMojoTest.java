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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JarsignerSignMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private final DefaultPlexusConfiguration configuration = new DefaultPlexusConfiguration("configuration");
    

    public MavenProject project = mock(MavenProject.class);
    
    @Before
    public void setUp() {
    }

    @Test
    public void testSimpleJavaProject() throws Exception {
        Artifact mainArtifact = mock(Artifact.class);
        File mainJarFile = folder.newFile("my-project.jar");
        createDummyZipFile(mainJarFile);
        when(mainArtifact.getFile()).thenReturn(mainJarFile);
        when(project.getArtifact()).thenReturn(mainArtifact);
        JarsignerSignMojo mojo = MojoTestCreator.create(JarsignerSignMojo.class, project);

        mojo.execute();
    }
    
    @Ignore
    @Test
    public void test() throws Exception {
        configuration.addChild("processMainArtifact", "true");

//        JarsignerSignMojo mojo = new JarsignerSignMojo();
//        mojo = (JarsignerSignMojo) mojoRule.configureMojo(mojo, configuration);

//        mojo = (JarsignerSignMojo) mojoRule.configureMojo(mojo, "maven-jarsigner-plugin", new File("src/test/resources/unit/project-to-test/pom.xml"));

//        JarsignerSignMojo mojo = mojoRule.lookupMojo("sign", new File("src/test/resources/unit/project-to-test/pom.xml"));
        JarsignerSignMojo mojo = mojoRule.lookupEmptyMojo("sign", new File("src/test/resources/unit/empty-project/pom.xml"));
        
//        JarsignerSignMojo mojo = (JarsignerSignMojo) mojoRule.lookupMojo("org.apache.maven.plugins",  "maven-jarsigner-plugin", "3.1.0-SNAPSHOT", "sign", null);


//        PlexusConfiguration pluginConfiguration = null;
//        JarsignerSignMojo mojo = (JarsignerSignMojo)
//                mojoRule.lookupMojo("testgroup", "testartifact", "10.0.2", "sign", configuration);
        
        
        mojo.execute();
        
    }

    private static void createDummyZipFile(File zipFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("dummy-entry.txt");
            zipOutputStream.putNextEntry(entry);
        }
    }

    
}
