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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class MojoTestCreator<T extends Mojo> {

    private final Class<T> clazz;
    private final MavenProject project;
    private final File projectDir;
    private final JarSigner jarSigner;
    private List<Field> fields;

    public MojoTestCreator(Class<T> clazz, MavenProject project, File projectDir, JarSigner jarSigner) throws Exception {
        this.clazz = clazz;
        this.project = project;
        this.projectDir = projectDir;
        this.jarSigner = jarSigner;
        fields = getAllFields(clazz);
    }

    public T configure() throws Exception {
        T mojo = clazz.getDeclaredConstructor().newInstance();
        setDefaultValues(mojo);

        setAttribute(mojo, "project", project);
        setAttribute(mojo, "jarSigner", jarSigner);

        SecDispatcher securityDispatcher = str -> str; //Simple SecDispatcher that only returns parameter
        setAttribute(mojo, "securityDispatcher", securityDispatcher);
        return mojo;
    }

    public void setAttribute(Object instance, String fieldName, Object value) throws Exception {
        Field field = fields.stream().filter(f -> f.getName().equals(fieldName))
                        .findFirst().orElseThrow(() -> new RuntimeException("Could not find field "
                            + fieldName + " in class " + instance.getClass().getName()));
        field.setAccessible(true);
        field.set(instance, value);
    }

    private void setDefaultValues(Mojo mojo) throws Exception {
        Map<String, String> defaultConfiguration = PluginXmlParser.getMojoDefaultConfiguration(mojo);
        for (String parameterName : defaultConfiguration.keySet()) {
            String defaultValue = defaultConfiguration.get(parameterName);
            defaultValue = substituteParameterValueVariables(defaultValue);

            Field field = fields.stream().filter(f -> f.getName().equals(parameterName))
                .findFirst().orElseThrow(() -> new RuntimeException("Could not find field "
                    + parameterName + " in class " + mojo.getClass().getName()));

            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            if (fieldType.equals(String.class)) {
                field.set(mojo, defaultValue.toString());
            } else if (fieldType.equals(int.class)) {
                field.setInt(mojo, Integer.parseInt(defaultValue));
            } else if (fieldType.equals(boolean.class)) {
                field.setBoolean(mojo, Boolean.parseBoolean(defaultValue));
            } else if (fieldType.equals(File.class)) {
                field.set(mojo, new File(defaultValue));
            }
        };
    }

    private String substituteParameterValueVariables(String parameterValue) {
        parameterValue = parameterValue.replaceAll(Pattern.quote("${project.basedir}"),
            Matcher.quoteReplacement(projectDir.getPath()));
        return parameterValue;
    }
    
    static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            fields.addAll(Arrays.asList(currentClazz.getDeclaredFields()));
            currentClazz = currentClazz.getSuperclass();
        }
        return fields;
    }
}
