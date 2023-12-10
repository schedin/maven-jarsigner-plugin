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
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.stubbing.Answer;

import static org.apache.maven.plugins.jarsigner.TestJavaToolResults.RESULT_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        mojoTestCreator =
                new MojoTestCreator<JarsignerSignMojo>(JarsignerSignMojo.class, project, projectDir, jarSigner);
        executor = Executors.newSingleThreadExecutor(namedThreadFactory(getClass().getSimpleName()));
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test(timeout = 600000) // TODO: change timeout before merge
    public void test10Files2Parallel() throws Exception {
        File archiveDirectory = new File(projectDir, "my_archive_dir");
        archiveDirectory.mkdir();
        for (int i = 0; i < 10; i++) {
            TestArtifacts.createDummyZipFile(new File(archiveDirectory, "archive" + i + ".jar"));
        }
        configuration.put("processMainArtifact", "false");
        configuration.put("archiveDirectory", archiveDirectory.getPath());
        configuration.put("threadCount", "2");

        CountDownLatch latch = new CountDownLatch(1);
        when(jarSigner.execute(isA(JarSignerSignRequest.class))).then(invocation -> {
            JarSignerSignRequest request = (JarSignerSignRequest) invocation.getArgument(0);
            if (request.getArchive().getPath().endsWith("archive2.jar")) {
                latch.await(); //Make one jar file wait until some external event happens
            }
            return RESULT_OK;
        });

        configuration.put("threadCount", "2");
        JarsignerSignMojo mojo = mojoTestCreator.configure(configuration);

        Future<Void> future = executor.submit(() -> {
            mojo.execute();
            return null;
        });

        verify(jarSigner, timeout(Duration.ofSeconds(10).toMillis()).times(9)).execute(any());

        latch.countDown(); // Release the one waiting jar file

        future.get(600, TimeUnit.SECONDS); // TODO: change timeout before merge
        verify(jarSigner, times(10)).execute(any());

    }

    private static ThreadFactory namedThreadFactory(String threadNamePrefix) {
        return r -> new Thread(r, threadNamePrefix + "-Thread");
    }
}
