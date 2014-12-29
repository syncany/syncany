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
package org.syncany.operations.daemon.auth;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.syncany.config.to.UserTO;

/**
 * A simple {@link IdentityManager} implementation, that just takes a map of users to their
 * password.
 * 
 * @see https://github.com/undertow-io/undertow/blob/d160f7c44951c25186595e4755c45659396d057c/examples/src/main/java/io/undertow/examples/security/basic/MapIdentityManager.java
 * @author Stuart Douglas
 */
public class MapIdentityManager implements IdentityManager {

	private final Map<String, char[]> users;

	public MapIdentityManager(final Map<String, char[]> users) {
		this.users = users;
	}

	public MapIdentityManager(List<UserTO> users) {
		this.users = new HashMap<String, char[]>();

		for (UserTO user : users) {
			this.users.put(user.getUsername(), user.getPassword().toCharArray());
		}
	}

	@Override
	public Account verify(Account account) {
		// An existing account so for testing assume still valid.
		return account;
	}

	@Override
	public Account verify(String id, Credential credential) {
		Account account = getAccount(id);
		if (account != null && verifyCredential(account, credential)) {
			return account;
		}

		return null;
	}

	@Override
	public Account verify(Credential credential) {
		throw new RuntimeException("Not implemented.");
	}

	private boolean verifyCredential(Account account, Credential credential) {
		if (credential instanceof PasswordCredential) {
			char[] password = ((PasswordCredential) credential).getPassword();
			char[] expectedPassword = users.get(account.getPrincipal().getName());

			return Arrays.equals(password, expectedPassword);
		}
		return false;
	}

	private Account getAccount(final String id) {
		if (users.containsKey(id)) {
			return new Account() {
				private final Principal principal = new Principal() {
					@Override
					public String getName() {
						return id;
					}
				};

				@Override
				public Principal getPrincipal() {
					return principal;
				}

				@Override
				public Set<String> getRoles() {
					return Collections.emptySet();
				}
			};
		}
		
		return null;
	}
}
