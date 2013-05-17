package com.j2trp.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class Settings {
  
  private static final Logger LOG = Logger.getLogger(Settings.class);
  private static final Object SINGLETON_LOCK = new Object();
  private static Thread backgroundThread;
  
  public Settings (File configFile) throws IllegalArgumentException, IOException {
    
    LOG.info("Trying to get the default file system object...");
    FileSystem fs = FileSystems.getDefault();
    LOG.info("File system is : " + fs.provider().getScheme());
    
    try {
      LOG.info("Trying to create new watch service...");
      WatchService watchService = fs.newWatchService();
      LOG.info("Trying to create new watch service...done.");
      Path pathToConfigFile = configFile.toPath();
      Path pathToConfifFileDirectory = pathToConfigFile.getParent();
      pathToConfifFileDirectory.register(watchService, 
          StandardWatchEventKinds.ENTRY_MODIFY, 
          StandardWatchEventKinds.ENTRY_DELETE, 
          StandardWatchEventKinds.ENTRY_CREATE);
      synchronized (SINGLETON_LOCK) {
        
        if (backgroundThread != null) {
          return;
        }
        
        backgroundThread = new Thread(new PropertiesFileWatcher(watchService, configFile), "PropertiesFileWatcher thread");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
      }
    }
    catch (UnsupportedOperationException e) {
      LOG.warn("Trying to create new watch service...failed, unsupported.");
    }
  }
  
  private static class PropertiesFileWatcher implements Runnable {
    WatchService watcher;
    
    PropertiesFileWatcher (WatchService watcher, File propertiesFile) {
      this.watcher = watcher;
    }
    
    @Override
    public void run() {
      while (true) {
        try {
          WatchKey key = watcher.take();
          for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent<Path> ev = cast(event);
            
            if (ev.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
              LOG.warn("Config file has been deleted, retaining the current settings in memory.");
            }
            else if (ev.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
              LOG.info("Config file has been modified, initiating reload...");
            }
            else if (ev.kind() == StandardWatchEventKinds.ENTRY_CREATE ) { 
              LOG.info("Config file has been recreated, initiating reload...");
            }
          }
          
        }
        catch (InterruptedException e) {
          LOG.warn(Thread.currentThread().getName() + " was interrupted. Changes in the settings file will no longer be detected.");
          // TODO: watcher.close();
        }
      }
    }
    
    
  }
  
  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
      return (WatchEvent<T>)event;
  }
  
  static String[] getPathComponents (Path path) {
    
    String[] result = new String[path.getNameCount()];
    for (int i = 0 ; i < result.length; i++) {
      result[i] = path.getName(i).toString();
    }
    
    return result;
  }
}
