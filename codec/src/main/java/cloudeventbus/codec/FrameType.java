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
enum FrameType {

	AUTHENTICATE ('A'),
	ERROR        ('E'),
	GREETING     ('G'),
	PING         ('I'),
	PONG         ('O'),
	PUBLISH      ('P'),
	AUTH_RESPONSE('R'),
	SUBSCRIBE    ('S'),
	UNSUBSCRIBE  ('U'),
	SERVER_READY ('Y');

	private final char opcode;

	FrameType(char opcode) {
		this.opcode = opcode;
	}

	public char getOpcode() {
		return opcode;
	}

	public static FrameType getFrameType(char opcode) {
		for (FrameType frameType : values()) {
			if (frameType.opcode == opcode) {
				return frameType;
			}
		}
		return null;
	}
}
