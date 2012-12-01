/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.util.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * cp. JAR available at https://issues.apache.org/jira/browse/IO-51
 * 
 * 
 * @see http://stackoverflow.com/questions/872496/java-read-file-at-a-certain-rate/872694#872694
 */
public class ThrottledInputStream extends InputStream {

    private final InputStream rawStream;
    private long totalBytesRead;
    private long startTimeMillis;
    private final int ratePerMillis;

    public ThrottledInputStream(InputStream rawStream, int kBytesPersecond) {
        this.rawStream = rawStream;
        ratePerMillis = kBytesPersecond * 1024 / 1000;
    }

    @Override
    public int read() throws IOException {
        if (startTimeMillis == 0) {
            startTimeMillis = System.currentTimeMillis();
        }
        
        long now = System.currentTimeMillis();
        long interval = now - startTimeMillis;
        //see if we are too fast..
        if (interval * ratePerMillis < totalBytesRead + 1) { //+1 because we are reading 1 byte
            try {
                final long sleepTime = ratePerMillis / (totalBytesRead + 1) - interval; // will most likely only be relevant on the first few passes
                Thread.sleep(Math.max(1, sleepTime));
            } 
            catch (InterruptedException e) { }
        }
        
        totalBytesRead += 1;
        return rawStream.read();
    }
}
