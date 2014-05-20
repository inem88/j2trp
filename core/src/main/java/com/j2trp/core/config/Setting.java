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
