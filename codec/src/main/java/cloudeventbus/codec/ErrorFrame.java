/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ErrorFrame implements Frame {

	public enum Code {

	}

	private final Code code;
	private final String message;

	public ErrorFrame(Code code) {
		this(code, null);
	}

	public ErrorFrame(Code code, String message) {
		if (code == null) {
			throw new IllegalArgumentException("Code cannot be null");
		}
		this.code = code;
		this.message = message;
	}

	public Code getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
