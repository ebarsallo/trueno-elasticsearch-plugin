package org.trueno.elasticsearch.plugin.service;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.util.Map;

public class APIService extends AbstractLifecycleComponent<APIService> {

    private SocketIOServer server;
    private String hostname;
    private int port;

    @Inject
    public APIService(final Settings settings) {
        super(settings);
        logger.info("CREATE APIService");

        this.port = Integer.parseInt(settings.get("trueno.api.port"));
        this.hostname = settings.get("trueno.api.hostname");

        // TODO Your code..
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        logger.info("START APIService");

        /* instantiate the configuration */
        Configuration config = new Configuration();

        /* set the listening hostname */
        config.setHostname(this.hostname);

        /* set the listening port */
        config.setPort(this.port);

        /* instantiate the server */
        final SocketIOServer server = new SocketIOServer(config);
        this.server = server;

        /* set search event listener */
        server.addEventListener("search", SearchObject.class, new DataListener<SearchObject>() {
            @Override
            public void onData(SocketIOClient client, SearchObject data, AckRequest ackRequest) {

            }
        });
        /* set bulk event listener */
        server.addEventListener("bulk", BulkObject.class, new DataListener<BulkObject>() {
            @Override
            public void onData(SocketIOClient client, BulkObject data, AckRequest ackRequest) {

            }
        });

        /* starting socket server */
        server.startAsync().addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (future.isSuccess()) {
                    logger.info("Trueno ElasticSearch API Service started");
                } else {
                    logger.info("ERROR: Trueno ElasticSearch API Service unable to start");
                }
            }
        });


    }

    @Override
    protected void doStop() throws ElasticsearchException {
        logger.info("STOP APIService");

        // TODO Your code..
        this.server.stop();
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        logger.info("CLOSE APIService");

        // TODO Your code..
    }

}
