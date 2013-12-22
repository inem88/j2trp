package com.j2trp.core.socket;

import java.net.Socket;

public class DefaultSocketFactory implements PlainSocketFactory {

  @Override
  public Socket createSocket() {
    return new Socket();
  }

}
