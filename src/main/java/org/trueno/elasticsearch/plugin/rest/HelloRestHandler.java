package org.trueno.elasticsearch.plugin.rest;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.BytesRestResponse;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.RestRequest.Method.GET;

public class HelloRestHandler implements RestHandler {

    @Inject
    public HelloRestHandler(RestController restController) {
        restController.registerHandler(GET, "/_hello", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel) throws Exception {
        String who = request.param("who");
        String whoSafe = (who != null) ? who : "world";
        channel.sendResponse(new BytesRestResponse(OK, "Hello, " + whoSafe + "!"));
    }
}
