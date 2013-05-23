package com.j2trp.core;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.j2trp.core.filter.TestFilter;
import com.j2trp.test.util.EmbeddedServiceContainer;

@ContextConfiguration(locations = "classpath:reverseProxyContext.xml")
@Test(groups = "J2TRP")
public class CoreTest extends AbstractTestNGSpringContextTests {

	EmbeddedServiceContainer svcContainer;
	private static final int PORT = 64000;
	SSLServer sslServer;

	public CoreTest() {
		svcContainer = new EmbeddedServiceContainer(
				EmbeddedServiceContainer.DEFAULT_SERVLET_LISTENER,
				"classpath:mockTargetServer.xml", "com.j2trp",
				EmbeddedServiceContainer.BASE_URI, PORT, "/sfibonusadmin");
	}

	private static String getRealFilePath (String resource) {
	  URL resourceUrl = CoreTest.class.getClassLoader().getResource(resource);
	  
	  return resourceUrl.getPath();
	}
	
	
	@BeforeClass
	public void setup() throws ServletException, IOException {
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		MockServletContext servletCtx = new MockServletContext();
		servletCtx.setContextPath("/j2trp");
		MockServletConfig servletConfig = new MockServletConfig(servletCtx);
		servletConfig.addInitParameter("configFile", getRealFilePath("normal_j2trp.properties"));
		reverseProxyServlet.init(servletConfig);
		svcContainer.startServer();
		
		// SSL Test setup:
		HttpServlet sslReverseProxyServlet = super.applicationContext.getBean("sslReverseProxy", HttpServlet.class);
		MockServletContext sslServletCtx = new MockServletContext();
		sslServletCtx.setContextPath("/j2trp_ssl");
		MockServletConfig sslServletConfig = new MockServletConfig(sslServletCtx);
		sslServletConfig.addInitParameter("configFile", getRealFilePath("ssl_j2trp.properties"));
		sslReverseProxyServlet.init(sslServletConfig);
		
		String pathToKeystore = CoreTest.class.getClassLoader().getResource("unit_test_ssl.keystore").toExternalForm().substring(5);
		System.setProperty("javax.net.ssl.keyStore", pathToKeystore);
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		
		String pathToTruststore = CoreTest.class.getClassLoader().getResource("unit_test_ssl.truststore").toExternalForm().substring(5);
		System.setProperty("javax.net.ssl.trustStore", pathToTruststore);
	    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		
		sslServer = new SSLServer(65000);
		sslServer.start();
		
		// Faulty reverse proxy setup:
		HttpServlet faultyReverseProxyServlet = super.applicationContext.getBean("faultyReverseProxy", HttpServlet.class);
		MockServletContext faultyServletCtx = new MockServletContext();
		faultyServletCtx.setContextPath("/j2trp_faulty");
		MockServletConfig faultyServletConfig = new MockServletConfig(faultyServletCtx);
		faultyServletConfig.addInitParameter("configFile", getRealFilePath("bogus_j2trp.properties"));
		faultyReverseProxyServlet.init(faultyServletConfig);
		
		// Tight time-out proxy setup
		HttpServlet timeoutReverseProxyServlet = super.applicationContext.getBean("tightTimeoutReverseProxy", HttpServlet.class);
    MockServletContext timeoutServletCtx = new MockServletContext();
    timeoutServletCtx.setContextPath("/j2trp_timeout");
    MockServletConfig timeoutServletConfig = new MockServletConfig(timeoutServletCtx);
    timeoutServletConfig.addInitParameter("configFile", getRealFilePath("timeout_j2trp.properties"));
    timeoutReverseProxyServlet.init(timeoutServletConfig);
	}

	@AfterClass
	public void tearDown() {
		svcContainer.stopServer();
		sslServer.stop();
	}
	
	static MockHttpServletRequest createReq (String method, String uri) {
		MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
		req.setServerName("my.revproxy.org");
		req.setScheme("https");
		req.setProtocol("HTTP/1.0");
		req.setServerPort(4711);
		req.setContextPath("/j2trp");
		return req;
	}
	
	static MockHttpServletRequest createSslReq (String method, String uri) {
		MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
		req.setServerName("my.revproxy.org");
		req.setScheme("https");
		req.setProtocol("HTTP/1.0");
		req.setServerPort(4711);
		req.setContextPath("/j2trp_ssl");
		return req;
	}

	@Test
	public void testRedirectedRequest() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/redirect.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(302, resp.getStatus());
		Assert.assertEquals("https://my.revproxy.org:4711/j2trp/other_location.html?q1=v1", resp.getRedirectedUrl());
	}

	
	@Test
	public void testNormalRequestWithoutQueryString() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/someFile.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		req.addHeader("X-Forwarded-For", "someOtherProxy");
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works without query strings!</h1></body></html>", contentAsString);
		Assert.assertEquals("someOtherProxy, 127.0.0.1", resp.getHeader("X-Forwarded-For"));
		
	}
	
	@Test
	public void testNormalRequestWithCookie() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/someFileWithCookie.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		req.setCookies(new Cookie("TEST_COOKIE", "MY_COOKIE"));
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works with cookie: MY_COOKIE</h1></body></html>", contentAsString);
	}
	
	@Test
	public void testNormalRequestWithCookies() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/someFileWithCookie.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		req.setCookies(new Cookie("TEST_COOKIE", "MY_COOKIE"), new Cookie("TEST_COOKIE2", "MY_COOKIE2"));
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works with cookie: MY_COOKIE:MY_COOKIE2</h1></body></html>", contentAsString);
		Cookie simpleCookie = resp.getCookie("SIMPLE");
		Cookie completeCookie = resp.getCookie("COMPLETE");
		Assert.assertNotNull(simpleCookie);
		Assert.assertEquals("COOKIE", simpleCookie.getValue());
		Assert.assertNotNull(completeCookie);
		Assert.assertEquals("VALUE2", completeCookie.getValue());
		Assert.assertEquals("Some comment.", completeCookie.getComment());
		Assert.assertEquals(".example.org", completeCookie.getDomain());
		Assert.assertEquals(0, completeCookie.getMaxAge());
		Assert.assertEquals("/j2trp", completeCookie.getPath());
		Assert.assertEquals(true, completeCookie.getSecure());
		Assert.assertEquals(1, completeCookie.getVersion());
		Assert.assertNotNull(resp.getCookie("NEW_COOKIE_VERSION"));
	}
	
	
	@Test(enabled = true)
	public void testNormalRequestWithQueryString() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/someFile.html");
		req.setQueryString("k1=v1&k2=v2");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works with query strings!</h1></body></html>", contentAsString);
		
		
	}
	
	@Test(enabled = true)
	public void testNormalPost() throws Exception {
		MockHttpServletRequest req = createReq("POST", "/j2trp/someFile.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("X-Test-Header", 1);
		req.addHeader("X-Test-Header", 2);
		req.addHeader("X-Test-Header", 3);
		req.addHeader("Accept", "text/html");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		req.setContentType("application/x-www-form-urlencoded"); // ; charset=UTF-8
		byte[] formData = "userid=joe&password=guessme".getBytes("ISO8859-1");
		// req.setCharacterEncoding(characterEncoding)
		req.setContent(formData);
		req.addHeader("Content-Length", formData.length);
		req.addHeader("Connection", "keep-alive");
		MockFilterChain mockFilterChain = new MockFilterChain();
		Filter filter = new TestFilter();
		filter.doFilter(req, resp, mockFilterChain);
		Assert.assertNotSame(mockFilterChain.getRequest(), req);
		reverseProxyServlet.service(mockFilterChain.getRequest(), resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body>joe:guessme</body></html>", contentAsString);
		
		
	}
	
	@Test
	public void testMiscUtils() {
		
		String dumpEnumStrMultiple = ReverseProxy.dumpEnumeration(Collections.enumeration(Arrays.asList("A", "B", "C")));
		Assert.assertNotNull(dumpEnumStrMultiple);
		Assert.assertEquals("[A, B, C]", dumpEnumStrMultiple);
		
		String dumpEnumStrSingle = ReverseProxy.dumpEnumeration(Collections.enumeration(Arrays.asList("X")));
		Assert.assertNotNull(dumpEnumStrSingle);
		Assert.assertEquals("X", dumpEnumStrSingle);
		
		String dumpEnumStrEmpty = ReverseProxy.dumpEnumeration(Collections.enumeration(new ArrayList<String>()));
		Assert.assertNotNull(dumpEnumStrEmpty);
		Assert.assertTrue(dumpEnumStrEmpty.isEmpty());
	}
	
	@Test
	public void testBuildRedirectUrl() {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setServerName("my.revproxy.org");
		req.setScheme("https");
		req.setServerPort(4711);
		ReverseProxy reverseProxy = super.applicationContext.getBean(
				"reverseProxy", ReverseProxy.class);
		
		Assert.assertEquals("https://my.revproxy.org:4711/translated/path", reverseProxy.buildRedirectUrl(req, "/translated/path"));
		
	}

	@Test
	public void testInterruptedResponse() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/500");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		reverseProxyServlet.service(req, resp);
		
		Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp.getStatus());
	}
	
	@Test
  public void testTightSocketTimeout() throws Exception {
    MockHttpServletRequest req = createReq("GET", "/j2trp_timeout/whatever");
    MockHttpServletResponse resp = new MockHttpServletResponse();
    HttpServlet reverseProxyServlet = super.applicationContext.getBean(
        "tightTimeoutReverseProxy", HttpServlet.class);
    
    reverseProxyServlet.service(req, resp);
    
    Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_GATEWAY_TIMEOUT);
  }
	
	@Test(enabled = false)
	public void testBrokenPipe() throws Exception {
		MockHttpServletRequest req = createReq("GET", "/j2trp/interrupt");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		reverseProxyServlet.service(req, resp);
		
		Assert.assertEquals(HttpServletResponse.SC_BAD_GATEWAY, resp.getStatus());
	}
	
	public void testSSL() throws Exception {
		MockHttpServletRequest req = createSslReq("GET", "/j2trp_ssl/normal");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet sslReverseProxyServlet = super.applicationContext.getBean(
				"sslReverseProxy", HttpServlet.class);
		sslReverseProxyServlet.service(req, resp);
		
		Assert.assertEquals(HttpServletResponse.SC_OK, resp.getStatus());
	}
	
	public void testSSLWithError() throws Exception {
		MockHttpServletRequest req = createSslReq("GET", "/j2trp_ssl/fake_an_error");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet sslReverseProxyServlet = super.applicationContext.getBean(
				"sslReverseProxy", HttpServlet.class);
		sslReverseProxyServlet.service(req, resp);
		
		Assert.assertEquals(HttpServletResponse.SC_BAD_GATEWAY, resp.getStatus());
	}
	
	public void testFaultyRevProxy() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/j2trp_faulty/somepage.htm");
		req.setServerName("my.revproxy.org");
		req.setScheme("http");
		req.setProtocol("HTTP/1.0");
		req.setServerPort(4711);
		req.setContextPath("/j2trp_faulty");
		
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet faultyReverseProxyServlet = super.applicationContext.getBean(
				"faultyReverseProxy", HttpServlet.class);
		faultyReverseProxyServlet.service(req, resp);
		
		
		Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp.getStatus());
	}
	
	public void testUrlRewriteQueryString() throws Exception {
	  
	  MockHttpServletRequest req = createReq("GET", "/j2trp/redirect.html");
    MockHttpServletResponse resp = new MockHttpServletResponse();
    final HttpServlet reverseProxyServlet = super.applicationContext.getBean(
        "reverseProxy", HttpServlet.class);
    
    req.addHeader("Accept", "*/*");
    req.addHeader("User-Agent", "MockHttpServletRequest");
    
    Filter queryRewrite = new Filter() {
      
      HttpServlet proxy = reverseProxyServlet;

      @Override
      public void init(FilterConfig filterConfig) throws ServletException { }

      @Override
      public void doFilter(ServletRequest request, ServletResponse response,
          FilterChain chain) throws IOException, ServletException {
        
        proxy.service(request, response);
        
        MockHttpServletResponse mockResponse = (MockHttpServletResponse) response;
        String modifiedRedirect = mockResponse.getRedirectedUrl();
        modifiedRedirect = modifiedRedirect.replace("q1=v1", "q2=v2");
        mockResponse.setCommitted(false);
        mockResponse.sendRedirect(modifiedRedirect);
        mockResponse.setCommitted(true);
        
      }

      @Override
      public void destroy() { }
      
    };
    
    queryRewrite.doFilter(req, resp, null);
    Assert.assertEquals(302, resp.getStatus());
    
    Assert.assertEquals(resp.getRedirectedUrl(), "https://my.revproxy.org:4711/j2trp/other_location.html?q2=v2");
	}
}
