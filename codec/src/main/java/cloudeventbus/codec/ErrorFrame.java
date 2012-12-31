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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ErrorFrame implements Frame {

	public enum Code {
		SERVER_ERROR(100),
		MALFORMED_REQUEST(101),
		SERVER_NOT_READY(102),
		UNSUPPORTED_PROTOCOL_VERSION(103),
		INVALID_SIGNATURE(200),
		DUPLICATE_SUBSCRIPTION(300),
		NOT_SUBSCRIBED(301);

		private static final Map<Integer, Code> codesMap = new HashMap<>();

		static {
			for (Code code : values()) {
				codesMap.put(code.errorNumber, code);
			}
		}

		private final int errorNumber;

		private Code(int errorNumber) {
			this.errorNumber = errorNumber;
		}

		public int getErrorNumber() {
			return errorNumber;
		}

		public static Code lookupCode(int errorNumber) {
			return codesMap.get(errorNumber);
		}
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
		if (message != null) {
			message = message.trim();
			if (message.length() == 0) {
				throw new IllegalArgumentException("Message cannot be empty.");
			}
		}
		this.message = message;
	}

	public Code getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
