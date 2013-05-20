package com.j2trp.core.config;

import java.io.File;
import java.io.FileNotFoundException;
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
    
    updatePropFile(props, tempFile);
    
    Settings settings = new Settings(tempFile);

    Assert.assertEquals(settings.getProperty("Key1"), "Value1");
    Assert.assertNull(settings.getProperty("Key2"));
    
    Thread.sleep(3000);
    
    // Try updating the file.
    props.setProperty("Key2", "Value2");
    
    updatePropFile(props, tempFile);
    
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty("Key1"));
    Assert.assertEquals(settings.getProperty("Key2"), "Value2");
    
    // Try deleting the file.
    tempFile.delete();
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty("Key1"));
    Assert.assertEquals(settings.getProperty("Key2"), "Value2");
    
    
    // Try recreating the file.
    props.setProperty("Key2", "Value2.2");
    updatePropFile(props, tempFile);
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty("Key1"));
    Assert.assertEquals(settings.getProperty("Key2"), "Value2.2");
  }

  private static void updatePropFile(Properties props, File tempFile) throws IOException,
      FileNotFoundException {
    try (PrintWriter pw = new PrintWriter(tempFile)) {
      props.store(pw, "");
    }
  }
  
  
  
}
