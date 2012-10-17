package com.j2trp.core;

enum TouchedHeader {
	ADDED,
	CHANGED;

	@Override
	public String toString() {
		switch (this) {
		case ADDED:
			return "+";
		case CHANGED:
			return "*";
		default:
			// This should not be possible unless the programmer as added an enum value and forgot to
			// override its toString() value.
			throw new NullPointerException(); 
		}
	}
	
	
}
