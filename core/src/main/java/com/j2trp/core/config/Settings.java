/*
   Copyright 2015 Daniel Roig

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.j2trp.core.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Settings {
  
  static final Logger LOG = LoggerFactory.getLogger(Settings.class);
  private static Thread backgroundThread;
  private static WatchService watchService;
  
  /**
   * This map must be synchronized in order to avoid a race condition between the time we 
   * register a watchable and the thread waiting for an event.
   */
  static final Map<WatchKey, Path> watchKeys = Collections.synchronizedMap(new HashMap<WatchKey, Path>());
  static final Map<File, Settings> metaDataMap = new HashMap<>();

  AtomicReference<Properties> props = new AtomicReference<Properties>();
  File propertiesFile;
  
  public Settings (File configFile) throws IllegalArgumentException, IOException {
    
    this.propertiesFile = configFile;
    reload();

      synchronized (watchKeys) {
        
        if (watchKeys.isEmpty()) {
          FileSystem fs = FileSystems.getDefault();
          watchService = fs.newWatchService();
          backgroundThread = new Thread(new PropertiesFileWatcher(watchService), "PropertiesFileWatcher thread");
          backgroundThread.setDaemon(true);
          backgroundThread.start();
          LOG.info("Started background thread");
        }
        
        Path pathToConfigFile = configFile.toPath();
        Path watchedDir = pathToConfigFile.getParent();
        
        WatchKey key = watchedDir.register(watchService, 
            StandardWatchEventKinds.ENTRY_MODIFY, 
            StandardWatchEventKinds.ENTRY_DELETE, 
            StandardWatchEventKinds.ENTRY_CREATE);
        // Add check to see if we are already watching this dir/file.
        watchKeys.put(key, watchedDir);
        LOG.info("Added new watch for properties file " + configFile);
        metaDataMap.put(configFile, this);
        
        LOG.info(String.format("Currently watching %d file%s", metaDataMap.size(), (metaDataMap.size() == 1 ? "" : "s")));
      }
  }
  
  void reload() throws IOException {
    
    Properties result = new Properties();
    
    try (InputStream is = new FileInputStream(propertiesFile)) {
      result.load(is);
    }
    
    props.set(result);
    LOG.info(String.format("Properties file %s successfully loaded: %d entries read, modified timestamp is %tF %<tT", propertiesFile, result.size(), propertiesFile.lastModified()));
  }
  
  public String getProperty (Setting key) {
    return props.get().getProperty(String.valueOf(key), key.getDefaultValue());
  }
  
  public int getPropertyAsInt (Setting key) {
    return Integer.parseInt(props.get().getProperty(key.toString(), key.getDefaultValue()));
  }
  
}
