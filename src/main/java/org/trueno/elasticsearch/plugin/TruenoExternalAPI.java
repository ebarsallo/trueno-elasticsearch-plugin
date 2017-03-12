package org.trueno.elasticsearch.plugin;

import java.util.Collection;

import org.elasticsearch.common.settings.Settings;
import org.trueno.elasticsearch.plugin.module.ExampleRestModule;
import org.trueno.elasticsearch.plugin.module.ExternalAPIModule;
import org.trueno.elasticsearch.plugin.service.APIService;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

import com.google.common.collect.Lists;

public class TruenoExternalAPI extends Plugin {


    private final Settings settings;

    public TruenoExternalAPI(Settings settings) {
        this.settings = settings;

    }

    @Override
    public String name() {
        return "TruenoElasticSearchPlugin";
    }

    @Override
    public String description() {
        return "Trueno ElasticSearch External API Plugin.";
    }

    @Override
    public Collection<Module> nodeModules() {
        final Collection<Module> modules = Lists.newArrayList();
        modules.add(new ExternalAPIModule());
        modules.add(new ExampleRestModule());
        return modules;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        final Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(APIService.class);
        return services;
    }
}
