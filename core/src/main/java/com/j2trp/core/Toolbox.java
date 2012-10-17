package com.j2trp.core;

import java.lang.reflect.Array;

public final class Toolbox {
	private Toolbox() { }
	
	public static <T> T getValue (Object orgValue, Object overriddenValue, Class<T> clazz) {
		if (overriddenValue == null) {
			return clazz.cast(orgValue);
		}
		
		return clazz.cast(overriddenValue);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T merge (Object[] orgValue, Object[] overriddenValue, Class<T> clazz) {

		if (overriddenValue == null) {
			return clazz.cast(orgValue);
		}
		
		int overriddenLength = overriddenValue.length;
		T[] result = (T[]) Array.newInstance(clazz.getComponentType(), orgValue.length + overriddenLength);

		System.arraycopy(orgValue, 0, result, 0, orgValue.length);
		System.arraycopy(overriddenValue, 0, result, orgValue.length, overriddenLength);
		
		return clazz.cast(result);
	}
}
