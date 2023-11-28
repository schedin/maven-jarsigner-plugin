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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.AbstractJarSignerRequest;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerSignRequest;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.javatool.JavaToolResult;
import org.apache.maven.shared.utils.cli.shell.Shell;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;

import static org.apache.maven.plugins.jarsigner.JarsignerSignMojoTest.TestJavaToolResults.RESULT_OK;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarsignerSignMojoTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MavenProject project = mock(MavenProject.class);
    private JarSigner jarSigner = mock(JarSigner.class);
    private File dummyMavenProjectDir;
    private Map<String, String> configuration = new LinkedHashMap<>();
    private MojoTestCreator<JarsignerSignMojo> mojoTestCreator;
    
    @Before
    public void setUp() throws Exception {
        dummyMavenProjectDir = folder.newFolder("dummy-project");
        mojoTestCreator = new MojoTestCreator<JarsignerSignMojo>(
                JarsignerSignMojo.class, project, dummyMavenProjectDir, jarSigner);
    }

    /** Standard Java project with nothing special configured */
    @Test
    public void testStandardJavaProject() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

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
        assertEquals(mainArtifact.getFile(), request.getArchive());
        assertFalse(request.isProtectedAuthenticationPath());

        assertNull(request.getKeypass());
        assertNull(request.getSigfile());
        assertNull(request.getTsaLocation());
        assertNull(request.getTsaAlias());
        assertNull(request.getSignedjar()); // TODO: Current JarsignerSignMojo does not have support for this parameter.
        assertNull(request.getCertchain());
    }

    /** Standard POM project with nothing special configured */
    @Test
    public void testStandardPOMProject() throws Exception {
        Artifact mainArtifact = TestArtifacts.createPomArtifact(dummyMavenProjectDir, "my-project.pom");
        when(project.getArtifact()).thenReturn(mainArtifact);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        verify(jarSigner, times(0)).execute(any()); // Should not try to sign anything
    }

    /** Normal Java project, but avoid to process the main artifact (processMainArtifact to false) */ 
    @Test
    public void testDontProcessMainArtifact() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        configuration.put("processMainArtifact", "false");
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        verify(jarSigner, times(0)).execute(any()); // Should not try to sign anything
    }

    /** Test that it is possible to disable processing of attached artifacts */ 
    @Test
    public void testDontProcessAttachedArtifacts() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        configuration.put("processAttachedArtifacts", "false");
        
        when(project.getAttachedArtifacts()).thenReturn(Arrays.asList(TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-sources.jar", "sources", "java-source")));
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);
        
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        // Make sure that only the main artifact has been processed, but not the attached artifact
        verify(jarSigner, times(1)).execute(MockitoHamcrest.argThat(JarSignerRequestMatcher.hasFileName("my-project.jar")));
        verify(jarSigner, times(0)).execute(MockitoHamcrest.argThat(JarSignerRequestMatcher.hasFileName("my-project-sources.jar")));
    }
    
    /** A Java project with main-, javadoc- and sources-artifacts */
    @Test
    public void testJavaProjectWithSourcesAndJavadoc() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        Artifact sourcesArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-sources.jar", "sources", "java-source");
        Artifact javadocArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-javadoc.jar", "javadoc", "javadoc");
        when(project.getAttachedArtifacts()).thenReturn(Arrays.asList(sourcesArtifact, javadocArtifact));
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        ArgumentCaptor<JarSignerSignRequest> requestArgument = ArgumentCaptor.forClass(JarSignerSignRequest.class);
        verify(jarSigner, times(3)).execute(requestArgument.capture());

        List<JarSignerSignRequest> requests = requestArgument.getAllValues();
        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("my-project.jar")));
        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("my-project-sources.jar")));
        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("my-project-javadoc.jar")));
    }

    /** Set every possible documented parameter (see Optional Parameters in site documentation). */
    @Test
    public void testEveryParameter() throws Exception {
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);

        // TODO: Implement this for every parameters
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);

        TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-classifier1.jar", "classifier1");
        TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-classifier2.jar", "classifier2");
        TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-different_not_included.jar", "different_not_included");
        TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-exclude_this.jar", "exclude_this_classifier");

        File archiveToProcess = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "archive-to-process.jar").getFile();

        File archiveDirectory = new File(dummyMavenProjectDir, "my_archive_dir");
        archiveDirectory.mkdir();
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive1.jar"));
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive2.jar"));
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive_to_exclude.jar"));
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "not_this.par"));

        configuration.put("alias", "myalias");
        configuration.put("archive", archiveToProcess.getPath());
        configuration.put("archiveDirectory", archiveToProcess.getPath());
        configuration.put("arguments", "jarsigner-arg1,jarsigner-arg2");
        configuration.put("excludeClassifiers", "exclude_this");
        configuration.put("includeClassifiers", "classifier");
        configuration.put("include", "*.jar");
        configuration.put("exclude", "*_to_exclude.jar");

        configuration.put("keypass", "mykeypass");
        configuration.put("keystore", "mykeystore");
        configuration.put("maxMemory", "mymaxmemory");
        configuration.put("processAttachedArtifacts", "true"); //Is default true, but set anyway.
        


        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();
    }

    static class TestArtifacts {
        static final String TEST_GROUPID = "org.test-group";
        static final String TEST_ARTIFACTID = "test-artifact";
        static final String TEST_VERSION = "9.10.2";
        static final String TEST_TYPE = "jar";
        static final String TEST_CLASSIFIER = "";

        static Artifact createJarArtifact(File directory, String filename) throws IOException {
            return createJarArtifact(directory, filename, TEST_CLASSIFIER);
        }

        public static Artifact createJarArtifact(File directory, String filename, String classifier) throws IOException {
            return createJarArtifact(directory, filename, classifier, TEST_TYPE);
        }
        
        public static Artifact createJarArtifact(File directory, String filename, String classifier, String type) throws IOException {
            File file = new File(directory, filename);
            createDummyZipFile(file);
            Artifact artifact = new DefaultArtifact(
                    TEST_GROUPID, TEST_ARTIFACTID, TEST_VERSION, Artifact.SCOPE_COMPILE, type, classifier, null);
            artifact.setFile(file);
            return artifact;
        }

        static Artifact createPomArtifact(File directory, String filename) throws IOException {
            File file = new File(directory, filename);
            createDummyXMLFile(file);
            Artifact artifact = new DefaultArtifact(
                    TEST_GROUPID, TEST_ARTIFACTID, TEST_VERSION, Artifact.SCOPE_COMPILE, TEST_TYPE, "", null);
            artifact.setFile(file);
            return artifact;
        }

        /** Create a dummy JAR/ZIP file, enough to pass ZipInputStream.getNextEntry() */
        static File createDummyZipFile(File zipFile) throws IOException {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
                ZipEntry entry = new ZipEntry("dummy-entry.txt");
                zipOutputStream.putNextEntry(entry);
            }
            return zipFile;
        }

        /** Create a dummy XML file, for example to simulate a pom.xml file */
        static File createDummyXMLFile(File xmlFile) throws IOException {
            Files.write(xmlFile.toPath(), "<project/>".getBytes());
            return xmlFile;
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

    /** Hamcrest matcher(s) to match properties on a AbstractJarSignerRequest object */
    private static class JarSignerRequestMatcher extends TypeSafeMatcher<AbstractJarSignerRequest> {
        private final String predicateDescription;
        private final Object value;
        private final Predicate<AbstractJarSignerRequest> predicate;

        private JarSignerRequestMatcher(String predicateDescription, Object value, Predicate<AbstractJarSignerRequest> predicate) {
            this.predicateDescription = predicateDescription;
            this.value = value;
            this.predicate = predicate;
        }

        @Override
        protected boolean matchesSafely(AbstractJarSignerRequest request) {
            return predicate.test(request);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("request that ").appendText(predicateDescription).appendValue(value);
        }
        
        /** Create a matcher that matches when the request is using a specific file name for the archive */
        static TypeSafeMatcher<AbstractJarSignerRequest> hasFileName(String expectedFileName) {
            return new JarSignerRequestMatcher("has archive file name ", expectedFileName,
                request -> request.getArchive().getPath().endsWith(expectedFileName));
        }
    }
}
