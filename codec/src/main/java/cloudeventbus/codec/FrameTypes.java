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
// TODO Convert to enum
class FrameTypes {

	public static final char AUTHENTICATE  = 'A';
	public static final char SEND =          'D';
	public static final char ERROR =         'E';
	public static final char GREETING =      'G';
	public static final char PING =          'I';
	public static final char PONG =          'J';
	public static final char PUBLISH =       'P';
	public static final char AUTH_RESPONSE = 'R';
	public static final char SUBSCRIBE =     'S';
	public static final char UNSUBSCRIBE =   'U';
	public static final char SERVER_READY =  'Y';

}
