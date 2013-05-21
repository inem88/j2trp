package com.j2trp.core.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "J2TRP")
public class SettingsTest {
  
  @Test
  public void testSettingsObject() throws IOException, InterruptedException {
    File tempFile = File.createTempFile("SettingsTest-", ".tmp");
    tempFile.deleteOnExit();
    Properties props = new Properties();
    props.setProperty(Setting.TARGET_URL.toString(), "Value1");
    
    updatePropFile(props, tempFile);
    
    Settings settings = new Settings(tempFile);

    Assert.assertEquals(settings.getProperty(Setting.TARGET_URL), "Value1");

    
    Thread.sleep(3000);
    
    // Try updating the file.
    props.setProperty(Setting.TARGET_SOCKET_TIMEOUT_MS.toString(), "15000");
    
    updatePropFile(props, tempFile);
    
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty(Setting.TARGET_URL));
    Assert.assertEquals(settings.getProperty(Setting.TARGET_SOCKET_TIMEOUT_MS), "15000");
    
    // Try deleting the file.
    tempFile.delete();
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty(Setting.TARGET_URL));
    Assert.assertEquals(settings.getProperty(Setting.TARGET_SOCKET_TIMEOUT_MS), "15000");
    
    
    // Try recreating the file.
    props.setProperty(Setting.TARGET_SOCKET_TIMEOUT_MS.toString(), "20000");
    updatePropFile(props, tempFile);
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty(Setting.TARGET_URL));
    Assert.assertEquals(settings.getProperty(Setting.TARGET_SOCKET_TIMEOUT_MS), "20000");
  }

  private static void updatePropFile(Properties props, File tempFile) throws IOException,
      FileNotFoundException {
    try (PrintWriter pw = new PrintWriter(tempFile)) {
      props.store(pw, "");
    }
  }
  
  
  
}
