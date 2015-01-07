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


public enum Setting {
  TARGET_URL("target_url", null),
  TARGET_SOCKET_TIMEOUT_MS("target_socket_timeout_ms", "30000");
  
  private String propertyKeyName;
  private String defaultValue;
  
  Setting(String propertyKeyName, String defaultValue) {
    this.propertyKeyName = propertyKeyName;
    this.defaultValue = defaultValue;
  }
  
  @Override
  public String toString() {
    return propertyKeyName;
  }
  
  public String getDefaultValue() {
    return defaultValue;
  }
}
