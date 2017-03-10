package org.trueno.elasticsearch.plugin;

import java.util.Collection;

import org.trueno.elasticsearch.plugin.module.SampleModule;
import org.trueno.elasticsearch.plugin.rest.SampleRestAction;
import org.trueno.elasticsearch.plugin.service.SampleService;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;

import com.google.common.collect.Lists;

public class SamplePlugin extends Plugin {
    @Override
    public String name() {
        return "SamplePlugin";
    }

    @Override
    public String description() {
        return "This is a sample plugin.";
    }

    public void onModule(final RestModule module) {
        module.addRestAction(SampleRestAction.class);
    }

    @Override
    public Collection<Module> nodeModules() {
        final Collection<Module> modules = Lists.newArrayList();
        modules.add(new SampleModule());
        return modules;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        final Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(SampleService.class);
        return services;
    }
}
