// ContentTransformer.java
// ---------------------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.document.parser.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;

import net.yacy.cora.document.ASCIIComparator;
import net.yacy.kelondro.io.CharBuffer;
import net.yacy.kelondro.logging.Log;

public class ContentTransformer extends AbstractTransformer implements Transformer {
    
    // statics: for initialization of the HTMLFilterAbstractTransformer
    private static final TreeSet<String> linkTags0 = new TreeSet<String>(ASCIIComparator.insensitiveASCIIComparator);
    private static final TreeSet<String> linkTags1 = new TreeSet<String>(ASCIIComparator.insensitiveASCIIComparator);

    static {
        linkTags0.add("img");
        linkTags0.add("input");

        linkTags1.add("a");
    }

    private ArrayList<String> bluelist = null;

    public ContentTransformer() {
        super(linkTags0, linkTags1);
    }

    public void init(final String initarg) {
        if (bluelist == null) {
            // here, the init arg is used to load a list of blue-listed words
            bluelist = new ArrayList<String>();
            final File f = new File(initarg);
            if (f.canRead()) {
                try {
                    final BufferedReader r = new BufferedReader(new FileReader(f));
                    String s;
                    while ((s = r.readLine()) != null) {
                        if (s.length() > 0 && s.charAt(0) != '#') bluelist.add(s.toLowerCase());
                    }
                    r.close();
                } catch (final IOException e) {
                }
                // if (bluelist.isEmpty()) System.out.println("BLUELIST is empty");
            }
        }
    }

    public boolean isIdentityTransformer() {
        return bluelist.isEmpty();
    }

    private static char[] genBlueLetters(int length) {
            final CharBuffer bb = new CharBuffer(" <FONT COLOR=#0000FF>".toCharArray());
            length = length / 2;
            if (length > 10) length = 7;
            while (length-- > 0) {
                bb.append((int)'X');
            }
            bb.append("</FONT> ");
            final char[] result = bb.getChars();
            try {
				bb.close();
			} catch (IOException e) {
			    Log.logException(e);
			}
            return result;
    }

    private boolean bluelistHit(final char[] text) {
        if (text == null || bluelist == null) return false;
        final String lc = new String(text).toLowerCase();
        for (int i = 0; i < bluelist.size(); i++) {
            if (lc.indexOf(bluelist.get(i)) >= 0) return true;
        }
        return false;
    }
    
    public char[] transformText(final char[] text) {
        if (bluelist != null) {
            if (bluelistHit(text)) {
                // System.out.println("FILTERHIT: " + text);
                return genBlueLetters(text.length);
            }
            return text;
        }
        return text;
    }

    @Override
    public char[] transformTag0(final String tagname, final Properties tagopts, final char quotechar) {
        if (tagname.equals("img")) {
            // check bluelist
            if (bluelistHit(tagopts.getProperty("src", "").toCharArray())) return genBlueLetters(5);
            if (bluelistHit(tagopts.getProperty("alt", "").toCharArray())) return genBlueLetters(5);

            // replace image alternative name
            tagopts.setProperty("alt", new String(transformText(tagopts.getProperty("alt", "").toCharArray())));
        }
        if (tagname.equals("input") && (tagopts.getProperty("type") != null && tagopts.getProperty("type").equals("submit"))) {
            // rewrite button name
            tagopts.setProperty("value", new String(transformText(tagopts.getProperty("value", "").toCharArray())));
        }
        return TransformerWriter.genTag0(tagname, tagopts, quotechar);
    }

    @Override
    public char[] transformTag1(final String tagname, final Properties tagopts, final char[] text, final char quotechar) {
        if (bluelistHit(tagopts.getProperty("href","").toCharArray())) return genBlueLetters(text.length);
        if (bluelistHit(text)) return genBlueLetters(text.length);
        return TransformerWriter.genTag1(tagname, tagopts, text, quotechar);
    }

    @Override
    public void close() {
        // free resources
        super.close();
    }

}