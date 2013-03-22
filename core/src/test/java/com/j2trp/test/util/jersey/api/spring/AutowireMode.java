package com.j2trp.test.util.jersey.api.spring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * This enumerations encapsulates the autowiring modes provided by the
 * {@link AutowireCapableBeanFactory}.
 * 
 * @author <a href="mailto:martin.grotzke@freiheit.com">Martin Grotzke</a>
 */
public enum AutowireMode {
    
    @SuppressWarnings("deprecation")
    AUTODETECT( AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT ),
    BY_NAME ( AutowireCapableBeanFactory.AUTOWIRE_BY_NAME ),
    BY_TYPE ( AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE ),
    CONSTRUCTOR ( AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR );
    
    private final int _springCode;
    
    private AutowireMode( int mode ) {
        _springCode = mode;
    }
    
    public int getSpringCode() {
        return _springCode;
    }
    
}
