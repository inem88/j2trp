package com.heimore.j2trp.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.heimore.test.util.EmbeddedServiceContainer;
import com.sun.grizzly.filter.SSLReadFilter;

@ContextConfiguration(locations = "classpath:reverseProxyContext.xml")
@Test
public class CoreTest extends AbstractTestNGSpringContextTests {

	EmbeddedServiceContainer svcContainer;
	private static final int PORT = 64000;
	SSLServer sslServer;

	public CoreTest() {
		svcContainer = new EmbeddedServiceContainer(
				EmbeddedServiceContainer.DEFAULT_SERVLET_LISTENER,
				"classpath:mockTargetServer.xml", "com.heimore",
				EmbeddedServiceContainer.BASE_URI, PORT, "/sfibonusadmin");
	}

	@BeforeClass
	public void setup() throws ServletException, IOException {
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		MockServletContext servletCtx = new MockServletContext();
		servletCtx.setContextPath("/j2trp");
		MockServletConfig servletConfig = new MockServletConfig(servletCtx);
		servletConfig.addInitParameter("TARGET_URL", "http://localhost:64000/sfibonusadmin");
		reverseProxyServlet.init(servletConfig);
		svcContainer.startServer();
		
		// SSL Test setup:
		HttpServlet sslReverseProxyServlet = super.applicationContext.getBean("sslReverseProxy", HttpServlet.class);
		MockServletContext sslServletCtx = new MockServletContext();
		sslServletCtx.setContextPath("/j2trp_ssl");
		MockServletConfig sslServletConfig = new MockServletConfig(sslServletCtx);
		sslServletConfig.addInitParameter("TARGET_URL", "https://localhost:65000");
		sslReverseProxyServlet.init(sslServletConfig);
		
		String pathToKeystore = CoreTest.class.getClassLoader().getResource("unit_test_ssl.keystore").toExternalForm().substring(5);
		System.setProperty("javax.net.ssl.keyStore", pathToKeystore);
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		
		String pathToTruststore = CoreTest.class.getClassLoader().getResource("unit_test_ssl.truststore").toExternalForm().substring(5);
		System.setProperty("javax.net.ssl.trustStore", pathToTruststore);
	    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		
		sslServer = new SSLServer(65000);
		sslServer.start();
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
		reverseProxyServlet.service(req, resp);
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
}
