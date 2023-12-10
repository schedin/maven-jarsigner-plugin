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
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerSignRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.apache.maven.plugins.jarsigner.TestJavaToolResults.RESULT_OK;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JarsignerSignMojoParallelTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MavenProject project = mock(MavenProject.class);
    private JarSigner jarSigner = mock(JarSigner.class);
    private File projectDir;
    private Map<String, String> configuration = new LinkedHashMap<>();
    private MojoTestCreator<JarsignerSignMojo> mojoTestCreator;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        projectDir = folder.newFolder("dummy-project");
        configuration.put("processMainArtifact", "false");
        mojoTestCreator =
                new MojoTestCreator<JarsignerSignMojo>(JarsignerSignMojo.class, project, projectDir, jarSigner);
        executor =
                Executors.newSingleThreadExecutor(namedThreadFactory(getClass().getSimpleName()));
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test(timeout = 300000) // TODO: change timeout before merge
    public void test10Files2Parallel() throws Exception {
        configuration.put("archiveDirectory", createArchives(10).getPath());
        configuration.put("threadCount", "2");

        CountDownLatch latch = new CountDownLatch(1);
        when(jarSigner.execute(isA(JarSignerSignRequest.class))).then(invocation -> {
            JarSignerSignRequest request = (JarSignerSignRequest) invocation.getArgument(0);
            // Make one jar file wait until some external event happens
            if (request.getArchive().getPath().endsWith("archive2.jar")) {
                latch.await();
            }
            return RESULT_OK;
        });
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        Future<Void> future = executor.submit(() -> {
            mojo.execute();
            return null;
        });

        // Wait until 10 invocation to execute has happened (nine files are done and one are hanging)
        verify(jarSigner, timeout(Duration.ofSeconds(100).toMillis()).times(10)).execute(any());
        // Even though 10 invocations to execute() has happened, mojo is not yet done executing (it is waiting for one)
        assertFalse(future.isDone());

        latch.countDown(); // Release the one waiting jar file
        future.get(100, TimeUnit.SECONDS); // Wait for entire Mojo to finish
        assertTrue(future.isDone());
    }

    
    @Test(timeout = 30000) // TODO: change timeout before merge
    public void test10Files2Parallel2Hanging() throws Exception {
        configuration.put("archiveDirectory", createArchives(10).getPath());
        configuration.put("threadCount", "2");

        CountDownLatch latch = new CountDownLatch(2);
        when(jarSigner.execute(isA(JarSignerSignRequest.class))).then(invocation -> {
            JarSignerSignRequest request = (JarSignerSignRequest) invocation.getArgument(0);
            // Make one jar file wait until some external event happens
            if (request.getArchive().getPath().endsWith("archive2.jar") || request.getArchive().getPath().endsWith("archive3.jar")) {
                latch.await();
            }
            return RESULT_OK;
        });
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        Future<Void> future = executor.submit(() -> {
            mojo.execute();
            return null;
        });

        // Wait until 4 invocation to execute has happened (0 and 1 has finished and 2 and 3 are hanging)
        verify(jarSigner, timeout(Duration.ofSeconds(10).toMillis()).times(4)).execute(any());
        assertFalse(future.isDone());

        latch.countDown(); // Release the one waiting jar file
        
        // Wait until 10 invocation to execute has happened (nine files are done and one are hanging)
        verify(jarSigner, timeout(Duration.ofSeconds(10).toMillis()).times(10)).execute(any());
        
        latch.countDown(); // Release last
        future.get(10, TimeUnit.SECONDS); // Wait for entire Mojo to finish
        assertTrue(future.isDone());
    }
    
    @Test(timeout = 30000) // TODO: change timeout before merge
    public void test10Files1Parallel() throws Exception {
        configuration.put("archiveDirectory", createArchives(10).getPath());
        configuration.put("threadCount", "1");

        CountDownLatch latch = new CountDownLatch(1);
        when(jarSigner.execute(isA(JarSignerSignRequest.class))).then(invocation -> {
            JarSignerSignRequest request = (JarSignerSignRequest) invocation.getArgument(0);
            // Make one jar file wait until some external event happens
            if (request.getArchive().getPath().endsWith("archive2.jar")) {
                latch.await();
            }
            return RESULT_OK;
        });
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        Future<Void> future = executor.submit(() -> {
            mojo.execute();
            return null;
        });

        // Wait until 10 invocation to execute has happened (nine has finished and one is hanging).
        verify(jarSigner, timeout(Duration.ofSeconds(10).toMillis()).times(10)).execute(any());
        assertFalse(future.isDone());

        latch.countDown(); // Release the one waiting jar file
        future.get(10, TimeUnit.SECONDS); // Wait for entire Mojo to finish
        assertTrue(future.isDone());
    }

    private File createArchives(int numberOfArchives) throws IOException {
        File archiveDirectory = new File(projectDir, "my_archive_dir");
        archiveDirectory.mkdir();
        for (int i = 0; i < numberOfArchives; i++) {
            TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive" + i + ".jar"));
        }
        return archiveDirectory;
    }

    private static ThreadFactory namedThreadFactory(String threadNamePrefix) {
        return r -> new Thread(r, threadNamePrefix + "-Thread");
    }
}
