package com.j2trp.test.util;

import java.io.IOException;

import com.sun.grizzly.tcp.Adapter;

interface EnhancedSelectorThread {
  
  public void start();
  public Thread getWorkerThread();
  public void stopEndpoint();
  public void setAlgorithmClassName(String clazz);
  public void setPort(int port);
  public void listen() throws InstantiationException, IOException;
  public void setAdapter(Adapter adapter);
  
}
