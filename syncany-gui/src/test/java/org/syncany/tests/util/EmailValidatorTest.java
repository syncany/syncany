/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.gui.util.EmailValidator;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class EmailValidatorTest {
	private static EmailValidator emailValidator = new EmailValidator();
	private static String[] validEmails;
	private static String[] invalidEmails;
	
	
	@BeforeClass
	public static void initData() {
		emailValidator = new EmailValidator();

		validEmails = new String[]{
			"user-number@domain.extension", 
			"user.100@domain.extension",
			"user111@domain.extension", 
			"user-100@domain.extension",
			"user.100@subdomain.domain.extension", 
			"user@1.extension",
			"user@subdomain.domain.extension", 
			"user+100@domain.extension",
			"user-100@domain-with-dash.extension" 
		};
		
		invalidEmails = new String[]{
			"user", 
			"user@.com.my",
			"user123@extension.a", 
			"user123@.com", 
			"user123@.com.com",
			".user@user.com", 
			"user()*@extension.com", 
			"user@%*.com",
			"user..2002@extension.com", 
			"user.@extension.com",
			"user@user@extension.com", 
			"user@extension.com.1a"
		};
	}
 
	@Test
	public void testValidEmails(){
		for (String email : validEmails) {
			assertTrue(emailValidator.validate(email));
		}
	}
 
	@Test
	public void testInvalidEmails(){
		for (String email : invalidEmails) {
			assertFalse(emailValidator.validate(email));
		}
	}
}
