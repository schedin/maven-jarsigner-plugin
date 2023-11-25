package org.apache.maven.plugins.jarsigner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

public class PluginXmlParser {

    public static void getMojo(Class<?> mojoClass) {
        try {
            InputStream inputStream = PluginXmlParser.class.getClassLoader().getResourceAsStream("META-INF/maven/plugin.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();

            Element mojoElement = findMojoByClass(doc, mojoClass.getName());

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
                    if (configurationElement.hasAttribute("default-value")) {
                        String defaultValue = configurationElement.getAttribute("default-value");
                        System.out.println(configurationParameterName + " = " + defaultValue);
                    }
                    
                    //System.out.println(configurationElement.getAttribute("lennart"));
                    
                    //System.out.println(configurationElement);
                    
                }
                
                System.out.println(configurationElement);
                // Extract information about the Mojo
//                String mojoName = getTextContent(mojoElement, "name");
//                System.out.println("Found Mojo: " + mojoName);
            } else {
                throw new RuntimeException("Mojo not found for class: " + mojoClass.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Element findMojoByClass(Document doc, String className) {
        NodeList mojoList = doc.getElementsByTagName("mojo");
        for (int i = 0; i < mojoList.getLength(); i++) {
            Element mojoElement = (Element) mojoList.item(i);
            String mojoClass = getTextContent(mojoElement, "implementation");
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
