package org.apache.maven.plugins.jarsigner;

import java.lang.reflect.InvocationTargetException;

import org.apache.maven.project.MavenProject;

public class MojoTestCreator {
    
    public static <T> T create(Class<T> clazz, MavenProject project) throws Exception {
        T object = clazz.getDeclaredConstructor().newInstance();
        
        PluginXmlParser.getMojo(clazz);
        
        return object;
    }

}
