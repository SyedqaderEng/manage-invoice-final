package com.manageinvoice.testsupport;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.manageinvoice.common.security.TestSupabaseTokenVerifier;
import com.manageinvoice.documents.InMemoryObjectStorage;

/**
 * For the "test" profile, swaps the Supabase Auth verifier and Supabase Storage client for in-process
 * fakes, so the whole application context starts and runs without network access to Supabase.
 */
@Component
@Profile("test")
class SupabaseTestDoubles implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        replace(registry, "supabaseTokenVerifier", TestSupabaseTokenVerifier.class);
        replace(registry, "supabaseObjectStorage", InMemoryObjectStorage.class);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Nothing to do; definitions are swapped above.
    }

    private static void replace(BeanDefinitionRegistry registry, String name, Class<?> type) {
        if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
        registry.registerBeanDefinition(name, new RootBeanDefinition(type));
    }
}
