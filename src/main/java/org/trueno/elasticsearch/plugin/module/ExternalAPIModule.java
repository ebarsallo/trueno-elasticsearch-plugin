package org.trueno.elasticsearch.plugin.module;

import org.trueno.elasticsearch.plugin.service.APIService;
import org.elasticsearch.common.inject.AbstractModule;

public class ExternalAPIModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(APIService.class).asEagerSingleton();
    }
}