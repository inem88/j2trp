package com.j2trp.core.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.testng.annotations.Test;

@Test
public class SettingsTest {
  
  @Test
  public void testSettingsObject() throws IOException, InterruptedException {
    File tempFile = File.createTempFile("SettingsTest-", ".tmp");
    tempFile.deleteOnExit();
    
    PrintWriter pw = new PrintWriter(tempFile);
    pw.println("Testline.");
    pw.flush();
    pw.close();
    
    Settings settings = new Settings(tempFile);
    String[] strs = Settings.getPathComponents(tempFile.toPath().getParent());
    for (String str : strs) {
      System.out.println(str);
    }
    
    Thread.sleep(3000);
    
    PrintWriter pw2 = new PrintWriter(tempFile);
    pw2.println("Testline2.");
    pw2.flush();
    pw2.close();
    
    Thread.sleep(10000);
    
    
  }
  
  
  
}
