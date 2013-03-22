package com.j2trp.test.util;

import java.io.IOException;
import java.net.URI;

import com.sun.grizzly.SSLConfig;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.grizzly.standalone.StaticStreamAlgorithm;
import com.sun.grizzly.tcp.Adapter;
import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;

public final class EnhancedGrizzlyServerFactory {
  private EnhancedGrizzlyServerFactory() {}
  
  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that 
   * in turn manages all root resource and provder classes found by searching the classes
   * referenced in the java classath.
   * <p>
   * This implementation defers to the 
   * {@link ContainerFactory#createContainer(Class)} method for creating
   * an Adapter that manages the root resources.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be 
   *        used. The URI path, query and fragment components are ignored.
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(String u)
          throws IOException, IllegalArgumentException {            
      if (u == null)
          throw new IllegalArgumentException("The URI must not be null");

      return create(URI.create(u));
  }
  
  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that 
   * in turn manages all root resource and provder classes found by searching 
   * the classes referenced in the java classath.
   * <p>
   * This implementation defers to the 
   * {@link ContainerFactory#createContainer(Class)} method for creating
   * an Adapter that manages the root resources.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be 
   *        used. The URI path, query and fragment components are ignored.
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(URI u)
          throws IOException, IllegalArgumentException {            
      return create(u, ContainerFactory.createContainer(Adapter.class));
  }
      
  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that
   * in turn manages all root resource and provder classes declared by the
   * resource configuration.
   * <p>
   * This implementation defers to the
   * {@link ContainerFactory#createContainer(Class, ResourceConfig)} method
   * for creating an Adapter that manages the root resources.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be
   *        used. The URI path, query and fragment components are ignored.
   * @param rc the resource configuration.
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(String u, ResourceConfig rc)
          throws IOException, IllegalArgumentException {
      if (u == null)
          throw new IllegalArgumentException("The URI must not be null");

      return create(URI.create(u), rc);
  }

  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that
   * in turn manages all root resource and provder classes declared by the
   * resource configuration.
   * <p>
   * This implementation defers to the
   * {@link ContainerFactory#createContainer(Class, ResourceConfig)} method
   * for creating an Adapter that manages the root resources.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be
   *        used. The URI path, query and fragment components are ignored.
   * @param rc the resource configuration.
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(URI u, ResourceConfig rc)
          throws IOException, IllegalArgumentException {
      return create(u, ContainerFactory.createContainer(Adapter.class, rc));
  }

  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that
   * in turn manages all root resource and provder classes declared by the
   * resource configuration.
   * <p>
   * This implementation defers to the
   * {@link ContainerFactory#createContainer(Class, ResourceConfig, IoCComponentProviderFactory)} method
   * for creating an Adapter that manages the root resources.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be
   *        used. The URI path, query and fragment components are ignored.
   * @param rc the resource configuration.
   * @param factory the IoC component provider factory the web application
   *        delegates to for obtaining instances of resource and provider
   *        classes. May be null if the web application is responsible for
   *        instantiating resource and provider classes.
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(String u, ResourceConfig rc,
          IoCComponentProviderFactory factory)
          throws IOException, IllegalArgumentException {
      if (u == null)
          throw new IllegalArgumentException("The URI must not be null");

      return create(URI.create(u), rc, factory);
  }

  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that
   * in turn manages all root resource and provder classes declared by the
   * resource configuration.
   * <p>
   * This implementation defers to the
   * {@link ContainerFactory#createContainer(Class, ResourceConfig, IoCComponentProviderFactory)} method
   * for creating an Adapter that manages the root resources.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be
   *        used. The URI path, query and fragment components are ignored.
   * @param rc the resource configuration.
   * @param factory the IoC component provider factory the web application
   *        delegates to for obtaining instances of resource and provider
   *        classes. May be null if the web application is responsible for
   *        instantiating resource and provider classes.
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(URI u, ResourceConfig rc,
          IoCComponentProviderFactory factory)
          throws IOException, IllegalArgumentException {
      return create(u, ContainerFactory.createContainer(Adapter.class, rc, factory));
  }

  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that 
   * in turn manages all root resource and provder classes found by searching the classes
   * referenced in the java classath.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be 
   *        used. The URI path, query and fragment components are ignored.
   * @param adapter the Adapter
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null
   */
  public static EnhancedSelectorThread create(String u, Adapter adapter)
          throws IOException, IllegalArgumentException {
      if (u == null)
          throw new IllegalArgumentException("The URI must not be null");

      return create(URI.create(u), adapter);
  }
  
  /**
   * Create a {@link SelectorThread} that registers an {@link Adapter} that 
   * in turn manages all root resource and provder classes found by searching the classes
   * referenced in the java classath.
   *
   * @param u the URI to create the http server. The URI scheme must be
   *        equal to "http". The URI user information and host
   *        are ignored If the URI port is not present then port 80 will be 
   *        used. The URI path will be set as the resources context root
   *        value, which must be an empty String or begin with a "/".
   *        The URI query and fragment components are ignored.
   * @param adapter the Adapter
   * @return the select thread, with the endpoint started
   * @throws IOException if an error occurs creating the container.
   * @throws IllegalArgumentException if <code>u</code> is null or the URI
   *         path does not begin with a "/".
   */
  public static EnhancedSelectorThread create(URI u, Adapter adapter) 
          throws IOException, IllegalArgumentException {
      if (u == null)
          throw new IllegalArgumentException("The URI must not be null");
          
      final String scheme = u.getScheme();
      if (!(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
          throw new IllegalArgumentException("The URI scheme, of the URI " + u + 
                  ", must be equal (ignoring case) to 'http' or 'https'");            

      
      
      if (adapter instanceof GrizzlyAdapter) {
          GrizzlyAdapter ga = (GrizzlyAdapter)adapter;
          ga.setResourcesContextPath(u.getRawPath());
      }
      
      EnhancedSelectorThread selectorThread;
      if (scheme.equalsIgnoreCase("https")) {
        SSLConfig sslConfig = new SSLConfig();
        String keystoreFile = ((ServletAdapter)adapter).getProperty("ssl.keystoreFile").toString(); // NPE
        char[] keystorePass = (char[]) ((ServletAdapter)adapter).getProperty("ssl.keystorePass");
        sslConfig.setKeyStoreFile(keystoreFile); 
        sslConfig.setKeyStorePass(keystorePass);
        EnhancedSslSelectorThread sslSelectorThread = new EnhancedSslSelectorThread();
        sslSelectorThread.setSSLConfig(sslConfig);
        selectorThread = sslSelectorThread;
      }
      else {
        selectorThread = new EnhancedPlainSelectorThread();
      }

      selectorThread.setAlgorithmClassName(StaticStreamAlgorithm.class.getName());
      
      final int port = (u.getPort() == -1) ? 80 : u.getPort();            
      selectorThread.setPort(port);

      selectorThread.setAdapter(adapter);
      
      try {
          selectorThread.listen();
      } catch (InstantiationException e) {
          IOException _e = new IOException();
          _e.initCause(e);
          throw _e;
      }
      return selectorThread;
  }
}
