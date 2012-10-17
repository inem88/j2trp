package com.j2trp.core;

public final class Toolbox {
	private Toolbox() { }
	
	public static <T> T getValue (Object orgValue, Object overriddenValue, Class<T> clazz) {
		if (overriddenValue == null) {
			return clazz.cast(orgValue);
		}
		
		return clazz.cast(overriddenValue);
	}
}
