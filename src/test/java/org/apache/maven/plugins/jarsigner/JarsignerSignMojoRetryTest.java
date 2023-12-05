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

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.jarsigner.JarsignerSignMojo.WaitStrategy;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerSignRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.apache.maven.plugins.jarsigner.TestJavaToolResults.RESULT_ERROR;
import static org.apache.maven.plugins.jarsigner.TestJavaToolResults.RESULT_OK;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarsignerSignMojoRetryTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MavenProject project = mock(MavenProject.class);
    private JarSigner jarSigner = mock(JarSigner.class);
    private WaitStrategy waitStrategy = mock(WaitStrategy.class);
    private File dummyMavenProjectDir;
    private Map<String, String> configuration = new LinkedHashMap<>();
    private MojoTestCreator<JarsignerSignMojo> mojoTestCreator;

    @Before
    public void setUp() throws Exception {
        dummyMavenProjectDir = folder.newFolder("dummy-project");
        mojoTestCreator = new MojoTestCreator<JarsignerSignMojo>(
                JarsignerSignMojo.class, project, dummyMavenProjectDir, jarSigner);
    }

    @Test
    public void testSignSuccessOnFirst() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);
        configuration.put("maxTries", "1");
        mojoTestCreator.setWaitStrategy(waitStrategy);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        verify(jarSigner)
                .execute(argThat(request -> request.getArchive().getPath().endsWith("my-project.jar")));
        verify(waitStrategy, times(0)).waitAfterFailure(0, Duration.ofSeconds(0));
    }

    @Test
    public void testSignFailureOnFirst() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_ERROR);
        configuration.put("maxTries", "1");
        mojoTestCreator.setWaitStrategy(waitStrategy);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);
        assertThrows(MojoExecutionException.class, () -> {
            mojo.execute();
        });
        verify(jarSigner, times(1)).execute(any());
        verify(waitStrategy, times(0)).waitAfterFailure(0, Duration.ofSeconds(0));
    }

    @Test
    public void testSignFailureOnFirstSuccessOnSecond() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        when(jarSigner.execute(any(JarSignerSignRequest.class)))
                .thenReturn(RESULT_ERROR)
                .thenReturn(RESULT_OK);
        configuration.put("maxTries", "2");
        mojoTestCreator.setWaitStrategy(waitStrategy);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        verify(jarSigner, times(2)).execute(any());
        verify(waitStrategy, times(1)).waitAfterFailure(0, Duration.ofSeconds(0));
    }

    @Test
    public void testSignFailureOnFirstFailureOnSecond() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        when(jarSigner.execute(any(JarSignerSignRequest.class)))
                .thenReturn(RESULT_ERROR)
                .thenReturn(RESULT_ERROR);
        configuration.put("maxTries", "2");
        mojoTestCreator.setWaitStrategy(waitStrategy);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);
        assertThrows(MojoExecutionException.class, () -> {
            mojo.execute();
        });
        verify(jarSigner, times(2)).execute(any());
        verify(waitStrategy, times(1)).waitAfterFailure(0, Duration.ofSeconds(0));
    }
}
