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

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;
import org.junit.Test;

public class MyProjectStub
extends MavenProjectStub
{
/**
 * Default constructor
 */
public MyProjectStub()
{
    MavenXpp3Reader pomReader = new MavenXpp3Reader();
    Model model;
    try
    {
        model = pomReader.read( ReaderFactory.newXmlReader( new File( getBasedir(), "pom.xml" ) ) );
        setModel( model );
    }
    catch ( Exception e )
    {
        throw new RuntimeException( e );
    }

    setGroupId( model.getGroupId() );
    setArtifactId( model.getArtifactId() );
    setVersion( model.getVersion() );
    setName( model.getName() );
    setUrl( model.getUrl() );
    setPackaging( model.getPackaging() );

    Build build = new Build();
    build.setFinalName( model.getArtifactId() );
    build.setDirectory( getBasedir() + "/target" );
    build.setSourceDirectory( getBasedir() + "/src/main/java" );
    build.setOutputDirectory( getBasedir() + "/target/classes" );
    build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
    build.setTestOutputDirectory( getBasedir() + "/target/test-classes" );
    setBuild( build );

    List compileSourceRoots = new ArrayList();
    compileSourceRoots.add( getBasedir() + "/src/main/java" );
    setCompileSourceRoots( compileSourceRoots );

    List testCompileSourceRoots = new ArrayList();
    testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
    setTestCompileSourceRoots( testCompileSourceRoots );
}

/** {@inheritDoc} */
public File getBasedir()
{
    return new File( super.getBasedir() + "/src/test/resources/unit/project-to-test/" );
}
}