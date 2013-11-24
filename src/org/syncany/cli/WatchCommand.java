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
package org.syncany.cli;

import static java.util.Arrays.asList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.operations.WatchOperation.WatchOperationOptions;

public class WatchCommand extends Command {
	@Override
	public boolean initializedLocalDirRequired() {	
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		WatchOperationOptions operationOptions = new WatchOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Integer> optionInterval = parser.acceptsAll(asList("i", "interval")).withRequiredArg().ofType(Integer.class);
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --interval
		if (options.has(optionInterval)) {
			operationOptions.setInterval(options.valueOf(optionInterval)*1000);
		}
		
		// Run!
		client.watch(operationOptions);

		return 0;
	}
}
