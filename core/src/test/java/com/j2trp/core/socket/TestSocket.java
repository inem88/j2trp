package com.j2trp.core.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class TestSocket extends Socket {
  boolean throwExceptionOnWrite;
  boolean throwSocketTimeoutExceptionOnConnect;
  boolean throwExceptionOnClose;
  
  public void setAlwaysTimeout (boolean throwSocketTimeoutExceptionOnConnect) {
    this.throwSocketTimeoutExceptionOnConnect = throwSocketTimeoutExceptionOnConnect;
  }

  public void setThrowExceptionOnClose (boolean throwExceptionOnClose) {
    this.throwExceptionOnClose = throwExceptionOnClose;
  }
  
  public void setThrowExceptionOnWrite(boolean throwExceptionOnWrite) {
    this.throwExceptionOnWrite = throwExceptionOnWrite;
  }

  @Override
  public void connect(SocketAddress endpoint) throws IOException {
    
    if (throwSocketTimeoutExceptionOnConnect) {
      throw new SocketTimeoutException("Purposely throwed exception on connect() according to configuration.");
    }
    super.connect(endpoint);
  }

  @Override
  public void connect(SocketAddress endpoint, int timeout) throws IOException {

    if (throwSocketTimeoutExceptionOnConnect) {
      throw new SocketTimeoutException("Purposely throwed exception on connect() according to configuration.");
    }
    
    super.connect(endpoint, timeout);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    
    if (throwExceptionOnWrite) {
      return new TestOutputStream();
    }
    return super.getOutputStream();
  }

  @Override
  public synchronized void close() throws IOException {
    
    if (throwExceptionOnClose) {
      throw new IOException("Purposely throwed exception on close() according to configuration.");
    }
    else {
      super.close();  
    }
    
  }
  
  
  
}
