package com.j2trp.test.util;

import com.sun.grizzly.ssl.SSLSelectorThread;
import com.sun.grizzly.util.WorkerThreadImpl;

public class EnhancedSslSelectorThread extends SSLSelectorThread implements EnhancedSelectorThread {

  Thread workerThread;

  @Override
  public void start() {
    if (port == 0) {
      selectorThreads.put(getPort(), this);
    }
    workerThread = new WorkerThreadImpl("SslSelectorThread-" + port, this);
    workerThread.start();

  }
  
  public Thread getWorkerThread() {
    return workerThread;
  }
}
