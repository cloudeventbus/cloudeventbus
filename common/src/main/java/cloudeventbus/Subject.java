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
package cloudeventbus;

import java.util.regex.Pattern;

/**
 * Class for holding a subject. Facilitates validating a subject is a "sub" subject.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Subject {

	public static final String WILD_CARD_TOKEN = "*";
	private static final Pattern SUBJECT_PATTERN = Pattern.compile("[0-9a-zA-Z_-]+(\\.[0-9a-zA-Z_-]+)*(\\.\\*){0,1}");

	private final String subject;

	public Subject(String subject) {
		if (subject == null) {
			throw new NullPointerException("subject can not be null");
		}
		if (!WILD_CARD_TOKEN.equals(subject) && !SUBJECT_PATTERN.matcher(subject).matches()) {
			throw new IllegalArgumentException("Not valid subject syntax.");
		}
		this.subject = subject;
	}

	public boolean isWildCard() {
		return subject.endsWith(WILD_CARD_TOKEN);
	}

	public boolean isSub(Subject subSubject) {
		if (subject.equals(WILD_CARD_TOKEN)) {
			return true;
		}
		if (isWildCard()) {
			final String subjectSansWildcard = subject.substring(0, subject.length() - 1);
			return subSubject.subject.startsWith(subjectSansWildcard);
		}
		return equals(subSubject);
	}

	@Override
	public int hashCode() {
		return subject.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj.getClass() != Subject.class) { return false; }
		return subject.equals(((Subject)obj).subject);
	}

	/**
	 * Returns the subject as a string.
	 * @return the subject as a string.
	 */
	@Override
	public String toString() {
		return subject;
	}


}
