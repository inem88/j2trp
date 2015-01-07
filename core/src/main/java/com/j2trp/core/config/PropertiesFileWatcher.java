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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

final class PropertiesFileWatcher implements Runnable {
  WatchService watcher;
  
  PropertiesFileWatcher (WatchService watcher) {
    this.watcher = watcher;
  }
  
  @Override
  public void run() {
    
    boolean active = true;
    
    while (active) {
      try {
        WatchKey key = watcher.take();
        Path watchedDir = Settings.watchKeys.get(key);
        
        for (WatchEvent<?> event : key.pollEvents()) {
 
          // TODO: Do safe cast:
          File watchedFile = watchedDir.resolve((Path) event.context()).toFile();
          Settings settings = Settings.metaDataMap.get(watchedFile);
       
          if (settings == null) {
            // This is not a file we are watching.
            Settings.LOG.debug(String.format("%s is not a file we have any interest in." , watchedFile));
            continue;
          }
          
          if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            Settings.LOG.warn(String.format("Properties file %s has been deleted, retaining the current settings in memory.", watchedFile));
          }
          else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            Settings.LOG.info(String.format("Properties file %s has been modified, initiating reload...", watchedFile));
            try {
              settings.reload();
            }
            catch (IOException e) {
              Settings.LOG.error(String.format("I/O Exception when trying to reload file %s, retaining the current settings in memory.", watchedFile), e);
            }
          }
          else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE ) { 
            Settings.LOG.info(String.format("Properties file %s has been recreated, initiating reload...", watchedFile));
            try {
              settings.reload();
            }
            catch (IOException e) {
              Settings.LOG.error(String.format("I/O Exception when trying to reload file %s, retaining the current settings in memory.", watchedFile), e);
            }
          }
        }
        key.reset();
        
      }
      catch (InterruptedException e) {
        Settings.LOG.warn(Thread.currentThread().getName() + " was interrupted. Changes in the settings file will no longer be detected.");
        active = false;
        break;
      }
    }
    
    try {
      Settings.LOG.debug("Closing the watcher.");
      watcher.close();
    }
    catch (IOException e) {
      // By design.
    }
  }
  
  
}