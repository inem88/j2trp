package com.j2trp.test.util;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.util.WorkerThreadImpl;

public class EnhancedPlainSelectorThread extends SelectorThread implements EnhancedSelectorThread {

  Thread workerThread;

  @Override
  public void start() {
    if (port == 0) {
      selectorThreads.put(getPort(), this);
    }
    workerThread = new WorkerThreadImpl("SelectorThread-" + port, this);
    workerThread.start();

  }
  
  public Thread getWorkerThread() {
    return workerThread;
  }

}
