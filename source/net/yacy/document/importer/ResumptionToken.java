/**
 *  ResumptionToken
 *  Copyright 2009 by Michael Peter Christen
 *  First released 31.10.2009 at http://yacy.net
 *  
 *  This is a part of YaCy, a peer-to-peer based web search engine
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

package net.yacy.document.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.yacy.cora.date.ISO8601Formatter;
import net.yacy.cora.document.UTF8;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.logging.Log;

public class ResumptionToken extends TreeMap<String, String> {
    
    private static final long serialVersionUID = -8389462290545629792L;

    // use a collator to relax when distinguishing between lowercase und uppercase letters
    private static final Collator insensitiveCollator = Collator.getInstance(Locale.US);
    static {
        insensitiveCollator.setStrength(Collator.SECONDARY);
        insensitiveCollator.setDecomposition(Collator.NO_DECOMPOSITION);
    }
    
    int recordCounter;
    
    private DigestURI source;
    
    public ResumptionToken(DigestURI source, final byte[] b) throws IOException {
        super((Collator) insensitiveCollator.clone());
        this.source = source;
        this.recordCounter = 0;
        new Parser(b);
    }
    
    /*
    public ResumptionToken(
            DigestURI source, 
            Date expirationDate,
            int completeListSize,
            int cursor,
            String token
            ) {
        super((Collator) insensitiveCollator.clone());
        this.source = source;
        this.recordCounter = 0;
        this.put("expirationDate", DateFormatter.formatISO8601(expirationDate));
        this.put("completeListSize", Integer.toString(completeListSize));
        this.put("cursor", Integer.toString(cursor));
        this.put("token", token);
    }
    
    public ResumptionToken(
            DigestURI source, 
            String expirationDate,
            int completeListSize,
            int cursor,
            String token
            ) {
        super((Collator) insensitiveCollator.clone());
        this.source = source;
        this.recordCounter = 0;
        this.put("expirationDate", expirationDate);
        this.put("completeListSize", Integer.toString(completeListSize));
        this.put("cursor", Integer.toString(cursor));
        this.put("token", token);
    }
    */
    
    /**
     * truncate the given url at the '?'
     * @param url
     * @return a string containing the url up to and including the '?'
     */
    public static String truncatedURL(DigestURI url) {
        String u = url.toNormalform(true, true);
        int i = u.indexOf('?');
        if (i > 0) u = u.substring(0, i + 1);
        return u;
    }
    
    /**
     * while parsing the resumption token, also all records are counted
     * @return the result from counting the records
     */
    public int getRecordCounter() {
        return this.recordCounter;
    }
    
    /**
     * compute a url that can be used to resume the retrieval from the OAI-PMH resource
     * @param givenURL
     * @return
     * @throws IOException in case that no follow-up url can be generated; i.e. if the expiration date is exceeded
     */
    public DigestURI resumptionURL() throws IOException {
        // decide which kind of encoding strategy was used to get a resumptionToken:

        String token = this.getToken();
        if (token == null) throw new IOException("end of resumption reached - token == null");
        if (token.length() == 0) throw new IOException("end of resumption reached - token.length() == 0");
        String url = truncatedURL(this.source);
        
        // encoded state
        if (token.indexOf("from=") >= 0) {
            return new DigestURI(url + "verb=ListRecords&" + token);
        }
        
        // cached result set
        // can be detected with given expiration date
        Date expiration = getExpirationDate();
        if (expiration != null) {
            if (expiration.before(new Date())) throw new IOException("the resumption is expired at " + ISO8601Formatter.FORMATTER.format(expiration) + " (now: " + ISO8601Formatter.FORMATTER.format());
            // the resumption token is still fresh
        }
        String u = url + "verb=ListRecords&resumptionToken=" + escape(token);
        return new DigestURI(u);
    }
    
    public static StringBuilder escape(final String s) {
        final int len = s.length();
        final StringBuilder sbuf = new StringBuilder(len + 10);
        for (int i = 0; i < len; i++) {
            final int ch = s.charAt(i);
            if (ch == '/') {
                sbuf.append("%2F");
            } else if (ch == '?') {
                sbuf.append("%3F");
            } else if (ch == '#') {
                sbuf.append("%23");
            } else if (ch == '=') {
                sbuf.append("%3D");
            } else if (ch == '&') {
                sbuf.append("%26");
            } else if (ch == ':') {
                sbuf.append("%3A");
            } else if (ch == ';') {
                sbuf.append("%3B");
            } else if (ch == ' ') {
                sbuf.append("%20");
            } else if (ch == '%') {
                sbuf.append("%25");
            } else if (ch == '+') {
                sbuf.append("%2B");
            } else {
                sbuf.append((char)ch);
            }
        }
        return sbuf;
    }
    
    /**
     * an expiration date of a resumption token that addresses how long a cached set will
     * stay in the cache of the oai-pmh server. See:
     * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#CachedResultSet
     * @return
     */
    public Date getExpirationDate() {
        String d = this.get("expirationDate");
        if (d == null) return null;
        try {
            return ISO8601Formatter.FORMATTER.parse(d);
        } catch (ParseException e) {
            Log.logException(e);
            return new Date();
        }
    }
    
    /**
     * The completeListSize attribute provides a place where the estimated number of results
     * in the complete list response may be announced. This is likely to be used for
     * status monitoring by harvesting software and implementation is recommended especially in
     * repositories with large numbers of records. The value of completeListSize can be reliably
     * accurate only in the case of a system where the result set is cached.
     * In other cases, it is permissible for repositories to revise
     * the estimate during a list request sequence.
     * An attribute according to
     * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#completeListSize
     * @return
     */
    public int getCompleteListSize() {
        String t = this.get("completeListSize");
        if (t == null) return 0;
        return Integer.parseInt(t);
    }
    
    /**
     * The cursor attribute is the number of results returned so far in the complete list response,
     * thus it is always "0" in the first incomplete list response.
     * It should only be specified if it is consistently used in all responses.
     * An attribute according to
     * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#completeListSize
     * @return
     */
    public int getCursor() {
        String t = this.get("cursor");
        if (t == null) return 0;
        return Integer.parseInt(t);
    }
    
    /**
     * get a token of the stateless transfer in case that no expiration date is given
     * see:
     * http://www.openarchives.org/OAI/2.0/guidelines-repository.htm#StateInResumptionToken
     * @return
     */
    public String getToken() {
        return this.get("token");
    }
    
    public String toString() {
        return "source = " +  this.source + ", expirationDate=" + ISO8601Formatter.FORMATTER.format(this.getExpirationDate()) + ", completeListSize=" + getCompleteListSize() +
        ", cursor=" + this.getCursor() + ", token=" + this.getToken();
    }
    
    // get a resumption token using a SAX xml parser from am input stream
    private class Parser extends DefaultHandler {

        // class variables
        private final StringBuilder buffer;
        private boolean parsingValue;
        private SAXParser saxParser;
        private InputStream stream;
        private Attributes atts;

        public Parser(final byte[] b) throws IOException {
            this.buffer = new StringBuilder();
            this.parsingValue = false;
            this.atts = null;
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            this.stream = new ByteArrayInputStream(b);
            try {
                this.saxParser = factory.newSAXParser();
                this.saxParser.parse(this.stream, this);
            } catch (SAXException e) {
                Log.logException(e);
                Log.logWarning("ResumptionToken", "token was not parsed (1):\n" + UTF8.String(b));
            } catch (IOException e) {
                Log.logException(e);
                Log.logWarning("ResumptionToken", "token was not parsed (2):\n" + UTF8.String(b));
            } catch (ParserConfigurationException e) {
                Log.logException(e);
                Log.logWarning("ResumptionToken", "token was not parsed (3):\n" + UTF8.String(b));
                throw new IOException(e.getMessage());
            } finally {
                try {
                    this.stream.close();
                } catch (IOException e) {
                    Log.logException(e);
                }
            }
        }
        
        /*
         <resumptionToken expirationDate="2009-10-31T22:52:14Z"
         completeListSize="226"
         cursor="0">688</resumptionToken>
         */

        /*
         <resumptionToken expirationDate="2010-05-03T19:30:43Z"
         completeListSize="578"
         cursor="0">1518323588</resumptionToken>
        */

        public void startElement(final String uri, final String name, final String tag, final Attributes atts) throws SAXException {
            if ("record".equals(tag)) {
                recordCounter++;
            }
            if ("resumptionToken".equals(tag)) {
                this.parsingValue = true;
                this.atts = atts;
            }
        }

        public void endElement(final String uri, final String name, final String tag) {
            if (tag == null) return;
            if ("resumptionToken".equals(tag)) {
                put("expirationDate", atts.getValue("expirationDate"));
                put("completeListSize", atts.getValue("completeListSize"));
                put("cursor", atts.getValue("cursor"));
                put("token", buffer.toString());
                this.buffer.setLength(0);
                this.parsingValue = false;
            }
        }

        public void characters(final char ch[], final int start, final int length) {
            if (parsingValue) {
                buffer.append(ch, start, length);
            }
        }

    }

}
