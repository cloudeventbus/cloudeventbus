/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
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
package cloudeventbus.cli;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.IStringConverter;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class LogLevelConverter implements IStringConverter<Level> {
	@Override
	public Level convert(String value) {
		final Level level = Level.valueOf(value);
		if (level == null) {
			throw new IllegalArgumentException("Invalid log level: " + value);
		}
		return level;
	}
}
