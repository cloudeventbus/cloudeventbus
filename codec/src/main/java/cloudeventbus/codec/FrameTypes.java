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
class FrameTypes {

	public static final String AUTHENTICATE  = "A";
	public static final String SEND =          "D";
	public static final String ERROR =         "E";
	public static final String GREETING =      "G";
	public static final String PING =          "I";
	public static final String PONG =          "J";
	public static final String OK =            "O";
	public static final String PUBLISH =       "P";
	public static final String AUTH_RESPONSE = "R";
	public static final String SUBSCRIBE =     "S";
	public static final String UNSUBSCRIBE =   "U";
	public static final String SERVER_READY =  "Y";

}
