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
  
  private static final String UPDATE_VALUE_2 = "20000";
  private static final String UPDATE_VALUE_1 = "15000";

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
    System.out.println(String.format("Updating %s to %s", Setting.TARGET_SOCKET_TIMEOUT_MS, UPDATE_VALUE_1));
    props.setProperty(Setting.TARGET_SOCKET_TIMEOUT_MS.toString(), UPDATE_VALUE_1);
    
    updatePropFile(props, tempFile);
    
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty(Setting.TARGET_URL));
    Assert.assertEquals(settings.getProperty(Setting.TARGET_SOCKET_TIMEOUT_MS), UPDATE_VALUE_1);
    
    // Try deleting the file.
    tempFile.delete();
    System.out.println("Properties file deleted: " + !tempFile.exists());
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty(Setting.TARGET_URL));
    Assert.assertEquals(settings.getProperty(Setting.TARGET_SOCKET_TIMEOUT_MS), UPDATE_VALUE_1);
    
    
    // Try recreating the file.
    System.out.println(String.format("Recreating file adn setting %s to %s", Setting.TARGET_SOCKET_TIMEOUT_MS, UPDATE_VALUE_2));
    props.setProperty(Setting.TARGET_SOCKET_TIMEOUT_MS.toString(), UPDATE_VALUE_2);
    updatePropFile(props, tempFile);
    Thread.sleep(10000);
    Assert.assertNotNull(settings.getProperty(Setting.TARGET_URL));
    Assert.assertEquals(settings.getProperty(Setting.TARGET_SOCKET_TIMEOUT_MS), UPDATE_VALUE_2);
  }

  private static void updatePropFile(Properties props, File tempFile) throws IOException,
      FileNotFoundException {
    try (PrintWriter pw = new PrintWriter(tempFile)) {
      props.store(pw, "");
    }
  }
  
  
  
}
