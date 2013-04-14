package com.j2trp.core;

import java.io.UnsupportedEncodingException;

import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class ModifiableHttpRequestTest {

  private static final String TEST_ATTR_NAME = "TEST_ATTR";
  private static final Object TEST_ATTR_VALUE = new Object();

  @Test
  public void testSingleValueProperties() throws UnsupportedEncodingException {
		
	  // Set values on the wrapped object.
		MockHttpServletRequest mockReq = new MockHttpServletRequest();
		mockReq.setAttribute(TEST_ATTR_NAME, new Object());
		mockReq.setCharacterEncoding("ISO-8859-1");
		
		ModifiableHttpRequest modReq = new ModifiableHttpRequest(mockReq);
		
		modReq.setAttribute(TEST_ATTR_NAME, TEST_ATTR_VALUE);
		// modReq.setCharacterEncoding("UTF-8");
		Assert.assertEquals(modReq.getCharacterEncoding(), "ISO-8859-1");
		// Assert.assertNotSame(modReq.getAttribute(TEST_ATTR_NAME), TEST_ATTR_VALUE);
  }
}
