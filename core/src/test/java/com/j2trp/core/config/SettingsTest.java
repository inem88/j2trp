package com.j2trp.core.config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class SettingsTest {
  
  @Test
  public void testSettingsObject() throws IOException, InterruptedException {
    File tempFile = File.createTempFile("SettingsTest-", ".tmp");
    tempFile.deleteOnExit();
    Properties props = new Properties();
    props.setProperty("Key1", "Value1");
    
    try (PrintWriter pw = new PrintWriter(tempFile)) {
      props.store(pw, "");
    }
    
    Settings settings = new Settings(tempFile);
    String[] strs = Settings.getPathComponents(tempFile.toPath().getParent());
    for (String str : strs) {
      System.out.println(str);
    }
    
    Assert.assertEquals(settings.getProperty("Key1"), "Value1");
    Assert.assertNull(settings.getProperty("Key2"));
    
    Thread.sleep(3000);
    
    props.setProperty("Key2", "Value2");
    
    try (PrintWriter pw = new PrintWriter(tempFile)) {
      props.store(pw, "");
    }
    
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty("Key1"));
    Assert.assertEquals(settings.getProperty("Key2"), "Value2");
    
    
  }
  
  
  
}
