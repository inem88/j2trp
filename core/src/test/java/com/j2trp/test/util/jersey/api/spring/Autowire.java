package com.j2trp.test.util.jersey.api.spring;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * This annotation provides autowiring capabilities for users that use spring 2.0
 * but that want to get their beans autowired.
 * <p>
 * Autowiring is performed via {@link AutowireCapableBeanFactory#createBean(Class, int, boolean)}
 * to have a fully initialized bean, including applied BeanPostProcessors (in contrast to
 * {@link AutowireCapableBeanFactory#autowire(java.lang.Class, int, boolean)}).<br/>
 * The parameters <em>autowiring mode</em> and <em>dependencyCheck</em> when invoking
 * {@link AutowireCapableBeanFactory#createBean(Class, int, boolean)} are used as specified
 * with this annotation.
 * </p>
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Autowire {
    
    /**
     * The autowiring mode to use.
     * @return One of {@link AutowireMode}, {@link AutowireMode#AUTODETECT} by default.
     */
    AutowireMode mode() default AutowireMode.AUTODETECT;
    
    /**
     * Whether to perform a dependency check for objects (not applicable to autowiring a constructor, thus ignored there).
     * @return true or false, false by default.
     */
    boolean dependencyCheck() default false;
    
}
