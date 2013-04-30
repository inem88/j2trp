package com.j2trp.core;

import java.util.Enumeration;

class SingleValueEnumeration<T> implements Enumeration<T> {

  T value;
  boolean iterated;
  
  SingleValueEnumeration(T value) {
    this.value = value;
  }
  
  @Override
  public boolean hasMoreElements() {
    return iterated;
  }

  @Override
  public T nextElement() {
    
    if (iterated) {
        return null;
    }
    iterated = true;
    return value;
  }

}
