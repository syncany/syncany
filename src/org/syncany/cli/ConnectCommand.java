package org.syncany.cli;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ConnectCommand extends AbstractInitCommand {
	public static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(\\w*)/([^/]*)/(.+)$");	
	
	public ConnectCommand() {
		super();
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		runConnectOperation(operationArgs);
		return 0;
	}	

	private void runConnectOperation(String[] operationArguments) throws OptionException, Exception {
		OptionParser parser = new OptionParser();	
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		if (nonOptionArgs.size() == 0) {
			runConnectNoLink();
		}
		else if (nonOptionArgs.size() == 1) {
			runConnectWithLink((String) nonOptionArgs.get(0));
		}
		else {
			throw new Exception("Invalid syntax.");
		}
	}

	private void runConnectWithLink(String link) throws Exception {
		Matcher linkMatcher = LINK_PATTERN.matcher(link);
		
		if (!linkMatcher.matches()) {
			throw new Exception("Invalid link provided, must start with syncany:// and match link pattern.");
		}
		
		String linkType = linkMatcher.group(1);
		String salt = linkMatcher.group(2);
		String ciphertext = linkMatcher.group(3);
		
		boolean isEncryptedLink = !"".equals(salt);
		
		if (isEncryptedLink) {
			askPassword();
			
			
			throw new Exception("this should decrypt and read the storage.xml from the string ...");
		}
		else {
			
		}
		
	}

	private void runConnectNoLink() {
		// TODO Auto-generated method stub
		
	}

}
