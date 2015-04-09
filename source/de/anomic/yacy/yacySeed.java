// yacySeed.java
// -------------------------------------
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
//
//
// YACY stands for Yet Another CYberspace
//
// the yacySeed Object is the object that bundles and carries all information about
// a single peer in the yacy space.
// The yacySeed object is carried along peers using a string representation, that can
// be compressed and/or scrambled, depending on the purpose of the process.
//
// the yacy status
// any value that is defined here will be overwritten each time the proxy is started
// to prevent that the system gets confused, it should be set to "" which means
// undefined. Other status' that can be reached at run-time are
// junior    - a peer that has no public socket, thus cannot be reached on demand
// senior    - a peer that has a public socked and serves search queries
// principal - a peer like a senior socket and serves as gateway for network definition

package de.anomic.yacy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import net.yacy.cora.date.AbstractFormatter;
import net.yacy.cora.date.GenericFormatter;
import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.Domains;
import net.yacy.kelondro.data.word.Word;
import net.yacy.kelondro.index.HandleSet;
import net.yacy.kelondro.order.Base64Order;
import net.yacy.kelondro.order.Digest;
import net.yacy.kelondro.util.MapTools;
import net.yacy.kelondro.util.OS;

import de.anomic.search.Switchboard;
import de.anomic.tools.bitfield;
import de.anomic.tools.crypt;
import de.anomic.yacy.dht.FlatWordPartitionScheme;

public class yacySeed implements Cloneable, Comparable<yacySeed>, Comparator<yacySeed> {

    public static String ANON_PREFIX = "_anon";
    
    public static final int maxsize = 16000;
    /**
     * <b>substance</b> "sI" (send index/words)
     */
    public static final String INDEX_OUT = "sI";
    /**
     * <b>substance</b> "rI" (received index/words)
     */
    public static final String INDEX_IN  = "rI";
    /**
     * <b>substance</b> "sU" (send URLs)
     */
    public static final String URL_OUT = "sU";
    /**
     * <b>substance</b> "rU" (received URLs)
     */
    public static final String URL_IN  = "rU";
    /**
     * <b>substance</b> "virgin"
     */
    public static final String PEERTYPE_VIRGIN = "virgin";
    /**
     * <b>substance</b> "junior"
     */
    public static final String PEERTYPE_JUNIOR = "junior";
    /**
     * <b>substance</b> "senior"
     */
    public static final String PEERTYPE_SENIOR = "senior";
    /**
     * <b>substance</b> "principal"
     */
    public static final String PEERTYPE_PRINCIPAL = "principal";
    /**
     * <b>substance</b> "PeerType"
     */
    public static final String PEERTYPE = "PeerType";

    /** static/dynamic (if the IP changes often for any reason) */
    private static final String IPTYPE    = "IPType";
    private static final String FLAGS     = "Flags";
    private static final String FLAGSZERO = "____";
    /** the applications version */
    public  static final String VERSION   = "Version";

    public  static final String YOURTYPE  = "yourtype";
    public  static final String LASTSEEN  = "LastSeen";
    private static final String USPEED    = "USpeed";

    /** the name of the peer (user-set) */
    public  static final String NAME      = "Name";
    private static final String HASH      = "Hash";
    /** Birthday - first startup */
    private static final String BDATE     = "BDate";
    /** UTC-Offset */
    public  static final String UTC       = "UTC";
    private static final String PEERTAGS  = "Tags";

    /** the speed of indexing (pages/minute) of the peer */
    public static final String ISPEED    = "ISpeed";
    /** the speed of retrieval (queries/minute) of the peer */
    public static final String RSPEED    = "RSpeed";
    /** the number of minutes that the peer is up in minutes/day (moving average MA30) */
    public static final String UPTIME    = "Uptime";
    /** the number of links that the peer has stored (LURL's) */
    public static final String LCOUNT    = "LCount";
    /** the number of links that the peer has noticed, but not loaded (NURL's) */
    public static final String NCOUNT    = "NCount";
    /** the number of links that the peer provides for remote crawls (ZURL's) */
    public static final String RCOUNT    = "RCount";
    /** the number of different words the peer has indexed */
    public static final String ICOUNT    = "ICount";
    /** the number of seeds that the peer has stored */
    public static final String SCOUNT    = "SCount";
    /** the number of clients that the peer connects (connects/hour as double) */
    public static final String CCOUNT    = "CCount";
    public static final String IP        = "IP";
    public static final String PORT      = "Port";
    public static final String SEEDLISTURL = "seedURL";
    /** zero-value */
    private static final String ZERO      = "0";
    
    private static final int FLAG_DIRECT_CONNECT            = 0;
    private static final int FLAG_ACCEPT_REMOTE_CRAWL       = 1;
    private static final int FLAG_ACCEPT_REMOTE_INDEX       = 2;
    
    public static final String DFLT_NETWORK_UNIT = "freeworld";
    public static final String DFLT_NETWORK_GROUP = "";

    private static final Random random = new Random(System.currentTimeMillis());
    
    // class variables
    /** the peer-hash */
    public String hash;
    /** a set of identity founding values, eg. IP, name of the peer, YaCy-version, ...*/
    private final ConcurrentMap<String, String> dna;
    private String alternativeIP = null;
    private final long birthdate; // keep this value in ram since it is often used and may cause lockings in concurrent situations.

    // use our own formatter to prevent concurrency locks with other processes
    private final static GenericFormatter my_SHORT_SECOND_FORMATTER  = new GenericFormatter(GenericFormatter.FORMAT_SHORT_SECOND, GenericFormatter.time_second);

    public yacySeed(final String theHash, final ConcurrentMap<String, String> theDna) {
        // create a seed with a pre-defined hash map
        assert theHash != null;
        this.hash = theHash;
        this.dna = theDna;
        final String flags = this.dna.get(yacySeed.FLAGS);
        if ((flags == null) || (flags.length() != 4)) { this.dna.put(yacySeed.FLAGS, yacySeed.FLAGSZERO); }
        this.dna.put(yacySeed.NAME, checkPeerName(get(yacySeed.NAME, "&empty;")));
        long b;
        try {
            b = my_SHORT_SECOND_FORMATTER.parse(get(yacySeed.BDATE, "20040101000000")).getTime();
        } catch (ParseException e) {
            b = System.currentTimeMillis();
        }
        this.birthdate = b;
    }
    
    private yacySeed(final String theHash) {
        this.dna = new ConcurrentHashMap<String, String>();

        // settings that can only be computed by originating peer:
        // at first startup -
        this.hash = theHash; // the hash key of the peer - very important. should be static somehow, even after restart
        this.dna.put(yacySeed.NAME, defaultPeerName());
        
        // later during operation -
        this.dna.put(yacySeed.ISPEED, yacySeed.ZERO);
        this.dna.put(yacySeed.RSPEED, yacySeed.ZERO);
        this.dna.put(yacySeed.UPTIME, yacySeed.ZERO);
        this.dna.put(yacySeed.LCOUNT, yacySeed.ZERO);
        this.dna.put(yacySeed.NCOUNT, yacySeed.ZERO);
        this.dna.put(yacySeed.RCOUNT, yacySeed.ZERO);
        this.dna.put(yacySeed.ICOUNT, yacySeed.ZERO);
        this.dna.put(yacySeed.SCOUNT, yacySeed.ZERO);
        this.dna.put(yacySeed.CCOUNT, yacySeed.ZERO);
        this.dna.put(yacySeed.VERSION, yacySeed.ZERO);

        // settings that is created during the 'hello' phase - in first contact
        this.dna.put(yacySeed.IP, "");                 // 123.234.345.456
        this.dna.put(yacySeed.PORT, "&empty;");
        this.dna.put(yacySeed.IPTYPE, "&empty;");

        // settings that can only be computed by visiting peer
        this.dna.put(yacySeed.USPEED, yacySeed.ZERO);  // the computated uplink speed of the peer

        // settings that are needed to organize the seed round-trip
        this.dna.put(yacySeed.FLAGS, yacySeed.FLAGSZERO);
        setFlagDirectConnect(false);
        setFlagAcceptRemoteCrawl(true);
        setFlagAcceptRemoteIndex(true);
        setUnusedFlags();

        // index transfer
        this.dna.put(yacySeed.INDEX_OUT, yacySeed.ZERO); // send index
        this.dna.put(yacySeed.INDEX_IN, yacySeed.ZERO);  // received index
        this.dna.put(yacySeed.URL_OUT, yacySeed.ZERO);   // send URLs
        this.dna.put(yacySeed.URL_IN, yacySeed.ZERO);    // received URLs
        
        // default first filling
        this.dna.put(yacySeed.BDATE, my_SHORT_SECOND_FORMATTER.format());
        this.dna.put(yacySeed.LASTSEEN, this.dna.get(yacySeed.BDATE)); // just as initial setting
        this.dna.put(yacySeed.UTC, GenericFormatter.UTCDiffString());
        this.dna.put(yacySeed.PEERTYPE, yacySeed.PEERTYPE_VIRGIN); // virgin/junior/senior/principal
        
        this.birthdate = System.currentTimeMillis();
    }
    
    /**
     * check the peer name: protect against usage as XSS hack
     * @param id
     * @return a checked name without "<" and ">"
     */
    final static Pattern ltp = Pattern.compile("<");
    final static Pattern gtp = Pattern.compile(">");
    private static String checkPeerName(String name) {
        name = ltp.matcher(name).replaceAll("_");
        name = gtp.matcher(name).replaceAll("_");
        return name;
    }

    /**
     * generate a default peer name
     * @return
     */
    private static String defaultPeerName() {
        return ANON_PREFIX + OS.infoKey() + "-" + (System.currentTimeMillis() % 77777777L) + "-" + yacyCore.speedKey;
    }
    
    /**
     * Checks for the static fragments of a generated default peer name, such as the string 'dpn'
     * @see #makeDefaultPeerName()
     * @param name the peer name to check for default peer name compliance
     * @return whether the given peer name may be a default generated peer name
     */
    public static boolean isDefaultPeerName(final String name) {
        return name.startsWith("_anon");
    }
    
    /**
     * used when doing routing within a cluster; this can assign a ip and a port
     * that is used instead the address stored in the seed DNA
     */
    public void setAlternativeAddress(final String ipport) {
        if (ipport == null) return;
        final int p = ipport.indexOf(':');
        if (p < 0) this.alternativeIP = ipport; else this.alternativeIP = ipport.substring(0, p);
    }

    /**
     * try to get the IP<br>
     * @return the IP or null
     */
    public final String getIP() {
        String ip = get(yacySeed.IP, "127.0.0.1");
        return (ip == null || ip.length() == 0) ? "127.0.0.1" : ip;
    }
    /**
     * try to get the peertype<br>
     * @return the peertype or null
     */
    public final String getPeerType() { return get(yacySeed.PEERTYPE, ""); }
    /**
     * try to get the peertype<br>
     * @return the peertype or "virgin"
     */
    public final String orVirgin() { return get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_VIRGIN); }
    /**
     * try to get the peertype<br>
     * @return the peertype or "junior"
     */
    public final String orJunior() { return get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_JUNIOR); }
    /**
     * try to get the peertype<br>
     * @return the peertype or "senior"
     */
    public final String orSenior() { return get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_SENIOR); }
    /**
     * try to get the peertype<br>
     * @return the peertype or "principal"
     */
    public final String orPrincipal() { return get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_PRINCIPAL); }

    /**
     * Get a value from the peer's DNA (its set of peer defining values, e.g. IP, name, version, ...)
     * @param key the key for the value to fetch
     * @param dflt the default value
     */
    public final String get(final String key, final String dflt) {
        final Object o = this.dna.get(key);
        if (o == null) { return dflt; }
        return (String) o;
    }

    public final float getFloat(final String key, final float dflt) {
        final Object o = this.dna.get(key);
        if (o == null) { return dflt; }
        if (o instanceof String) try {
        	return Float.parseFloat((String) o);
        } catch (final NumberFormatException e) {
        	return dflt;
        } else if (o instanceof Float) {
            return ((Float) o).floatValue();
        } else return dflt;
    }
    
    public final long getLong(final String key, final long dflt) {
        final Object o = this.dna.get(key);
        if (o == null) { return dflt; }
        if (o instanceof String) try {
        	return Long.parseLong((String) o);
        } catch (final NumberFormatException e) {
        	return dflt;
        } else if (o instanceof Long) {
            return ((Long) o).longValue();
        } else if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else return dflt;
    }

    public final void setIP(final String ip)     { dna.put(yacySeed.IP, ip); }
    public final void setPort(final String port) { dna.put(yacySeed.PORT, port); }
    public final void setType(final String type) { dna.put(yacySeed.PEERTYPE, type); }
    public final void setJunior()          { dna.put(yacySeed.PEERTYPE, yacySeed.PEERTYPE_JUNIOR); }
    public final void setSenior()          { dna.put(yacySeed.PEERTYPE, yacySeed.PEERTYPE_SENIOR); }
    public final void setPrincipal()       { dna.put(yacySeed.PEERTYPE, yacySeed.PEERTYPE_PRINCIPAL); }

    public final void put(final String key, final String value) {
        synchronized (this.dna) {
            this.dna.put(key, value);
        }
    }

    /** @return the DNA-map of this peer */
    public final Map<String, String> getMap() {
        return this.dna;
    }

    public final void setName(String name) {
        synchronized (this.dna) {
            this.dna.put(yacySeed.NAME, checkPeerName(name));
        }
    }
    
    public final String getName() {
        return checkPeerName(get(yacySeed.NAME, "&empty;"));
    }

    public final String getHexHash() {
        return b64Hash2hexHash(this.hash);
    }

    public final void incSI(final int count) {
        String v = this.dna.get(yacySeed.INDEX_OUT);
        if (v == null) { v = yacySeed.ZERO; }
        dna.put(yacySeed.INDEX_OUT, Long.toString(Long.parseLong(v) + (long) count));
    }

    public final void incRI(final int count) {
        String v = this.dna.get(yacySeed.INDEX_IN);
        if (v == null) { v = yacySeed.ZERO; }
        dna.put(yacySeed.INDEX_IN, Long.toString(Long.parseLong(v) + (long) count));
    }

    public final void incSU(final int count) {
        String v = this.dna.get(yacySeed.URL_OUT);
        if (v == null) { v = yacySeed.ZERO; }
        dna.put(yacySeed.URL_OUT, Long.toString(Long.parseLong(v) + (long) count));
    }

    public final void incRU(final int count) {
        String v = this.dna.get(yacySeed.URL_IN);
        if (v == null) { v = yacySeed.ZERO; }
        dna.put(yacySeed.URL_IN, Long.toString(Long.parseLong(v) + (long) count));
    }
    
    public final void resetCounters(){
    	dna.put(yacySeed.INDEX_OUT, yacySeed.ZERO);
    	dna.put(yacySeed.INDEX_IN, yacySeed.ZERO);
    	dna.put(yacySeed.URL_OUT, yacySeed.ZERO);
    	dna.put(yacySeed.URL_IN, yacySeed.ZERO);
    }

    /**
     * <code>12 * 6 bit = 72 bit = 24</code> characters octal-hash
     * <p>Octal hashes are used for cache-dumps that are DHT-ready</p>
     * <p>
     *   Cause: the natural order of octal hashes are the same as the b64-order of b64Hashes.
     *   a hexhash cannot be used in such cases, and b64Hashes are not appropriate for file names
     * </p>
     * @param b64Hash a base64 hash
     * @return the octal representation of the given base64 hash
     */
    public static String b64Hash2octalHash(final String b64Hash) {
        return Digest.encodeOctal(Base64Order.enhancedCoder.decode(b64Hash));
    }

    /**
     * <code>12 * 6 bit = 72 bit = 18</code> characters hex-hash
     * @param b64Hash a base64 hash
     * @return the hexadecimal representation of the given base64 hash
     */
    public static String b64Hash2hexHash(final String b64Hash) {
        // the hash string represents 12 * 6 bit = 72 bits. This is too much for a long integer.
        return Digest.encodeHex(Base64Order.enhancedCoder.decode(b64Hash));
    }
    
    /**
     * @param hexHash a hexadecimal hash
     * @return the base64 representation of the given hex hash
     */
    public static String hexHash2b64Hash(final String hexHash) {
        return Base64Order.enhancedCoder.encode(Digest.decodeHex(hexHash));
    }
    
    /**
     * The returned version follows this pattern: <code>MAJORVERSION . MINORVERSION 0 SVN REVISION</code> 
     * @return the YaCy version of this peer as a float or <code>0</code> if no valid value could be retrieved
     * from this yacySeed object
     */
    public final float getVersion() {
        try {
            return Float.parseFloat(get(yacySeed.VERSION, yacySeed.ZERO));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * @return the public address of the peer as IP:port string or <code>null</code> if no valid values for
     * either the IP or the port could be retrieved from this yacySeed object
     */
    public final String getPublicAddress() {
        String ip = this.getIP();
        if (ip == null || ip.length() < 8 || ip.length() > 60) ip = "127.0.0.1";
        
        final String port = this.dna.get(yacySeed.PORT);
        if (port == null || port.length() < 2 || port.length() > 5) return null;

        StringBuilder sb = new StringBuilder(ip.length() + port.length() + 1);
        sb.append(ip);
        sb.append(':');
        sb.append(port);
        return sb.toString();
    }
    
    /**
     * If this seed is part of a cluster, the peer has probably the {@linkplain #alternativeIP} object set to
     * a local IP. If this is present and the public IP of this peer is identical to the public IP of the own seed,
     * construct an address using this IP; otherwise return the public address
     * @see #getPublicAddress()
     * @return the alternative IP:port if present, else the public address
     */
    public final String getClusterAddress() {
    	if (this.alternativeIP == null) return getPublicAddress();
    			
        final String port = this.dna.get(yacySeed.PORT);
        if ((port == null) || (port.length() < 2)) return null;

        return this.alternativeIP + ":" + port;
    }
    
    /**
     * @return the IP address of the peer represented by this yacySeed object as {@link InetAddress}
     */
    public final InetAddress getInetAddress() {
        return Domains.dnsResolve(this.getIP());
    }
    
    /** @return the portnumber of this seed or <code>-1</code> if not present */
    public final int getPort() {
        final String port = this.dna.get(yacySeed.PORT);
        if (port == null) return -1;
        /*if (port.length() < 2) return -1; It is possible to use port 0-9*/
        return Integer.parseInt(port);
    }
    
    /** puts the current time into the lastseen field and cares about the time differential to UTC */
    public final void setLastSeenUTC() {
        // because java thinks it must apply the UTC offset to the current time,
        // to create a string that looks like our current time, it adds the local UTC offset to the
        // time. To create a corrected UTC Date string, we first subtract the local UTC offset.
        String ls = my_SHORT_SECOND_FORMATTER.format(new Date(System.currentTimeMillis() /*- DateFormatter.UTCDiff()*/));
        //System.out.println("SETTING LAST-SEEN of " + this.getName() + " to " + ls);
        dna.put(yacySeed.LASTSEEN, ls );
    }
    
    /**
     * @return the last seen time converted to UTC in milliseconds
     */
    public final long getLastSeenUTC() {
        try {
            final long t = my_SHORT_SECOND_FORMATTER.parse(get(yacySeed.LASTSEEN, "20040101000000")).getTime();
            // getTime creates a UTC time number. But in this case java thinks, that the given
            // time string is a local time, which has a local UTC offset applied.
            // Therefore java subtracts the local UTC offset, to get a UTC number.
            // But the given time string is already in UTC time, so the subtraction
            // of the local UTC offset is wrong. We correct this here by adding the local UTC
            // offset again.
            return t /*+ DateFormatter.UTCDiff()*/;
        } catch (final java.text.ParseException e) { // in case of an error make seed look old!!!
            return System.currentTimeMillis() - AbstractFormatter.dayMillis;
        } catch (final java.lang.NumberFormatException e) {
            return System.currentTimeMillis() - AbstractFormatter.dayMillis;
        }
    }
    
    /**
     * test if the lastSeen time of the seed has a time-out
     * @param milliseconds the maximum age of the last-seen value
     * @return true, if the time between the last-seen time and now is greater then the given time-out
     */
    public final boolean isLastSeenTimeout(long milliseconds) {
        long d = Math.abs(System.currentTimeMillis() - this.getLastSeenUTC());
        return d > milliseconds;
    }

    /** @return the age of the seed in number of days */
    public final int getAge() {
        return (int) Math.abs((System.currentTimeMillis() - birthdate) / 1000 / 60 / 60 / 24);
    }

    public void setPeerTags(final Set<String> keys) {
        dna.put(PEERTAGS, MapTools.set2string(keys, "|", false));
    }

    public Set<String> getPeerTags() {
        return MapTools.string2set(get(PEERTAGS, "*"), "|");
    }

    public boolean matchPeerTags(final HandleSet searchHashes) {
        final String peertags = get(PEERTAGS, "");
        if (peertags.equals("*")) return true;
        final Set<String> tags = MapTools.string2set(peertags, "|");
        final Iterator<String> i = tags.iterator();
        while (i.hasNext()) {
        	if (searchHashes.has(Word.word2hash(i.next()))) return true;
        }
        return false;
    }

    public int getPPM() {
        try {
            return Integer.parseInt(get(yacySeed.ISPEED, yacySeed.ZERO));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    public float getQPM() {
        try {
            return Float.parseFloat(get(yacySeed.RSPEED, yacySeed.ZERO));
        } catch (final NumberFormatException e) {
            return 0f;
        }
    }

    public final long getLinkCount() {
        try {
            return getLong(yacySeed.LCOUNT, 0);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    public final long getWordCount() {
        try {
            return getLong(yacySeed.ICOUNT, 0);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private boolean getFlag(final int flag) {
        final String flags = get(yacySeed.FLAGS, yacySeed.FLAGSZERO);
        return (new bitfield(UTF8.getBytes(flags))).get(flag);
    }

    private void setFlag(final int flag, final boolean value) {
        String flags = get(yacySeed.FLAGS, yacySeed.FLAGSZERO);
        if (flags.length() != 4) { flags = yacySeed.FLAGSZERO; }
        final bitfield f = new bitfield(UTF8.getBytes(flags));
        f.set(flag, value);
        dna.put(yacySeed.FLAGS, UTF8.String(f.getBytes()));
    }

    public final void setFlagDirectConnect(final boolean value) { setFlag(FLAG_DIRECT_CONNECT, value); }
    public final void setFlagAcceptRemoteCrawl(final boolean value) { setFlag(FLAG_ACCEPT_REMOTE_CRAWL, value); }
    public final void setFlagAcceptRemoteIndex(final boolean value) { setFlag(FLAG_ACCEPT_REMOTE_INDEX, value); }
    public final boolean getFlagDirectConnect() { return getFlag(0); }
    public final boolean getFlagAcceptRemoteCrawl() {
        //if (getVersion() < 0.300) return false;
        //if (getVersion() < 0.334) return true;
        return getFlag(1);
    }
    public final boolean getFlagAcceptRemoteIndex() {
        //if (getVersion() < 0.335) return false;
        return getFlag(2);
    }
    public final void setUnusedFlags() {
        for (int i = 4; i < 24; i++) { setFlag(i, true); }
    }
    public final boolean isType(final String type) {
        return get(yacySeed.PEERTYPE, "").equals(type);
    }
    public final boolean isVirgin() {
        return get(yacySeed.PEERTYPE, "").equals(yacySeed.PEERTYPE_VIRGIN);
    }
    public final boolean isJunior() {
        return get(yacySeed.PEERTYPE, "").equals(yacySeed.PEERTYPE_JUNIOR);
    }
    public final boolean isSenior() {
        return get(yacySeed.PEERTYPE, "").equals(yacySeed.PEERTYPE_SENIOR);
    }
    public final boolean isPrincipal() {
        return get(yacySeed.PEERTYPE, "").equals(yacySeed.PEERTYPE_PRINCIPAL);
    }
    public final boolean isPotential() {
        return isVirgin() || isJunior();
    }
    public final boolean isActive() {
        return isSenior() || isPrincipal();
    }
    public final boolean isOnline() {
        return isSenior() || isPrincipal();
    }
    public final boolean isOnline(final String type) {
        return type.equals(yacySeed.PEERTYPE_SENIOR) || type.equals(yacySeed.PEERTYPE_PRINCIPAL);
    }
    
    public long nextLong(Random random, long n) {
        return Math.abs(random.nextLong()) % n;
    }

    private static byte[] bestGap(final yacySeedDB seedDB) {
        byte[] randomHash = randomHash();
        if ((seedDB == null) || (seedDB.sizeConnected() <= 2)) {
            // use random hash
            return randomHash;
        }
        // find gaps
        final TreeMap<Long, String> gaps = hashGaps(seedDB);
        
        // take one gap; prefer biggest but take also another smaller by chance
        String interval = null;
        while (!gaps.isEmpty()) {
            interval = gaps.remove(gaps.lastKey());
            if (random.nextBoolean()) break;
        }
        if (interval == null) return randomHash();
        
        // find dht position and size of gap
        long left = FlatWordPartitionScheme.std.dhtPosition(UTF8.getBytes(interval.substring(0, 12)), null);
        long right = FlatWordPartitionScheme.std.dhtPosition(UTF8.getBytes(interval.substring(12)), null);
        final long gap8 = FlatWordPartitionScheme.dhtDistance(left, right) >> 3; //  1/8 of a gap
        long gapx = gap8 + (Math.abs(random.nextLong()) % (6 * gap8));
        long gappos = (Long.MAX_VALUE - left >= gapx) ? left + gapx : (left - Long.MAX_VALUE) + gapx;
        byte[] computedHash = FlatWordPartitionScheme.positionToHash(gappos);
        // the computed hash is the perfect position (modulo gap4 population and gap alternatives)
        // this is too tight. The hash must be more randomized. We take only (!) the first two bytes
        // of the computed hash and add random bytes at the remaining positions. The first two bytes
        // of the hash may have 64*64 = 2^^10 positions, good for over 1 million peers.
        byte[] combined = new byte[12];
        System.arraycopy(computedHash, 0, combined, 0, 2);
        System.arraycopy(randomHash, 2, combined, 2, 10);
        // finally check if the hash is already known
        while (seedDB.hasConnected(combined) || seedDB.hasDisconnected(combined) || seedDB.hasPotential(combined)) {
            // if we are lucky then this loop will never run
            combined = randomHash();
        }
        return combined;
    }
    
    private static TreeMap<Long, String> hashGaps(final yacySeedDB seedDB) {
        final TreeMap<Long, String>gaps = new TreeMap<Long, String>();
        if (seedDB == null) return gaps;
        
        final Iterator<yacySeed> i = seedDB.seedsConnected(true, false, null, (float) 0.0);
        long l;
        yacySeed s0 = null, s1, first = null;
        while (i.hasNext()) {
            s1 = i.next();
            if (s0 == null) {
                s0 = s1;
                first = s0;
                continue;
            }
            l = FlatWordPartitionScheme.dhtDistance(
                    FlatWordPartitionScheme.std.dhtPosition(UTF8.getBytes(s0.hash), null),
                    FlatWordPartitionScheme.std.dhtPosition(UTF8.getBytes(s1.hash), null));
            gaps.put(l, s0.hash + s1.hash);
            s0 = s1;
        }
        // compute also the last gap
        if ((first != null) && (s0 != null)) {
            l = FlatWordPartitionScheme.dhtDistance(
                    FlatWordPartitionScheme.std.dhtPosition(UTF8.getBytes(s0.hash), null),
                    FlatWordPartitionScheme.std.dhtPosition(UTF8.getBytes(first.hash), null));
            gaps.put(l, s0.hash + first.hash);
        }
        return gaps;
    }
    
    public static yacySeed genLocalSeed(final yacySeedDB db) {
        return genLocalSeed(db, 0, null); // an anonymous peer
    }
    
    public static yacySeed genLocalSeed(final yacySeedDB db, final int port, final String name) {
        // generate a seed for the local peer
        // this is the birthplace of a seed, that then will start to travel to other peers

        final String hashs = UTF8.String(bestGap(db));
        yacyCore.log.logInfo("init: OWN SEED = " + hashs);

        final yacySeed newSeed = new yacySeed(hashs);

        // now calculate other information about the host
        newSeed.dna.put(yacySeed.NAME, (name) == null ? defaultPeerName() : name);
        newSeed.dna.put(yacySeed.PORT, Integer.toString((port <= 0) ? 8090 : port));
        return newSeed;
    }

    //public static String randomHash() { return "zLXFf5lTteUv"; } // only for debugging

    public static byte[] randomHash() {
        final String hash =
            Base64Order.enhancedCoder.encode(Digest.encodeMD5Raw(Long.toString(random.nextLong()))).substring(0, 6) +
            Base64Order.enhancedCoder.encode(Digest.encodeMD5Raw(Long.toString(random.nextLong()))).substring(0, 6);
        return UTF8.getBytes(hash);
    }

    public static yacySeed genRemoteSeed(final String seedStr, final String key, final boolean ownSeed, String patchIP) throws IOException {
        // this method is used to convert the external representation of a seed into a seed object
        // yacyCore.log.logFinest("genRemoteSeed: seedStr=" + seedStr + " key=" + key);

        // check protocol and syntax of seed
        if (seedStr == null) throw new IOException("seedStr == null");
        if (seedStr.length() == 0) throw new IOException("seedStr.length() == 0");
        final String seed = crypt.simpleDecode(seedStr, key);
        if (seed == null) throw new IOException("seed == null");
        if (seed.length() == 0) throw new IOException("seed.length() == 0");
        
        // extract hash
        final ConcurrentHashMap<String, String> dna = MapTools.string2map(seed, ",");
        final String hash = dna.remove(yacySeed.HASH);
        if (hash == null) throw new IOException("hash == null");
        final yacySeed resultSeed = new yacySeed(hash, dna);

        // check semantics of content
        String testResult = resultSeed.isProper(ownSeed);
        if (testResult != null && patchIP != null) {
            // in case that this proper-Test fails and a patchIP is given
            // then replace the given IP in the resultSeed with given patchIP
            // this is done if a remote peer reports its IP in a wrong way (maybe fraud attempt)
            resultSeed.setIP(patchIP);
            testResult = resultSeed.isProper(ownSeed);
        }
        if (testResult != null) throw new IOException("seed is not proper (" + testResult + "): " + resultSeed);
        
        // seed ok
        return resultSeed;
    }

    // TODO: add here IP ranges to accept also intranet networks
    public final String isProper(final boolean checkOwnIP) {
        // checks if everything is ok with that seed
        
        // check hash
        if (this.hash == null) return "hash is null";
        if (this.hash.length() != Word.commonHashLength) return "wrong hash length (" + this.hash.length() + ")";

        // name
        final String peerName = this.dna.get(yacySeed.NAME);
        if (peerName == null) return "no peer name given";
        dna.put(yacySeed.NAME, checkPeerName(peerName));

        // type
        final String peerType = this.getPeerType();
        if ((peerType == null) || 
            !(peerType.equals(yacySeed.PEERTYPE_VIRGIN) || peerType.equals(yacySeed.PEERTYPE_JUNIOR)
              || peerType.equals(yacySeed.PEERTYPE_SENIOR) || peerType.equals(yacySeed.PEERTYPE_PRINCIPAL)))
            return "invalid peerType '" + peerType + "'";

        // check IP
        if (!checkOwnIP) {
            // checking of IP is omitted if we read the own seed file        
            final String ipCheck = isProperIP(this.getIP());
            if (ipCheck != null) return ipCheck;
        }
        
        // seedURL
        final String seedURL = this.dna.get(SEEDLISTURL);
        if (seedURL != null && seedURL.length() > 0) {
            if (!seedURL.startsWith("http://") && !seedURL.startsWith("https://")) return "wrong protocol for seedURL";
            try {
                final URL url = new URL(seedURL);
                final String host = url.getHost();
                if (host.equals("localhost") || host.startsWith("127.") || (host.startsWith("0:0:0:0:0:0:0:1"))) return "seedURL in localhost rejected";
            } catch (final MalformedURLException e) {
                return "seedURL malformed";
            }
        }
        return null;
    }
    
    public static final String isProperIP(final String ipString) {
        // returns null if ipString is proper, a string with the cause otherwise
        if (ipString == null) return ipString + " -> IP is null";
        if (ipString.length() > 0 && ipString.length() < 8) return ipString + " -> IP is too short: ";
        if (Switchboard.getSwitchboard().isAllIPMode()) return null;
        boolean islocal = Domains.isLocal(ipString);
        if (islocal && Switchboard.getSwitchboard().isGlobalMode()) return ipString + " - local IP for global mode rejected";
        if (!islocal && Switchboard.getSwitchboard().isIntranetMode()) return ipString + " - global IP for intranet mode rejected";
        return null;
    }

    @Override
    public final String toString() {
        HashMap<String, String> copymap = new HashMap<String, String>();
        copymap.putAll(this.dna);
        copymap.put(yacySeed.HASH, this.hash);                // set hash into seed code structure
        return MapTools.map2string(copymap, ",", true); // generate string representation
    }

    public final String genSeedStr(final String key) {
        // use a default encoding
        final String z = this.genSeedStr('z', key);
        final String b = this.genSeedStr('b', key);
        // the compressed string may be longer that the uncompressed if there is too much overhead for compression meta-info
        // take simply that string that is shorter
        if (b.length() < z.length()) return b; else return z;
    }

    public final String genSeedStr(final char method, final String key) {
        return crypt.simpleEncode(this.toString(), key, method);
    }

    public final void save(final File f) throws IOException {
        final String out = this.genSeedStr('p', null);
        final FileWriter fw = new FileWriter(f);
        fw.write(out, 0, out.length());
        fw.close();
    }

    public static yacySeed load(final File f) throws IOException {
        final FileReader fr = new FileReader(f);
        final char[] b = new char[(int) f.length()];
        fr.read(b, 0, b.length);
        fr.close();
        final yacySeed mySeed = genRemoteSeed(new String(b), null, true, null);
        assert mySeed != null; // in case of an error, an IOException is thrown
        mySeed.dna.put(yacySeed.IP, ""); // set own IP as unknown
        return mySeed;
    }

    @Override
    public final yacySeed clone() {
        ConcurrentHashMap<String, String> ndna = new ConcurrentHashMap<String, String>();
        ndna.putAll(this.dna);
        return new yacySeed(this.hash, ndna);
    }

    @Override
    public int compareTo(yacySeed arg0) {
        // TODO Auto-generated method stub
        int o1 = this.hashCode();
        int o2 = arg0.hashCode();
        if (o1 > o2) return 1;
        if (o2 > o1) return -1;
        return 0;
    }
    
    @Override
    public int hashCode() {
        return (int) (Base64Order.enhancedCoder.cardinal(this.hash) & ((long) Integer.MAX_VALUE));
    }

    @Override
    public int compare(yacySeed o1, yacySeed o2) {
        return o1.compareTo(o2);
    }
    
}
