/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncany.util;

import java.io.File;

/**
 * Recursively search a directory tree, for each File found in it, call
 * FileFinderListener.proceedFile. A filter can be set prior calling this
 * method.
 *
 * @author ycoppel@google.com (Yohann Coppel)
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @see http://www.java2s.com/Code/Java/File-Input-Output/Recursivelysearchadirectorytree.htm
 */
public class FileLister {

    /**
     * Base directory to explore.
     */
    private File rootFolder;
    /**
     * File listener for callbacks.
     */
    private FileListerListener listener;

    /**
     * @param root
     *          path to the classFile (usually top level package as com in
     *          com.google.common)
     */
    public FileLister(File root, FileListerListener depmk) {
        this.rootFolder = root;
        this.listener = depmk;
    }

    /**
     * begin the search of class files in the classPath given to the constructor.
     */
    public void start() {
        listener.startProcessing();
        
        File[] children = rootFolder.listFiles();
        
        if (children != null && children.length > 0) {
            for (File f : rootFolder.listFiles()) {
                dfsFileSearch(f);
            }
        }
        
        listener.endOfProcessing();
    }

    /**
     * operate a recursive depth first search in the directory. If file is a
     * directory, recursively search into for files. For each file found, check if
     * it passes the filter of DepMaker, and if yes, call proceedFile with the
     * file.
     *
     * @param file
     *          File where to start the dfsFileSearch
     */
    private void dfsFileSearch(File file) {
        if (file == null) {
            return;
        }

        // Directory
        if (file.isDirectory() && listener.directoryFilter(file)) {
            listener.enterDirectory(file);

            File[] children = file.listFiles();

            if (children != null) {
                for (File f : children) {
                    dfsFileSearch(f);
                }
            }

            listener.outDirectory(file);
        } // File
        else if (file.isFile() && listener.fileFilter(file)) {
            listener.proceedFile(file);
        }
    }

    /**
     * Listener for callbacks when directories and files are found when exploring a
     * directory, or a jar file for example.
     *
     * @author ycoppel@google.com (Yohann Coppel)
     *
     */
    public interface FileListerListener {

        /**
         * Filter to apply to (normal - i.e not a directory) files found in the
         * directory. May be a simple filename check as "end with .class"
         *
         * @param name
         *          filename
         * @return true if the filename pass the filter, and that proceedFile should
         *         be called on this file later
         *
         */
        public boolean fileFilter(File file);

        /**
         * Filter to apply to directories. If this methods return false,
         * subdirectories should not be traversed. May be a simple directory path
         * check as "start with 'directoryfilter'"
         *
         * @param name
         *          filename to check
         * @return true if the directory can contain interresting files.
         */
        public boolean directoryFilter(File directory);

        /**
         * method called when a file passing the fileFilter test is found.
         *
         * @param f
         *          file found.
         */
        public void proceedFile(File f);

        /**
         * called when entering in the given directory
         *
         * @param directoryPath
         */
        public void enterDirectory(File directory);

        /**
         * called when we step out from the given directory
         *
         * @param directoryPath
         */
        public void outDirectory(File directory);

        /**
         * callback called at the begining of the processing
         */
        public void startProcessing();

        /**
         * callback called at the end of the processing
         */
        public void endOfProcessing();
    }

    /**
     * Can be overridden.
     */
    public static class FileListerAdapter implements FileListerListener {

        @Override
        public boolean fileFilter(File file) {
            return true;
        }

        @Override
        public boolean directoryFilter(File directory) {
            return true;
        }

        @Override
        public void proceedFile(File f) {
            // Nothing.
        }

        @Override
        public void enterDirectory(File directory) {
            // Nothing.
        }

        @Override
        public void outDirectory(File directory) {
            // Nothing.
        }

        @Override
        public void startProcessing() {
            // Nothing.
        }

        @Override
        public void endOfProcessing() {
            // Nothing.
        }
    }
}
