package org.apache.maven.plugins.jarsigner;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.apache.maven.plugins.jarsigner.TsaSelector.TsaServer;
import org.junit.Test;

public class TsaSelectorTest {
    
    private TsaSelector tsaSelector;
    private TsaServer tsaServer;

    @Test
    public void testNullInit() {
        tsaSelector = new TsaSelector(null, null, null, null);
        tsaServer = tsaSelector.getServer();
        assertNull(tsaServer.getTsaUrl());
        assertNull(tsaServer.getTsaAlias());
        assertNull(tsaServer.getTsaPolicyId());
        assertNull(tsaServer.getTsaDigestAlt());

        //Make sure "next" server also contains null values
        tsaServer = tsaSelector.getServer();
        assertNull(tsaServer.getTsaUrl());
        assertNull(tsaServer.getTsaAlias());
        assertNull(tsaServer.getTsaPolicyId());
        assertNull(tsaServer.getTsaDigestAlt());
    }

    @Test
    public void testFailureCount() {
        tsaSelector = new TsaSelector(
                new String[]{"http://url1.com", "http://url2.com", "http://url2.com"}, null, null, null);

        tsaServer = tsaSelector.getServer();
        assertEquals("http://url1.com", tsaServer.getTsaUrl());
        assertNull(tsaServer.getTsaAlias());
        assertNull(tsaServer.getTsaPolicyId());
        assertNull(tsaServer.getTsaDigestAlt());
        
        tsaSelector.registerFailure();
        
        tsaServer = tsaSelector.getServer();
        assertEquals("http://url2.com", tsaServer.getTsaUrl());
        assertNull(tsaServer.getTsaAlias());
        assertNull(tsaServer.getTsaPolicyId());
        assertNull(tsaServer.getTsaDigestAlt());

        //Should get same server again
        tsaServer = tsaSelector.getServer();
        assertEquals("http://url2.com", tsaServer.getTsaUrl());
        assertNull(tsaServer.getTsaAlias());
        assertNull(tsaServer.getTsaPolicyId());
        assertNull(tsaServer.getTsaDigestAlt());
    }
}
