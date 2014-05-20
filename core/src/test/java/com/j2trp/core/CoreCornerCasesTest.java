package com.j2trp.core;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ReverseProxy.class, Socket.class })
public class CoreCornerCasesTest {

  @Test
  public void testFaultySocket() throws Exception {

    Socket mockSocket = PowerMockito.mock(Socket.class);
    PowerMockito.whenNew(Socket.class).withAnyArguments().thenReturn(mockSocket);
    
    Assert.assertSame(mockSocket, new Socket());
    
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse resp = new MockHttpServletResponse();
    
    Mockito.doThrow(new IOException("Mocked!")).when(mockSocket).connect(new InetSocketAddress("localhost", 64000), 15000);
    // PowerMockito.stub(MemberMatcher.method(Socket.class, "connect", InetSocketAddress.class, Integer.TYPE)).toThrow(new IOException("Mocked!"));
    ReverseProxy proxy = new ReverseProxy();
    MockServletConfig config = new MockServletConfig();
    File configFile = new File("src/test/resources/cornercase_j2trp.properties");
    Assert.assertTrue(configFile.exists());
    config.addInitParameter("configFile", configFile.getAbsolutePath());
    proxy.init(config);
    proxy.execute(req, resp);
    
    Assert.assertEquals(HttpServletResponse.SC_BAD_GATEWAY, resp.getStatus());
    Assert.assertTrue(resp.getErrorMessage().startsWith("I/O error when connecting the target"));
    
  }
}
