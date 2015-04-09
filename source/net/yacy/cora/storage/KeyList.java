/**
 *  KeyList
 *  Copyright 2011 by Michael Peter Christen, mc@yacy.net, Frankfurt a. M., Germany
 *  First released 18.4.2011 at http://yacy.net
 *
 *  $LastChangedDate: 2011-03-22 10:34:10 +0100 (Di, 22 Mrz 2011) $
 *  $LastChangedRevision: 7619 $
 *  $LastChangedBy: orbiter $
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.cora.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import net.yacy.cora.document.UTF8;

/**
 * a key list is a file which contains a list of key words; each line one word
 * The key list is stored into a java set object and the list can be extended on the fly
 * which is done by extending the file with just another line.
 * When is key list file is initialized, all lines are read and pushed into a java set
 */
public class KeyList {

    private static final Object _obj = new Object();
    
    private Map<String, Object> keys;
    private RandomAccessFile raf;
    
    public KeyList(File file) throws IOException {
        this.keys = new ConcurrentHashMap<String, Object>();
        
        if (file.exists()) {
            InputStream is = new FileInputStream(file);
            if (file.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String l;
            try {
                while ((l = reader.readLine()) != null) {
                    if (l.length() == 0 || l.charAt(0) == '#') continue;
                    l = l.trim().toLowerCase();
                    this.keys.put(l, _obj);
                }
            } catch (IOException e) {
                // finish
            }
        }
        
        this.raf = new RandomAccessFile(file, "rw");
        
    }
    
    public boolean contains(String key) {
        return this.keys.containsKey(key);
    }
    
    public void add(String key) throws IOException {
        if (keys.containsKey(key)) return;
        synchronized (this.raf) {
            if (keys.containsKey(key)) return; // check again for those threads who come late (after another has written this)
            this.keys.put(key, _obj);
            this.raf.seek(raf.length());
            this.raf.write(UTF8.getBytes(key));
            this.raf.writeByte('\n');
        }
    }
    
    public void close() throws IOException {
        synchronized (this.raf) {
            raf.close();
        }
    }
    
}
