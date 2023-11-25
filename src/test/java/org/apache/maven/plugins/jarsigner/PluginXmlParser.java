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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PluginXmlParser {
    private static final String IMPLEMENTATION_TAG = "implementation";
    private static final String MOJO_TAG = "mojo";
    private static final String CONF_DEFAULT_VALUE = "default-value";

    public static void setDefaultValues(Mojo mojo) throws Exception {
        InputStream inputStream = PluginXmlParser.class.getClassLoader().getResourceAsStream("META-INF/maven/plugin.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();

        Element mojoElement = findMojoByClass(doc, mojo.getClass().getName());

        if (mojoElement != null) {
            Element configurationElement = (Element) mojoElement.getElementsByTagName("configuration").item(0);
            
            NodeList configurationList = configurationElement.getChildNodes();
            for (int i = 0; i < configurationList.getLength(); i++) {
                Node child = configurationList.item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                configurationElement = (Element) child;
                String configurationParameterName = configurationElement.getTagName();
                
                List<Field> fields = getAllFields(mojo.getClass());
                
                if (configurationElement.hasAttribute(CONF_DEFAULT_VALUE)) {
                    String defaultValue = configurationElement.getAttribute(CONF_DEFAULT_VALUE);
                    Field field = fields.stream().filter(f -> f.getName().equals(configurationParameterName))
                        .findFirst().orElseThrow(() -> new RuntimeException("Could not find field "
                            + configurationParameterName + " in class " + mojo.getClass().getName()));
                    
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    if (fieldType.equals(String.class)) {
                        field.set(mojo, defaultValue.toString());
                    } else if (fieldType.equals(int.class)) {
                        field.setInt(mojo, Integer.parseInt(defaultValue));
                    } else if (fieldType.equals(boolean.class)) {
                        field.setBoolean(mojo, Boolean.parseBoolean(defaultValue));
                    }
                }
            }
        } else {
            throw new RuntimeException("Mojo not found for class: " + mojo.getClass().getName());
        }
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            fields.addAll(Arrays.asList(currentClazz.getDeclaredFields()));
            currentClazz = currentClazz.getSuperclass();
        }
        return fields;
    }
    
    
    private static Element findMojoByClass(Document doc, String className) {
        NodeList mojoList = doc.getElementsByTagName(MOJO_TAG);
        for (int i = 0; i < mojoList.getLength(); i++) {
            Element mojoElement = (Element) mojoList.item(i);
            String mojoClass = getTextContent(mojoElement, IMPLEMENTATION_TAG);
            if (mojoClass.equals(className)) {
                return mojoElement;
            }
        }
        return null;
    }

    private static String getTextContent(Element parentElement, String tagName) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
}
