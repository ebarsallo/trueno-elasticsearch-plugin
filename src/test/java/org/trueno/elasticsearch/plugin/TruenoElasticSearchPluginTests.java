package org.trueno.elasticsearch.plugin;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

import java.net.InetSocketAddress;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.sort.SortBuilders;

import junit.framework.TestCase;

public class TruenoElasticSearchPluginTests extends TestCase {

    private ElasticsearchClusterRunner runner;

    @Override
    protected void setUp() throws Exception {
        // create runner instance
        runner = new ElasticsearchClusterRunner();
        // create ES nodes
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("trueno.api.hostname", "localhost");
                settingsBuilder.put("trueno.api.port", 8008);
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
                settingsBuilder.put("plugin.types", "org.trueno.elasticsearch.plugin.TruenoExternalAPI");
            }
        }).build(newConfigs().clusterName("es-cl-run-" + System.currentTimeMillis()).numOfNode(1));

        // wait for yellow status
        runner.ensureYellow();
    }

    @Override
    protected void tearDown() throws Exception {
        // close runner
        runner.close();
        // delete all files
        runner.clean();
    }

    /**
     * Test web socket connection
     * @throws Exception
     */
    public void test_plugin() throws Exception {

        // check if runner has nodes
        assertEquals(1, runner.getNodeSize());
        assertNotNull(runner.getNode(0));
//        assertNotNull(runner.getNode(1));
//        assertNotNull(runner.getNode(2));
        assertNotNull(runner.getNode("Node 1"));
//        assertNotNull(runner.getNode("Node 2"));
//        assertNotNull(runner.getNode("Node 3"));
        assertNull(runner.getNode("Node 4"));
        assertNotNull(runner.node());

        assertNotNull(runner.client());

        // check if a master node exists
//        assertNotNull(runner.masterNode());
//        assertNotNull(runner.nonMasterNode());
//        assertFalse(runner.masterNode() == runner.nonMasterNode());

        // check if a cluster service exists
        assertNotNull(runner.clusterService());

        final String index = "test_index";
        final String type = "test_type";

        // create an index
        runner.createIndex(index, null);
        runner.ensureYellow(index);

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject(type)//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "string")//
                .field("index", "not_analyzed")//
                .endObject()//

                // msg
                .startObject("msg")//
                .field("type", "string")//
                .endObject()//

                // order
                .startObject("order")//
                .field("type", "long")//
                .endObject()//

                // @timestamp
                .startObject("@timestamp")//
                .field("type", "date")//
                .endObject()//

                .endObject()//
                .endObject()//
                .endObject();
        runner.createMapping(index, type, mappingBuilder);

        if (!runner.indexExists(index)) {
            fail();
        }

        // create 1000 documents
        for (int i = 1; i <= 1000; i++) {
            final IndexResponse indexResponse1 = runner.insert(index, type,
                    String.valueOf(i), "{\"id\":\"" + i + "\",\"msg\":\"test "
                            + i + "\",\"order\":" + i
                            + ",\"@timestamp\":\"2000-01-01T00:00:00\"}");
            assertTrue(indexResponse1.isCreated());
        }
        runner.refresh();

        final Node node = runner.node();

//        try (CurlResponse curlResponse = Curl.get(node, "/" + index + "/_sample").execute()) {
//            final String content = curlResponse.getContentAsString();
//            assertNotNull(content);
//            assertEquals(index, curlResponse.getContentAsMap().get("index"));
//        }

        // update alias
        final String alias = index + "_alias";
        {
            final GetAliasesResponse aliasesResponse = runner.getAlias(alias);
            assertNull(aliasesResponse.getAliases().get(alias));
        }

        {
            runner.updateAlias(alias, new String[] { index }, null);
            runner.flush();
            final GetAliasesResponse aliasesResponse = runner.getAlias(alias);
            assertEquals(1, aliasesResponse.getAliases().size());
            assertEquals(1, aliasesResponse.getAliases().get(index).size());
            assertEquals(alias, aliasesResponse.getAliases().get(index).get(0)
                    .alias());
        }

        {
            runner.updateAlias(alias, null, new String[] { index });
            final GetAliasesResponse aliasesResponse = runner.getAlias(alias);
            assertNull(aliasesResponse.getAliases().get(alias));
        }

        // search 1000 documents
        {
            final SearchResponse searchResponse = runner.search(index, type,
                    null, null, 0, 10);
            assertEquals(1000, searchResponse.getHits().getTotalHits());
            assertEquals(10, searchResponse.getHits().hits().length);
        }

        {
            final SearchResponse searchResponse = runner.search(index, type,
                    QueryBuilders.matchAllQuery(),
                    SortBuilders.fieldSort("id"), 0, 10);
            assertEquals(1000, searchResponse.getHits().getTotalHits());
            assertEquals(10, searchResponse.getHits().hits().length);
        }

        {
            final SearchResponse searchResponse = runner.count(index, type);
            assertEquals(1000, searchResponse.getHits().getTotalHits());
        }

        // delete 1 document
        runner.delete(index, type, String.valueOf(1));
        runner.flush();

        {
            final SearchResponse searchResponse = runner.search(index, type,
                    null, null, 0, 10);
            assertEquals(999, searchResponse.getHits().getTotalHits());
            assertEquals(10, searchResponse.getHits().hits().length);
        }

        // upgrade
        runner.upgrade();

        // transport client
        final Settings transportClientSettings = Settings.settingsBuilder()
                .put("cluster.name", runner.getClusterName()).build();
        final int port = runner.node().settings()
                .getAsInt("transport.tcp.port", 9300);
        try (TransportClient client = TransportClient.builder()
                .settings(transportClientSettings).build()) {
            client.addTransportAddress(new InetSocketTransportAddress(
                    new InetSocketAddress("localhost", port)));
            final SearchResponse searchResponse = client.prepareSearch(index)
                    .setTypes(type).setQuery(QueryBuilders.matchAllQuery())
                    .execute().actionGet();
            assertEquals(999, searchResponse.getHits().getTotalHits());
            assertEquals(10, searchResponse.getHits().hits().length);
        }

//        // http access
//        // get
//        try (CurlResponse curlResponse = Curl.get(node, "/_search")
//                .param("q", "*:*").execute()) {
//            final String content = curlResponse.getContentAsString();
//            assertNotNull(content);
//            assertTrue(content.contains("total"));
//            final Map<String, Object> map = curlResponse.getContentAsMap();
//            assertNotNull(map);
//            assertEquals("false", map.get("timed_out").toString());
//        }
//
//        // post
//        try (CurlResponse curlResponse = Curl
//                .post(node, "/" + index + "/" + type)
//                .body("{\"id\":\"2000\",\"msg\":\"test 2000\"}").execute()) {
//            final Map<String, Object> map = curlResponse.getContentAsMap();
//            assertNotNull(map);
//            assertEquals("true", map.get("created").toString());
//        }
//
//        // put
//        try (CurlResponse curlResponse = Curl
//                .put(node, "/" + index + "/" + type + "/2001")
//                .body("{\"id\":\"2001\",\"msg\":\"test 2001\"}").execute()) {
//            final Map<String, Object> map = curlResponse.getContentAsMap();
//            assertNotNull(map);
//            assertEquals("true", map.get("created").toString());
//        }
//
//        // delete
//        try (CurlResponse curlResponse = Curl.delete(node,
//                "/" + index + "/" + type + "/2001").execute()) {
//            final Map<String, Object> map = curlResponse.getContentAsMap();
//            assertNotNull(map);
//            assertEquals("true", map.get("found").toString());
//        }
//
//        // post
//        try (CurlResponse curlResponse = Curl
//                .post(node, "/" + index + "/" + type)
//                .onConnect(new CurlRequest.ConnectionBuilder() {
//                    @Override
//                    public void onConnect(CurlRequest curlRequest,
//                            HttpURLConnection connection) {
//                        connection.setDoOutput(true);
//                        try (BufferedWriter writer = new BufferedWriter(
//                                new OutputStreamWriter(connection
//                                        .getOutputStream(), "UTF-8"))) {
//                            writer.write("{\"id\":\"2002\",\"msg\":\"test 2002\"}");
//                            writer.flush();
//                        } catch (IOException e) {
//                            throw new CurlException("Failed to write data.", e);
//                        }
//                    }
//                }).execute()) {
//            final Map<String, Object> map = curlResponse.getContentAsMap();
//            assertNotNull(map);
//            assertEquals("true", map.get("created").toString());
//        }

        // close 1 node
        final Node node1 = runner.node();
        node1.close();
//        final Node node2 = runner.node();
//        assertTrue(node1 != node2);
        assertTrue(runner.getNode(0).isClosed());
//        assertFalse(runner.getNode(1).isClosed());
//        assertFalse(runner.getNode(2).isClosed());

        // restart a node
        assertTrue(runner.startNode(0));
//        assertFalse(runner.startNode(1));
//        assertFalse(runner.startNode(2));

        //runner.ensureGreen();
    }

    /**
     * Test Hello Rest action
     * @throws Exception
     */
    public void test_hello() throws Exception {

        final Node node = runner.node();

        // Saying `hello`
        try (CurlResponse curlResponse = Curl.get(node, "/_hello").execute()) {
            System.out.println("--> " + curlResponse.getContentAsString());
            final String content = curlResponse.getContentAsString();
            assertNotNull(content);
            assertEquals(content, "Hello, world!");
        }

        try (CurlResponse curlResponse = Curl.get(node, "/_hello?who=Elasticsearch").execute()) {
            System.out.println("--> " + curlResponse.getContentAsString());
            final String content = curlResponse.getContentAsString();
            assertNotNull(content);
            assertEquals(content, "Hello, Elasticsearch!");
        }
    }

}
