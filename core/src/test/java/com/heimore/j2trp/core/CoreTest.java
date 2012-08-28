package com.heimore.j2trp.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.heimore.test.util.EmbeddedServiceContainer;

@ContextConfiguration(locations = "classpath:reverseProxyContext.xml")
@Test
public class CoreTest extends AbstractTestNGSpringContextTests {

	EmbeddedServiceContainer svcContainer;
	private static final int PORT = 64000;

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
		MockServletConfig servletConfig = new MockServletConfig();
		servletConfig.addInitParameter("TARGET_HOST", "127.0.0.1");
		servletConfig.addInitParameter("TARGET_PORT", "64000");
		servletConfig.addInitParameter("TARGET_BASE_URI", "/sfibonusadmin");
		servletConfig.addInitParameter("PROXIED_BASE_URI", "/j2trp");
		servletConfig.addInitParameter("PROXIED_PROTOCOL", "https");
		servletConfig.addInitParameter("PROXIED_HOST", "my.revproxy.org");
		servletConfig.addInitParameter("PROXIED_PORT", "4711");
		reverseProxyServlet.init(servletConfig);
		svcContainer.startServer();
	}

	@AfterClass
	public void tearDown() {
		svcContainer.stopServer();
	}

	@Test
	public void testRedirectedRequest() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/j2trp/redirect.html");
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
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/j2trp/someFile.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works without query strings!</h1></body></html>", contentAsString);
		
		
	}
	
	@Test
	public void testNormalRequestWithCookie() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/j2trp/someFileWithCookie.html");
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
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/j2trp/someFileWithCookie.html");
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
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/j2trp/someFile.html");
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
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/j2trp/someFile.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "text/html");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		req.setContentType("application/x-www-form-urlencoded"); // ; charset=UTF-8
		byte[] formData = "userid=joe&password=guessme".getBytes("ISO8859-1");
		// req.setCharacterEncoding(characterEncoding)
		req.setContent(formData);
		req.addHeader("Content-Length", formData.length);
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String contentAsString = resp.getContentAsString();
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body>joe:guessme</body></html>", contentAsString);
		
		
	}

}
