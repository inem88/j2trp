package com.j2trp.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.ws.rs.core.UriBuilder;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.j2trp.test.util.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.grizzly.http.servlet.ServletAdapter;

/**
 * Class for running an embedded service.
 * <p>
 * See the web.xml for values to assign.
 * </p>
 * 
 * @author Martin Lindström, Heimore Group AB
 */
public class EmbeddedServiceContainer {

  /** The class name of the default context loader listener. */
  public static final String DEFAULT_SERVLET_LISTENER = "org.springframework.web.context.ContextLoaderListener";

  /** The default base URI. */
  public static final String BASE_URI = "http://localhost";

  /** A semicolon separated string of package names containing Jersey providers. */
  private String propertyPackages;

  /**
   * The context configuration location, e.g.
   * "classpath:applicationContext.xml".
   */
  private String contextConfigLocation;

  /** The servlet listener class. */
  private String servletListener;

  /** The Base URI for the server. */
  private URI baseUri;

  /** The Grizzly selector thread. */
  private EnhancedSelectorThread selectorThread;

  /** The servlet adapter. */
  private ServletAdapter adapter;

  private Map<String, Filter> filters = new LinkedHashMap<String, Filter>();
  
  private boolean ssl;
  
  private String keystoreFile;
  
  private char[] keystorePass;

  public EmbeddedServiceContainer(String servletListener, String contextConfigLocation, String propertyPackages,
      String baseUri, int port, String contextPath) {
    this.servletListener = servletListener;
    this.contextConfigLocation = contextConfigLocation;
    this.propertyPackages = propertyPackages;
    this.baseUri = UriBuilder.fromUri(baseUri).port(port).path(contextPath).build();
  }
  
  public void setSsl (boolean useSsl) {
    this.ssl = useSsl;
  }
  
  public void setSslParameters (String keystoreFile, char[] storePass) {
    this.keystoreFile = keystoreFile;
    this.keystorePass = storePass;
  }

  /**
   * Starts the embedded service.
   * 
   * @throws IllegalArgumentException
   *           if illegal arguments are passed.
   * @throws IOException
   *           if the configuration files cannot be read.
   */
  public void startServer() throws IllegalArgumentException, IOException {
    this.adapter = new ServletAdapter();
    this.adapter.addInitParameter("com.sun.jersey.config.property.packages", this.propertyPackages);
    this.adapter.addContextParameter("contextConfigLocation", this.contextConfigLocation);
    this.adapter.addServletListener(this.servletListener);
    this.adapter.setServletInstance(new SpringServlet());
    this.adapter.setProperty("load-on-startup", 1);
    if (this.ssl) {
      this.adapter.setProperty("ssl.keystoreFile", this.keystoreFile);
      this.adapter.setProperty("ssl.keystorePass", this.keystorePass);
    }

    for (Map.Entry<String, Filter> filterEntry : filters.entrySet()) {
      this.adapter.addFilter(filterEntry.getValue(), filterEntry.getKey(), null);

    }

    this.adapter.setContextPath(baseUri.getPath());
    this.selectorThread = EnhancedGrizzlyServerFactory.create(this.baseUri, adapter);
  }

  /**
   * Returns the application context for the server.
   * 
   * @return the application context for the server.
   */
  public ApplicationContext getServerContext() {
    return WebApplicationContextUtils.getWebApplicationContext(this.adapter.getServletInstance().getServletConfig()
        .getServletContext());
  }

  public void addFilter(String filterName, Filter filter) {
    filters.put(filterName, filter);
  }

  public void removeFilter(String filterName) {
    filters.remove(filterName);
  }

  public void clearFilters() {
    filters.clear();
  }

  /**
   * Stops the embedded service.
   */
  public void stopServer() {

    if (this.selectorThread != null) {
      try {
        this.selectorThread.stopEndpoint();
        Thread workerThread = this.selectorThread.getWorkerThread();
        workerThread.join(2000);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        this.selectorThread = null;
      }
    }
  }

  /**
   * Returns the base URI for the embedded server.
   * 
   * @return the base URI for the embedded server.
   */
  public URI getBaseUri() {
    return this.baseUri;
  }

  public static void main(String[] args) {

    // args[0] = "classpath:replicatingApplicationContext-test1.xml"
    // args[1] = "com.heimore"
    // args[2] = "/lean-sso"
    // args[3] = port
    EmbeddedServiceContainer svc = new EmbeddedServiceContainer(DEFAULT_SERVLET_LISTENER, args[0], args[1], BASE_URI,
        Integer.parseInt(args[3]), args[2]);
    try {
      svc.startServer();
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      
      String line = reader.readLine();
      
      while (line != null) {
        if (line.equals("quit")) {
          svc.stopServer();
          break;
        }
        line = reader.readLine();
      }
    }
    catch (IOException e) {
      System.exit(-1);
    }
      
    
  }
}
