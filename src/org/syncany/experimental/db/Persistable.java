/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.syncany.experimental.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author pheckel
 */
interface Persistable {     
    public void write(DataOutput out) throws IOException;
    public int read(DataInput in) throws IOException;
}
