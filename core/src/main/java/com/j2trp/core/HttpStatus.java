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
package com.j2trp.core;

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
