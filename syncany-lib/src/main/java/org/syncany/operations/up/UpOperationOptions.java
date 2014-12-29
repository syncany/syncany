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
package org.syncany.operations.up;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.status.StatusOperationOptions;

@Root(name = "up")
public class UpOperationOptions implements OperationOptions {
	@Element(name = "status", required = false)
	private StatusOperationOptions statusOptions = new StatusOperationOptions();

	@Element(required = false)
	private boolean forceUploadEnabled = false;

	@Element(required = false)
	private boolean resume = true;

	public StatusOperationOptions getStatusOptions() {
		return statusOptions;
	}

	public void setStatusOptions(StatusOperationOptions statusOptions) {
		this.statusOptions = statusOptions;
	}

	public boolean forceUploadEnabled() {
		return forceUploadEnabled;
	}

	public void setForceUploadEnabled(boolean forceUploadEnabled) {
		this.forceUploadEnabled = forceUploadEnabled;
	}

	public boolean isResume() {
		return resume;
	}

	public void setResume(boolean resume) {
		this.resume = resume;
	}
}