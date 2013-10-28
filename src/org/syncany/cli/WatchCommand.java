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
