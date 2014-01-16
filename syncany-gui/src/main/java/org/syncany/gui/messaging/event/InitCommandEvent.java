/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui.messaging.event;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class InitCommandEvent extends CommandEvent {
	private String result;
	private String shareLink;
	private String localFolder;
	private boolean shareLinkEncrypted;

	public InitCommandEvent(String commandId, String result, String shareLink, String localFolder, boolean shareLinkEncrypted) {
		super(commandId);
		this.result = result;
		this.shareLink = shareLink;
		this.localFolder = localFolder;
		this.shareLinkEncrypted = shareLinkEncrypted;
	}

	public String getResult() {
		return result;
	}

	public String getShareLink() {
		return shareLink;
	}

	public String getLocalFolder() {
		return localFolder;
	}

	public boolean isShareLinkEncrypted() {
		return shareLinkEncrypted;
	}
}
