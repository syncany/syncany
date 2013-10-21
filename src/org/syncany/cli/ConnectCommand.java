package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.Encryption;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.util.StringUtil;

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
			initPassword();
			
			
			throw new Exception("this should decrypt and read the storage.xml from the string ...");
		}
		else {
			
		}
		
	}

	private void runConnectNoLink() {
		// TODO Auto-generated method stub
		
	}

}
