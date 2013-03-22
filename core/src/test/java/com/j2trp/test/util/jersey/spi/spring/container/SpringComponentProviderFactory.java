package com.j2trp.test.util.jersey.spi.spring.container;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;


import com.j2trp.test.util.jersey.api.spring.Autowire;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCInstantiatedComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCManagedComponentProvider;
import com.sun.jersey.spi.inject.Inject;

/**
 * The Spring-based {@link IoCComponentProviderFactory}.
 * <p>
 * Resource and provider classes can be registered Spring-based beans using
 * XML-based registration or auto-wire-based registration.
 */
public class SpringComponentProviderFactory implements IoCComponentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(SpringComponentProviderFactory.class.getName());
    
    private final ConfigurableApplicationContext springContext;

    public SpringComponentProviderFactory(ResourceConfig rc, ConfigurableApplicationContext springContext) {
        this.springContext = springContext;
        register(rc, springContext);
    }

    private void register(ResourceConfig rc, ConfigurableApplicationContext springContext) {
        String[] names = BeanFactoryUtils.beanNamesIncludingAncestors(springContext);
        for (String name : names) {
            Class<?> type = ClassUtils.getUserClass(springContext.getType(name));
            if (ResourceConfig.isProviderClass(type)) {
                LOGGER.info("Registering Spring bean, " + name +
                        ", of type " + type.getName() +
                        " as a provider class");
                rc.getClasses().add(type);
            } else if (ResourceConfig.isRootResourceClass(type)) {
                LOGGER.info("Registering Spring bean, " + name +
                        ", of type " + type.getName() +
                        " as a root resource class");
                rc.getClasses().add(type);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public IoCComponentProvider getComponentProvider(Class c) {
        return getComponentProvider(null, c);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public IoCComponentProvider getComponentProvider(ComponentContext cc, Class c) {
        final Autowire autowire = (Autowire) c.getAnnotation(Autowire.class);
        if (autowire != null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Creating resource class " +
                        c.getSimpleName() +
                        " annotated with @" +
                        Autowire.class.getSimpleName() +
                        " as spring bean.");
            }
            return new SpringInstantiatedComponentProvider(c, autowire);
        }

        final String beanName = getBeanName(cc, c, springContext);
        if (beanName == null) {
            return null;
        }

        final String scope = findBeanDefinition(beanName).getScope();
        return new SpringManagedComponentProvider(getComponentScope(scope), beanName, c);
    }

    /**
     * Fine the bean definition from a given context or from any of the parent
     * contexts.
     *
     * @param beanName the bean name.
     * @return the bean definition.
     * @throws NoSuchBeanDefinitionException if the bean definition could not
     *         be found.
     */
    private BeanDefinition findBeanDefinition(String beanName) {
        ConfigurableApplicationContext current = springContext;
        BeanDefinition beanDef = null;
        do {
            try {
                return current.getBeanFactory().getBeanDefinition(beanName);
            } catch (NoSuchBeanDefinitionException e) {
                final ApplicationContext parent = current.getParent();
                if (parent != null && parent instanceof ConfigurableApplicationContext) {
                    current = (ConfigurableApplicationContext) parent;
                } else {
                    throw e;
                }
            }
        } while (beanDef == null && current != null);
        return beanDef;
    }

    private ComponentScope getComponentScope(String scope) {
        ComponentScope cs = scopeMap.get(scope);
        return (cs != null) ? cs : ComponentScope.Undefined;
    }

    private final Map<String, ComponentScope> scopeMap = createScopeMap();

    private Map<String, ComponentScope> createScopeMap() {
        Map<String, ComponentScope> m = new HashMap<String, ComponentScope>();
        m.put(BeanDefinition.SCOPE_SINGLETON, ComponentScope.Singleton);
        m.put(BeanDefinition.SCOPE_PROTOTYPE, ComponentScope.PerRequest);
        m.put("request", ComponentScope.PerRequest);
        return m;
    }

    private class SpringInstantiatedComponentProvider implements IoCInstantiatedComponentProvider {

        @SuppressWarnings("rawtypes")
        private final Class c;
        private final Autowire a;

        @SuppressWarnings("rawtypes")
        SpringInstantiatedComponentProvider(Class c, Autowire a) {
            this.c = c;
            this.a = a;
        }

        public Object getInstance() {
            return springContext.getBeanFactory().createBean(c,
                    a.mode().getSpringCode(), a.dependencyCheck());
        }

        public Object getInjectableInstance(Object o) {
            return SpringComponentProviderFactory.getInjectableInstance(o);
        }
    }

    private class SpringManagedComponentProvider implements IoCManagedComponentProvider {

        private final ComponentScope scope;
        private final String beanName;
        @SuppressWarnings("rawtypes")
        private final Class c;

        @SuppressWarnings("rawtypes")
        SpringManagedComponentProvider(ComponentScope scope, String beanName, Class c) {
            this.scope = scope;
            this.beanName = beanName;
            this.c = c;
        }

        public ComponentScope getScope() {
            return scope;
        }

        public Object getInjectableInstance(Object o) {
            return SpringComponentProviderFactory.getInjectableInstance(o);
        }

        @SuppressWarnings("unchecked")
        public Object getInstance() {
            return springContext.getBean(beanName, c);
        }
    }

    private static Object getInjectableInstance(Object o) {
        if (AopUtils.isAopProxy(o)) {
            final Advised aopResource = (Advised) o;
            try {
                return aopResource.getTargetSource().getTarget();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not get target object from proxy.", e);
                throw new RuntimeException("Could not get target object from proxy.", e);
            }
        } else {
            return o;
        }
    }

    private static String getBeanName(ComponentContext cc, Class<?> c, ApplicationContext springContext) {
        boolean annotatedWithInject = false;
        if (cc != null) {
            final Inject inject = getAnnotation(cc.getAnnotations(), Inject.class);
            if (inject != null) {
                annotatedWithInject = true;
                if (inject.value() != null && !inject.value().equals("")) {
                    return inject.value();
                }

            }
        }

        final String names[] = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(springContext, c);

        if (names.length == 0) {
            return null;
        } else if (names.length == 1) {
            return names[0];
        } else {
            // Check if types of the beans names are assignable
            // Spring auto-registration for a type A will include the bean
            // names for classes that extend A
            boolean inheritedNames = false;
            String beanName = null;
            for (String name : names) {
                Class<?> beanType = ClassUtils.getUserClass(springContext.getType(name));

                inheritedNames = c.isAssignableFrom(beanType);

                if (c == beanType)
                    beanName = name;
            }

            if (inheritedNames) {
                if (beanName != null)
                    return beanName;
            }
            
            final StringBuilder sb = new StringBuilder();
            sb.append("There are multiple beans configured in spring for the type ").
                    append(c.getName()).append(".");

            if (annotatedWithInject) {
                sb.append("\nYou should specify the name of the preferred bean at @Inject: Inject(\"yourBean\").");
            } else {
                sb.append("\nAnnotation information was not available, the reason might be because you're not using " +
                        "@Inject. You should use @Inject and specifiy the bean name via Inject(\"yourBean\").");
            }

            sb.append("\nAvailable bean names: ").append(toCSV(names));

            throw new RuntimeException(sb.toString());
        }
    }

    private static <T extends Annotation> T getAnnotation(Annotation[] annotations,
            Class<T> clazz) {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(clazz)) {
                    return clazz.cast(annotation);
                }
            }
        }
        return null;
    }

    private static <T> String toCSV(T[] items) {
        if (items == null) {
            return null;
        }
        return toCSV(Arrays.asList(items));
    }

    private static <I> String toCSV(Collection<I> items) {
        return toCSV(items, ", ", null);
    }

    private static <I> String toCSV(Collection<I> items, String separator, String delimiter) {
        if (items == null) {
            return null;
        }
        if (items.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Iterator<I> iter = items.iterator(); iter.hasNext();) {
            if (delimiter != null) {
                sb.append(delimiter);
            }
            final I item = iter.next();
            sb.append(item);
            if (delimiter != null) {
                sb.append(delimiter);
            }
            if (iter.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }
}
