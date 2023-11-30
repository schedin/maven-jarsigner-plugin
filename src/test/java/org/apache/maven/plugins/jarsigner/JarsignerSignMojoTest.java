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
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerSignRequest;
import org.apache.maven.shared.jarsigner.JarSignerUtil;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.javatool.JavaToolResult;
import org.apache.maven.shared.utils.cli.shell.Shell;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.hamcrest.MockitoHamcrest;

import static org.apache.maven.plugins.jarsigner.JarsignerSignMojoTest.TestJavaToolResults.RESULT_OK;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
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
        assertNull(request.getSignedjar()); // Current JarsignerSignMojo does not have support for this parameter.
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

    /** Make sure that when skip is configured the Mojo will not process anything */
    @Test
    public void testSkip() throws Exception {
        when(project.getArtifact()).thenThrow(new AssertionError("Code should not try to get any artifacts"));
        configuration.put("skip", "true");
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        verify(jarSigner, times(0)).execute(any()); // Should not try to sign anything
    }

    /** Only process the specified archive, don't process the main artifact nor the attached. */
    @Test
    public void testArchive() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        File archiveToProcess = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "archive-to-process.jar").getFile();
        configuration.put("archive", archiveToProcess.getPath());
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        // Make sure only the jar pointed by "archive" has been processed, but not the main artifact
        verify(jarSigner, times(0)).execute(MockitoHamcrest.argThat(JarSignerRequestMatcher.hasFileName("my-project.jar")));
        verify(jarSigner, times(1)).execute(MockitoHamcrest.argThat(JarSignerRequestMatcher.hasFileName("archive-to-process.jar")));
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

    /** A Java project with 3 types of artifacts: main, javadoc and sources */
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

    /**
     * Set most possible documented parameter (that does not interfere too much with testing of other parameters). See
     * Optional Parameters in site documentation.
     */
    @Test
    public void testBig() throws Exception {
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);

        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);

        when(project.getAttachedArtifacts()).thenReturn(Arrays.asList(
            TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-sources.jar", "sources"),
            TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-javadoc.jar", "javadoc"),
            TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-included_and_excluded.jar", "included_and_excluded"),
            TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project-excluded_classifier.jar", "excluded_classifier")
        ));

        File workingDirectory = new File(dummyMavenProjectDir, "my_working_dir");
        workingDirectory.mkdir();

        File archiveDirectory = new File(dummyMavenProjectDir, "my_archive_dir");
        archiveDirectory.mkdir();
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive1.jar"));
        File previouslySignedArchive = TestArtifacts.createDummySignedJarFile(new File(archiveDirectory, "previously_signed_archive.jar"));
        assertTrue("previously_signed_archive.jar should be detected as a signed file before executing the Mojo", JarSignerUtil.isArchiveSigned(previouslySignedArchive));
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive_to_exclude.jar"));
        TestArtifacts.createDummyZipFile(new File(archiveDirectory, "not_this.par"));

        configuration.put("alias", "myalias");

        //Setting "archive" parameter disables effect of many others, so it is tested separately in other test case

        configuration.put("archiveDirectory", archiveDirectory.getPath());
        configuration.put("arguments", "jarsigner-arg1,jarsigner-arg2");
        configuration.put("certchain", "mycertchain");
        configuration.put("excludeClassifiers", "excluded_classifier,included_and_excluded");
        configuration.put("includeClassifiers", "sources,javadoc,included_and_excluded");
        configuration.put("includes", "*.jar");
        configuration.put("excludes", "*_to_exclude.jar");
        configuration.put("keypass", "mykeypass");
        configuration.put("keystore", "mykeystore");
        configuration.put("maxMemory", "mymaxmemory");
        configuration.put("processAttachedArtifacts", "true"); //Is default true, but set anyway.
        configuration.put("processMainArtifact", "true"); //Is default true, but set anyway.
        configuration.put("protectedAuthenticationPath", "true");
        configuration.put("providerArg", "myproviderarg");
        configuration.put("providerClass", "myproviderclass");
        configuration.put("providerName", "myprovidername");
        configuration.put("removeExistingSignatures", "true");
        configuration.put("sigfile", "mysigfile");
        //configuration.put("signedjar", "mysignedjar"); // Current JarsignerSignMojo does not support this parameter.

        configuration.put("skip", "false"); //Is default false, but set anyway
        configuration.put("storepass", "mystorepass");
        configuration.put("storetype", "mystoretype");
        configuration.put("tsa", "mytsa");
        configuration.put("tsacert", "mytsacert");
        configuration.put("verbose", "true");
        configuration.put("workingDirectory", workingDirectory.getPath());

        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        ArgumentCaptor<JarSignerSignRequest> requestArgument = ArgumentCaptor.forClass(JarSignerSignRequest.class);
        verify(jarSigner, times(5)).execute(requestArgument.capture());
        List<JarSignerSignRequest> requests = requestArgument.getAllValues();

        assertThat(requests, everyItem(JarSignerRequestMatcher.hasAlias("myalias")));

        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("archive1.jar")));
        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("previously_signed_archive.jar")));
        assertThat(requests, hasItem(not(JarSignerRequestMatcher.hasFileName("archive_to_exclude.jar"))));
        assertThat(requests, hasItem(not(JarSignerRequestMatcher.hasFileName("not_this.par"))));

        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("my-project.jar")));
        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("my-project-sources.jar")));
        assertThat(requests, hasItem(JarSignerRequestMatcher.hasFileName("my-project-javadoc.jar")));
        assertThat(requests, hasItem(not(JarSignerRequestMatcher.hasFileName("my-project-included_and_excluded.jar"))));
        assertThat(requests, hasItem(not(JarSignerRequestMatcher.hasFileName("my-project-excluded_classifier.jar"))));

        assertThat(requests, everyItem(JarSignerRequestMatcher.hasArguments(new String[]{"jarsigner-arg1", "jarsigner-arg2"})));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasCertchain("mycertchain")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasKeypass("mykeypass")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasKeystore("mykeystore")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasMaxMemory("mymaxmemory")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasProtectedAuthenticationPath(true)));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasProviderArg("myproviderarg")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasProviderClass("myproviderclass")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasProviderName("myprovidername")));
        assertFalse(JarSignerUtil.isArchiveSigned(previouslySignedArchive)); // Make sure previous signing is gone
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasSigfile("mysigfile")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasStorepass("mystorepass")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasStoretype("mystoretype")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasTsa("mytsa")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasTsacert("mytsacert")));
        assertThat(requests, everyItem(JarSignerRequestMatcher.hasVerbose(true)));
    }

    /** Make sure that if a custom ToolchainManager is set on the Mojo, it is used by jarSigner */
    @Test
    public void testToolchainManager() throws Exception {
        Artifact mainArtifact = TestArtifacts.createJarArtifact(dummyMavenProjectDir, "my-project.jar");
        when(project.getArtifact()).thenReturn(mainArtifact);
        when(jarSigner.execute(any(JarSignerSignRequest.class))).thenReturn(RESULT_OK);

        Toolchain toolchain = mock(Toolchain.class);
        ToolchainManager toolchainManager = mock(ToolchainManager.class);
        when(toolchainManager.getToolchainFromBuildContext(any(), any())).thenReturn(toolchain);
        mojoTestCreator.setToolchainManager(toolchainManager);

        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        mojo.execute();

        verify(jarSigner).setToolchain(toolchain);
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

        /** Create a dummy signed JAR, enough to pass JarSignerUtil.isArchiveSigned() */
        static File createDummySignedJarFile(File jarFile) throws IOException {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(jarFile))) {
                ZipEntry entry = new ZipEntry("dummy-entry.txt");
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.putNextEntry(new ZipEntry("META-INF/dummy.RSA"));
            }

            return jarFile;
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
    private static class JarSignerRequestMatcher extends TypeSafeMatcher<JarSignerSignRequest> {
        private final String predicateDescription;
        private final Object value;
        private final Predicate<JarSignerSignRequest> predicate;

        private JarSignerRequestMatcher(String predicateDescription, Object value, Predicate<JarSignerSignRequest> predicate) {
            this.predicateDescription = predicateDescription;
            this.value = value;
            this.predicate = predicate;
        }


        @Override
        protected boolean matchesSafely(JarSignerSignRequest request) {
            return predicate.test(request);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("request that ").appendText(predicateDescription).appendValue(value);
        }

        /** Create a matcher that matches when the request is using a specific file name for the archive */
        static TypeSafeMatcher<JarSignerSignRequest> hasFileName(String expectedFileName) {
            return new JarSignerRequestMatcher("has archive file name ", expectedFileName,
                request -> request.getArchive().getPath().endsWith(expectedFileName));
        }

        static TypeSafeMatcher<JarSignerSignRequest> hasAlias(String alias) {
            return new JarSignerRequestMatcher("has alias ", alias,
                request -> request.getAlias().equals(alias));
        }

        static TypeSafeMatcher<JarSignerSignRequest> hasArguments(String[] arguments) {
            return new JarSignerRequestMatcher("has arguments ", arguments,
                request -> {
                    List<String> haystack = Arrays.asList(request.getArguments());
                    for (String argumentNeedle : arguments) {
                        if (!haystack.contains(argumentNeedle)) {
                            return false;
                        }
                    }
                    return true;
                }
           );
        }

        static TypeSafeMatcher<JarSignerSignRequest> hasKeypass(String keypass) {
            return new JarSignerRequestMatcher("has keypass ", keypass,
                request -> request.getKeypass().equals(keypass));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasKeystore(String keystore) {
            return new JarSignerRequestMatcher("has keystore ", keystore,
                request -> request.getKeystore().equals(keystore));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasMaxMemory(String maxMemory) {
            return new JarSignerRequestMatcher("has maxMemory ", maxMemory,
                request -> request.getMaxMemory().equals(maxMemory));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasProtectedAuthenticationPath(boolean protectedAuthenticationPath) {
            return new JarSignerRequestMatcher("has protectedAuthenticationPath ", protectedAuthenticationPath,
                request -> request.isProtectedAuthenticationPath() == protectedAuthenticationPath);
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasProviderArg(String providerArg) {
            return new JarSignerRequestMatcher("has providerArg ", providerArg,
                request -> request.getProviderArg().equals(providerArg));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasProviderClass(String providerClass) {
            return new JarSignerRequestMatcher("has providerClass ", providerClass,
                request -> request.getProviderClass().equals(providerClass));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasProviderName(String providerName) {
            return new JarSignerRequestMatcher("has providerName ", providerName,
                request -> request.getProviderName().equals(providerName));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasSigfile(String sigfile) {
            return new JarSignerRequestMatcher("has sigfile ", sigfile,
                request -> request.getSigfile().equals(sigfile));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasStorepass(String storepass) {
            return new JarSignerRequestMatcher("has storepass ", storepass,
                request -> request.getStorepass().equals(storepass));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasStoretype(String storetype) {
            return new JarSignerRequestMatcher("has storetype ", storetype,
                request -> request.getStoretype().equals(storetype));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasTsa(String tsa) {
            return new JarSignerRequestMatcher("has tsa ", tsa,
                request -> request.getTsaLocation().equals(tsa));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasTsacert(String tsacert) {
            return new JarSignerRequestMatcher("has tsacert ", tsacert,
                request -> request.getTsaAlias().equals(tsacert));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasCertchain(String certchain) {
            return new JarSignerRequestMatcher("has certchain ", certchain,
                request -> request.getCertchain().getPath().equals(certchain));
        }
        static TypeSafeMatcher<JarSignerSignRequest> hasVerbose(boolean verbose) {
            return new JarSignerRequestMatcher("has verbose ", verbose,
                request -> request.isVerbose() == verbose);
        }
    }
}

