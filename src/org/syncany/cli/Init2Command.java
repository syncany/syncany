package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
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

public class Init2Command extends AbstractInitCommand {
	private Console console;
	
	private Plugin plugin;
	private Map<String, String> pluginSettings;
	private Connection connection;
	
	private String machineName;
	private File appDir;
	private String password;
	
	public Init2Command() {
		console = System.console();
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		runInitOperation(operationArgs);
		return 0;
	}	

	private void runInitOperation(String[] operationArguments) throws OptionException, Exception {
		OptionParser parser = new OptionParser();	
		OptionSpec<Void> optionAdvanced = parser.acceptsAll(asList("a", "advanced"));
		OptionSpec<Void> optionNoGzip = parser.acceptsAll(asList("g", "no-gzip"));
		OptionSpec<Void> optionNoEncryption = parser.acceptsAll(asList("e", "no-encryption"));
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		runInitInteractive();
	}
	
	private List<CryptoSuite> getAvailableCryptoSuites() {
		return null; // TODO
	}

	private void runInitInteractive() throws Exception {
		out.println("Interactive repository initialization started.");
		out.println("Default options are in brackets.");
		out.println();
		
		initAppDir();
		initMachineName();
		
		// Ask for plugin, and plugin settings
		initPlugin();
		initPluginSettings();
		
		initRepoTest();
			
		if (false /* TODO Repo exists */) {
			
		}
		else {
			out.println("Successful. No repository found on the remote storage. ");			
			
			initPassword();
			initEncryption();
			
			writeRepoFile();
			writeStorageFile();
			writeLocalFile();
		}
	}

	private void initEncryption() throws Exception {
		out.println();
		out.println("The cipher defines the encryption parameters and key size. There are a ");
		out.println("few predefined crypto settings. To choose your own, please edit the config file.");
		out.println();
		out.println("Options:");
		out.println("  [1] AES/GCM/NoPadding, 128 bit (weak, but no country restrictions)");
		out.println("  [2] AES/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		out.println("  [3] Twofish/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		out.println();
		
		String cipherStr = null;
		int keySize = 0;
		boolean unlimitedCryptoNeeded = false;
		
		while (cipherStr == null) {
			cipherStr = console.readLine("Cipher [1]: ");
			
			if ("".equals(cipherStr) || "1".equals(cipherStr)) {
				cipherStr = "AES/GCM/NoPadding";
				keySize = 128;
				unlimitedCryptoNeeded = false;
			}
			else if ("2".equals(cipherStr) || "3".equals(cipherStr)) {
				out.println();
				out.println("This cipher/keysize might not be allowed in your country.");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (yesno.toLowerCase().startsWith("y")) {
					if ("1".equals(cipherStr)) {
						cipherStr = "AES/GCM/NoPadding";
						keySize = 256;
					}
					else {
						cipherStr = "Twofish/GCM/NoPadding";
						keySize = 256;
					}

					// Try enabling unlimited crypto
					unlimitedCryptoNeeded = true;
				
					try {
						Encryption.enableUnlimitedCrypto();
					}
					catch (Exception e) {
						throw new Exception("Unable to enable unlimited crypto. Check out: http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
					}
				}
				else {
					out.println();
					cipherStr = null;
				}
			}
			else {
				out.println("ERROR: Please choose a valid option.");
				out.println();
				
				cipherStr = null;
			}
		}
		
		//operationOptions.setEncryption(new EncryptionSettings(true, password, cipherStr, keySize, true, unlimitedCryptoNeeded));
		
	}
	
	private static class CryptoSuite {
		private boolean unlimitedStrength;
		private String cipherStr;
		private int keySize;
		private boolean iv;		
		private int ivSize;
		
		public CryptoSuite(boolean unlimitedStrength, String cipherStr, int keySize, boolean iv, int ivSize) {
			this.unlimitedStrength = unlimitedStrength;
			this.cipherStr = cipherStr;
			this.keySize = keySize;
			this.iv = iv;
			this.ivSize = ivSize;
		}

		public boolean isUnlimitedStrength() {
			return unlimitedStrength;
		}

		public String getCipherStr() {
			return cipherStr;
		}

		public int getKeySize() {
			return keySize;
		}

		public boolean isIv() {
			return iv;
		}

		public int getIvSize() {
			return ivSize;
		}				
	}

}
