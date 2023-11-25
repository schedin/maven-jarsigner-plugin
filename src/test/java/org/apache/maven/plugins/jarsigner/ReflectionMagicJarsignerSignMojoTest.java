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


import org.apache.maven.plugin.Mojo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ReflectionMagicJarsignerSignMojoTest {

    private JarsignerSignMojo mojo;

    @Before
    public void setUp() {
        mojo = new JarsignerSignMojo();
        MojoTestUtils.initDefaultParameters(mojo);
        
    }

    @Test
    public void test() throws Exception {
        mojo.execute();
    }
    
    private static class MojoTestUtils {
        private static void initDefaultParameters(Mojo mojo) {
            for (Field field : getAllFields(mojo.getClass())) {
                
                if (! field.getName().equals("alias")) {
                    continue;
                }
                
                System.out.println(field.getName());
                for (Annotation annotation : field.getAnnotations()) {
                    System.out.println(annotation);
                }
            }
        }

        private static List<Field> getAllFields(Class<?>clazz) {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClazz = clazz;
            while (currentClazz != null) {
                fields.addAll(Arrays.asList(currentClazz.getDeclaredFields()));
                currentClazz = currentClazz.getSuperclass();
            }
            return fields;
            
        }
        

    }
}
