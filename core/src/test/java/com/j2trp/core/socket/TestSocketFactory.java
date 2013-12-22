package com.j2trp.core.socket;

import java.net.Socket;

public class TestSocketFactory implements PlainSocketFactory {

  boolean alwaysTimeout;
  boolean throwExceptionOnClose;
  
  public void setAlwaysTimeout (boolean alwaysTimeout) {
    this.alwaysTimeout = alwaysTimeout;
  }

  public void setThrowExceptionOnClose (boolean throwExceptionOnClose) {
    this.throwExceptionOnClose = throwExceptionOnClose;
  }
  
  
  @Override
  public Socket createSocket() {
    
    TestSocket socket = new TestSocket();
    
    socket.setThrowExceptionOnClose(throwExceptionOnClose);
    socket.setAlwaysTimeout(alwaysTimeout);
    
    return socket;
  }

}
