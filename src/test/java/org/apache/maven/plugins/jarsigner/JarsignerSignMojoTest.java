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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerSignRequest;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.javatool.JavaToolResult;
import org.apache.maven.shared.utils.cli.shell.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.apache.maven.plugins.jarsigner.JarsignerSignMojoTest.TestJavaToolResults.RESULT_OK;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarsignerSignMojoTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public MavenProject project = mock(MavenProject.class);

    private File dummyMavenProjectDir;

    private JarSigner jarSigner;

    private MojoTestCreator<JarsignerSignMojo> mojoTestCreator;

    @Before
    public void setUp() throws Exception {
        dummyMavenProjectDir = folder.newFolder("dummy-project");
        
        jarSigner = mock(JarSigner.class);
        mojoTestCreator = new MojoTestCreator<JarsignerSignMojo>(JarsignerSignMojo.class, project, dummyMavenProjectDir, jarSigner);
    }

    /** Standard Java project with nothing special configured */
    @Test
    public void testStandardJavaProject() throws Exception {


        Artifact mainArtifact = mock(Artifact.class);
        File mainJarFile = new File(dummyMavenProjectDir, "my-project.jar");
        
        createDummyZipFile(mainJarFile);
        
        when(mainArtifact.getFile()).thenReturn(mainJarFile);
        when(project.getArtifact()).thenReturn(mainArtifact);

        JarsignerSignMojo mojo = mojoTestCreator.configure();

        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);

        //when(jarSigner.execute(Mockito.isNull(JarSignerSignRequest.class))).thenReturn(null);

        mojo.execute();

        //verify(jarSigner).execute(any(JarSignerSignRequest.class));
        //verify(jarSigner).execute(Mockito.isNull());

        ArgumentCaptor<JarSignerSignRequest> requestArgument = ArgumentCaptor.forClass(JarSignerSignRequest.class);
        verify(jarSigner).execute(requestArgument.capture());
        JarSignerSignRequest request = requestArgument.getValue();

        assertFalse(request.isVerbose());
        assertNull(request.getKeystore());
        assertNull(request.getStoretype());
        assertNull(request.getStorepass());
        assertNull(request.getAlias());
        assertNull(request.getProviderName());
        assertNull(request.getProviderClass());
        assertNull(request.getProviderArg());
        assertNull(request.getMaxMemory());
        assertThat(request.getArguments()[0], startsWith("-J-Dfile.encoding="));
        assertEquals(dummyMavenProjectDir, request.getWorkingDirectory());
        assertEquals(mainJarFile, request.getArchive());
        assertFalse(request.isProtectedAuthenticationPath());

        assertNull(request.getKeypass());
        assertNull(request.getSigfile());
        assertNull(request.getTsaLocation());
        assertNull(request.getTsaAlias());
        assertNull(request.getSignedjar()); // TODO: Current JarsignerSignMojo does not have support for this parameter.
        assertNull(request.getCertchain());
    }

    /** Create a dummy JAR/ZIP file, enough to pass ZipInputStream.getNextEntry() */
    private static void createDummyZipFile(File zipFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("dummy-entry.txt");
            zipOutputStream.putNextEntry(entry);
        }
    }

    static class TestJavaToolResults {
        static final JavaToolResult RESULT_OK = createOk();
        static final JavaToolResult RESULT_ERROR = createError();

        private static JavaToolResult createOk() {
            JavaToolResult result = new JavaToolResult();
            result.setExitCode(0);
            result.setExecutionException(null);
            result.setCommandline(getSimpleCommandline());
            return result;
        }

        private static JavaToolResult createError() {
            JavaToolResult result = new JavaToolResult();
            result.setExitCode(1);
            result.setExecutionException(null);
            result.setCommandline(getSimpleCommandline());
            return result;
        }

        private static Commandline getSimpleCommandline() {
            Shell shell = new Shell();
            Commandline commandline = new Commandline(shell);
            commandline.setExecutable("jarsigner");
            commandline.addArguments("my-project.jar", "myalias");
            return commandline;
        }
    }
}

