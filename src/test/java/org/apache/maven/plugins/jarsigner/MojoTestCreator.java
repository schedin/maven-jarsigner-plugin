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


import java.lang.reflect.Field;
import java.util.List;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.mockito.Mockito;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class MojoTestCreator {
    
    public static <T extends Mojo> T create(Class<T> clazz, MavenProject project, JarSigner jarSigner) throws Exception {
        T mojo = clazz.getDeclaredConstructor().newInstance();
        PluginXmlParser.setDefaultValues(mojo);
        setAttribute(mojo, "project", project);
        setAttribute(mojo, "jarSigner", jarSigner);
        
        // SecDispatcher that only returns passed value
        SecDispatcher securityDispatcher = Mockito.mock(SecDispatcher.class);
        Mockito.when(securityDispatcher.decrypt(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        setAttribute(mojo, "securityDispatcher", securityDispatcher);
        return mojo;
    }

    public static void setAttribute(Object instance, String fieldName, Object value) throws Exception {
        List<Field> fields = PluginXmlParser.getAllFields(instance.getClass());
       
        Field field = fields.stream().filter(f -> f.getName().equals(fieldName))
                        .findFirst().orElseThrow(() -> new RuntimeException("Could not find field "
                            + fieldName + " in class " + instance.getClass().getName()));
        field.setAccessible(true);
        field.set(instance, value);
    }
}
