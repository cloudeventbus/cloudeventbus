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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class SubjectTest {

	@Test
	public void validSubjects() {
		new Subject("*");
		new Subject("com.github");
		new Subject("com.github.*");
		new Subject("1.2.3.4.5");
		new Subject("foo-123.bar_567");
	}

	@Test(expectedExceptions = NullPointerException.class)
	public void nullSubject() {
		new Subject(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void invalidSubject1() {
		new Subject("foo.*.bar");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void invalidSubject2() {
		new Subject("foo*");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void invalidSubject3() {
		new Subject("foo!");
	}

	@Test
	public void subSubjects() {
		final Subject all = new Subject("*");
		final Subject wildCard = new Subject("foo.*");
		final Subject sub = new Subject("foo.bar");
		final Subject nonSub = new Subject("bar.foo");
		final Subject copy = new Subject("foo.bar");

		assertTrue(all.isSub(wildCard));
		assertTrue(all.isSub(sub));
		assertTrue(all.isSub(nonSub));

		assertFalse(wildCard.isSub(all));
		assertFalse(sub.isSub(all));
		assertFalse(nonSub.isSub(all));

		assertTrue(wildCard.isSub(sub));
		assertFalse(wildCard.isSub(nonSub));
		assertTrue(wildCard.isSub(wildCard));

		assertTrue(sub.isSub(copy));
	}

	@Test
	public void testToString() {
		final String subject = "this.is.a.test";
		assertEquals(new Subject(subject).toString(), subject);
	}

	@Test
	public void testHashCode() {
		final String subject = "this.is.a.test";
		assertEquals(new Subject(subject).hashCode(), subject.hashCode());
	}

}
