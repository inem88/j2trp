package com.heimore.j2trp.core;

class HttpStatus {
	private int code;
	private String status;

	HttpStatus (String returnLine) {
		int indexOfCodeStart = returnLine.indexOf(" ");
		int indexOfCodeEnd = returnLine.indexOf(" ", indexOfCodeStart + 1);
		code = Integer.parseInt(returnLine.substring(indexOfCodeStart + 1, indexOfCodeEnd));
		status = returnLine.substring(indexOfCodeEnd + 1);
	}
	
	String getStatus() {
		return status;
	}
	
	int getCode() {
		return code;
	}
}
