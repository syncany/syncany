package org.syncany.tests;

public class Cleaner {

	/**
	 * Cleaning Home-Folders
	 */
	public static void main(String[] args) {
		TestSettings settings = TestSettings.getInstance();
		
		FileTestHelper.emptyDirectory(settings.getHome1());
		FileTestHelper.emptyDirectory(settings.getHome2());
	}

}
