package com.j2trp.core.socket;

import java.io.IOException;
import java.io.OutputStream;

public class TestOutputStream extends OutputStream {

  @Override
  public void write(int b) throws IOException {
    
    throw new IOException("Purposely throwed exception on close() according to configuration.");

  }

}
