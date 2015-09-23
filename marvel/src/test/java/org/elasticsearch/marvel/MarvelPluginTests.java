/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.marvel;

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.LicensePlugin;
import org.elasticsearch.marvel.agent.AgentService;
import org.elasticsearch.marvel.test.MarvelIntegTestCase;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginInfo;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.tribe.TribeService;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = TEST, transportClientRatio = 0, numClientNodes = 0, numDataNodes = 0)
public class MarvelPluginTests extends MarvelIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .build();
    }

    @Test
    public void testMarvelEnabled() {
        internalCluster().startNode(Settings.builder().put(MarvelPlugin.ENABLED, true).build());
        assertPluginIsLoaded();
        assertServiceIsBound(AgentService.class);
    }

    @Test
    public void testMarvelDisabled() {
        internalCluster().startNode(Settings.builder().put(MarvelPlugin.ENABLED, false).build());
        assertPluginIsLoaded();
        assertServiceIsNotBound(AgentService.class);
    }

    @Test
    public void testMarvelDisabledOnTribeNode() {
        internalCluster().startNode(Settings.builder().put(TribeService.TRIBE_NAME, "t1").build());
        assertPluginIsLoaded();
        assertServiceIsNotBound(AgentService.class);
    }

    private void assertPluginIsLoaded() {
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().setPlugins(true).get();
        for (NodeInfo nodeInfo : response) {
            assertNotNull(nodeInfo.getPlugins());

            boolean found = false;
            for (PluginInfo plugin : nodeInfo.getPlugins().getInfos()) {
                assertNotNull(plugin);

                if (MarvelPlugin.NAME.equals(plugin.getName())) {
                    found = true;
                    break;
                }
            }
            assertThat("marvel plugin not found", found, equalTo(true));
        }
    }

    private void assertServiceIsBound(Class klass) {
        try {
            Object binding = internalCluster().getDataNodeInstance(klass);
            assertNotNull(binding);
            assertTrue(klass.isInstance(binding));
        } catch (Exception e) {
            fail("no service bound for class " + klass.getSimpleName());
        }
    }

    private void assertServiceIsNotBound(Class klass) {
        try {
            internalCluster().getDataNodeInstance(klass);
            fail("should have thrown an exception about missing implementation");
        } catch (Exception ce) {
            assertThat("message contains error about missing implemention: " + ce.getMessage(),
                    ce.getMessage().contains("No implementation"), equalTo(true));
        }
    }
}
