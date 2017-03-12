package org.trueno.elasticsearch.plugin.module;

import org.elasticsearch.common.inject.AbstractModule;
import org.trueno.elasticsearch.plugin.rest.HelloRestHandler;

public class ExampleRestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HelloRestHandler.class).asEagerSingleton();
    }
}
