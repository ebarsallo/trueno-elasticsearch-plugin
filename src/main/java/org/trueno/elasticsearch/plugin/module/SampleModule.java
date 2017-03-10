package org.trueno.elasticsearch.plugin.module;

import org.trueno.elasticsearch.plugin.service.SampleService;
import org.elasticsearch.common.inject.AbstractModule;

public class SampleModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SampleService.class).asEagerSingleton();
    }
}