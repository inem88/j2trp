package com.heimore.j2trp.core;

import java.io.IOException;

import javax.servlet.ServletException;
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
		reverseProxyServlet.init(servletConfig);
		svcContainer.startServer();
	}

	@AfterClass
	public void tearDown() {
		svcContainer.stopServer();
	}

	@Test
	public void testNormalRequestWithoutQueryString() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/sfibonusadmin/someFile.html");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String[] splits = resp.getContentAsString().split("\\x0d\\x0a\\x0d\\x0a");
//		System.out.println(splits[0]);
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works without query strings!</h1></body></html>", splits[1]);
		
		
	}
	
	@Test(enabled = true)
	public void testNormalRequestWithQueryString() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/sfibonusadmin/someFile.html?k1=v1&k2=v2");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		HttpServlet reverseProxyServlet = super.applicationContext.getBean(
				"reverseProxy", HttpServlet.class);
		
		req.addHeader("Accept", "*/*");
		req.addHeader("User-Agent", "MockHttpServletRequest");
		reverseProxyServlet.service(req, resp);
		Assert.assertEquals(200, resp.getStatus());
		String[] splits = resp.getContentAsString().split("\\x0d\\x0a\\x0d\\x0a");
//		System.out.println(splits[0]);
		Assert.assertEquals("<html><head><title>MockTargetServer</title></head><body><h1>It works with query strings!</h1></body></html>", splits[1]);
		
		
	}

}
