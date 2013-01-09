package com.j2trp.core;

import java.util.Enumeration;

class EmptyEnumeration<T> implements Enumeration<T> {
	
	@Override
	public boolean hasMoreElements() {
		return false;
	}

	@Override
	public T nextElement() {
		return null;
	}
}
