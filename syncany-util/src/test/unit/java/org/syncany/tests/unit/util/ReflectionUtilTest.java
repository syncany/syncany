/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.unit.util;

import java.lang.reflect.Constructor;

import org.junit.Test;
import org.syncany.util.ReflectionUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ReflectionUtilTest {

	@Test
	public void testMatchingConstructor() throws Exception {

		final Constructor<?> reference = X.class.getConstructor(A.class, A.class);
		assertNotNull(reference);

		for (Constructor<?> c : X.class.getConstructors()) {
			System.out.println(c.toString());
		}

		assertNotNull(ReflectionUtil.getMatchingConstructorForClass(X.class, A.class, A.class));
		assertEquals(reference, ReflectionUtil.getMatchingConstructorForClass(X.class, A.class, A.class));

		assertNotNull(ReflectionUtil.getMatchingConstructorForClass(X.class, A.class, C.class));

		assertNotNull(ReflectionUtil.getMatchingConstructorForClass(X.class, A.class));
		assertNotNull(ReflectionUtil.getMatchingConstructorForClass(X.class, B.class));

		assertNull(ReflectionUtil.getMatchingConstructorForClass(X.class, A.class, B.class)); // type erasure :(
		assertNull(ReflectionUtil.getMatchingConstructorForClass(X.class, B.class, B.class));
		assertNull(ReflectionUtil.getMatchingConstructorForClass(X.class, B.class, A.class));
		assertNull(ReflectionUtil.getMatchingConstructorForClass(X.class, A.class, B.class, B.class));
		assertNull(ReflectionUtil.getMatchingConstructorForClass(X.class));

	}

	public static class X {
		public <T extends A> X(A a, T b) {
		}

		public X(B b) {
		}

		public X(A a, C c) {
		}
	}

	public static abstract class A {
	}

	public static class B extends A {
	}

	public static class C extends A {
	}

}
