// RemoteCrawl_p.java
// --------------------
// (C) 2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 20.04.2007 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2010-09-02 21:24:22 +0200 (Do, 02 Sep 2010) $
// $LastChangedRevision: 7092 $
// $LastChangedBy: orbiter $
//
// LICENSE
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


import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.yacy.cora.protocol.RequestHeader;

import de.anomic.data.WorkTables;
import de.anomic.search.Switchboard;
import de.anomic.search.SwitchboardConstants;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.yacy.yacyPeerActions;
import de.anomic.yacy.yacySeed;
import de.anomic.yacy.yacyVersion;

public class RemoteCrawl_p {

    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        
        if (post != null) {
            
            // store this call as api call
            sb.tables.recordAPICall(post, "RemoteCrawl_p.html", WorkTables.TABLE_API_TYPE_CONFIGURATION, "remote crawler configuration");
            
            if (post.containsKey("crawlResponse")) {
                boolean crawlResponse = post.get("crawlResponse", "off").equals("on");
                
                // read remote crawl request settings
                sb.setConfig("crawlResponse", crawlResponse);
            }

            if (post.containsKey("acceptCrawlLimit")) {
                // read remote crawl request settings
                int newppm = 1;
                try {
                    newppm = Math.max(1, post.getInt("acceptCrawlLimit", 1));
                } catch (final NumberFormatException e) {}
                sb.setRemotecrawlPPM(newppm);
            }
        }
        
        // write remote crawl request settings
        prop.put("crawlResponse", sb.getConfigBool("crawlResponse", false) ? "1" : "0");
        long RTCbusySleep = Math.max(1, env.getConfigLong(SwitchboardConstants.CRAWLJOB_REMOTE_TRIGGERED_CRAWL_BUSYSLEEP, 100));
        final int RTCppm = (int) (60000L / RTCbusySleep);
        prop.put("acceptCrawlLimit", RTCppm);
        
        // set seed information directly
        sb.peers.mySeed().setFlagAcceptRemoteCrawl(sb.getConfigBool("crawlResponse", false));
        
        // -------------------------------------------------------------------------------------
        // write network list
        final String STR_TABLE_LIST = "list_";
        int conCount = 0;

        boolean dark = true;
        yacySeed seed;
        Iterator<yacySeed> e = null;
        e = sb.peers.seedsSortedConnected(false, yacySeed.RCOUNT);
        //e = sb.peers.seedsSortedConnected(false, yacySeed.LCOUNT);
        Pattern peerSearchPattern = null;
        while (e.hasNext() && conCount < 300) {
            seed = e.next();
            assert seed != null;
            if (seed != null) {
                final long lastseen = Math.abs((System.currentTimeMillis() - seed.getLastSeenUTC()) / 1000 / 60);
                if (lastseen > 720) continue;
                long rcount = seed.getLong(yacySeed.RCOUNT, 0);
                if (rcount == 0) continue;
                if ((post != null && post.containsKey("search"))  && peerSearchPattern != null /*(wrongregex == null)*/) {
                    boolean abort = true;
                    Matcher m = peerSearchPattern.matcher (seed.getName());
                    if (m.find ()) {
                        abort = false;
                    }
                    m = peerSearchPattern.matcher (seed.hash);
                    if (m.find ()) {
                        abort = false;
                    }
                    if (abort) continue;
                }
                prop.put(STR_TABLE_LIST + conCount + "_dark", ((dark) ? 1 : 0) ); dark=!dark;
                String shortname = seed.get(yacySeed.NAME, "deadlink");
                if (shortname.length() > 20) shortname = shortname.substring(0, 20) + "..."; 
                prop.putHTML(STR_TABLE_LIST + conCount + "_shortname", shortname);
                prop.putHTML(STR_TABLE_LIST + conCount + "_fullname", seed.get(yacySeed.NAME, "deadlink"));
                prop.put(STR_TABLE_LIST + conCount + "_age", seed.getAge());
                prop.putHTML(STR_TABLE_LIST + conCount + "_version", yacyVersion.combined2prettyVersion(seed.get(yacySeed.VERSION, "0.1"), shortname));
                prop.putNum(STR_TABLE_LIST + conCount + "_lastSeen", /*seed.getLastSeenString() + " " +*/ lastseen);
                prop.put(STR_TABLE_LIST + conCount + "_utc", seed.get(yacySeed.UTC, "-"));
                prop.putHTML(STR_TABLE_LIST + conCount + "_uptime", yacyPeerActions.formatInterval(60000 * seed.getLong(yacySeed.UPTIME, 0L)));
                prop.putNum(STR_TABLE_LIST + conCount + "_LCount", seed.getLinkCount());
                prop.putNum(STR_TABLE_LIST + conCount + "_ICount", seed.getWordCount());
                prop.putNum(STR_TABLE_LIST + conCount + "_RCount", rcount);
                prop.putNum(STR_TABLE_LIST + conCount + "_ppm", seed.getPPM());
                prop.putNum(STR_TABLE_LIST + conCount + "_qph", Math.round(6000d * seed.getQPM()) / 100d);
                conCount++;
            } // seed != null
        } // while
        prop.putNum("list", conCount);
        
        return prop;
    }
}
