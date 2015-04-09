// yacyPeerActions.java 
// -------------------------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://yacy.net
// Frankfurt, Germany, 2005
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

package de.anomic.yacy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.yacy.cora.document.RSSMessage;
import net.yacy.cora.document.UTF8;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.MapTools;


public class yacyPeerActions {
   
    private final yacySeedDB seedDB;
    private Map<String, String> userAgents;
    public  long disconnects;
    private final yacyNewsPool newsPool;
    
    public yacyPeerActions(final yacySeedDB seedDB, final yacyNewsPool newsPool) {
        this.seedDB = seedDB;
        this.newsPool = newsPool;
        this.userAgents = new ConcurrentHashMap<String, String>();
        this.disconnects = 0;
    }

    public void close() {
        // the seedDB and newsPool should be cleared elsewhere
        if (userAgents != null) userAgents.clear();
        userAgents = null;
    }
    
    public synchronized boolean connectPeer(final yacySeed seed, final boolean direct) {
        // store a remote peer's seed
        // returns true if the peer is new and previously unknown
        if (seed == null) {
            yacyCore.log.logSevere("connect: WRONG seed (NULL)");
            return false;
        }
        final String error = seed.isProper(false);
        if (error != null) {
            yacyCore.log.logSevere("connect: WRONG seed (" + seed.getName() + "/" + seed.hash + "): " + error);
            return false;
        }
        if ((this.seedDB.mySeedIsDefined()) && (seed.hash.equals(this.seedDB.mySeed().hash))) {
            yacyCore.log.logInfo("connect: SELF reference " + seed.getPublicAddress());
            return false;
        }
        final String peerType = seed.get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_VIRGIN);

        if ((peerType.equals(yacySeed.PEERTYPE_VIRGIN)) || (peerType.equals(yacySeed.PEERTYPE_JUNIOR))) {
            // reject unqualified seeds
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting NOT QUALIFIED " + peerType + " seed " + seed.getName());
            return false;
        }
        if (!(peerType.equals(yacySeed.PEERTYPE_SENIOR) || peerType.equals(yacySeed.PEERTYPE_PRINCIPAL))) {
            // reject unqualified seeds
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting NOT QUALIFIED " + peerType + " seed " + seed.getName());
            return false;
        }

        final yacySeed doubleSeed = this.seedDB.lookupByIP(seed.getInetAddress(), true, false, false);
        if ((doubleSeed != null) && (doubleSeed.getPort() == seed.getPort()) && (!(doubleSeed.hash.equals(seed.hash)))) {
            // a user frauds with his peer different peer hashes
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting FRAUD (double hashes " + doubleSeed.hash + "/" + seed.hash + " on same port " + seed.getPort() + ") peer " + seed.getName());
            return false;
        }

        if (seed.get(yacySeed.LASTSEEN, "").length() != 14) {
            // hack for peers that do not have a LastSeen date
            seed.setLastSeenUTC();
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: reset wrong date (" + seed.getName() + "/" + seed.hash + ")");
        }

        // connection time
        final long nowUTC0Time = System.currentTimeMillis(); // is better to have this value in a variable for debugging
        long ctimeUTC0 = seed.getLastSeenUTC();

        if (ctimeUTC0 > nowUTC0Time) {
            // the peer is future-dated, correct it
            seed.setLastSeenUTC();
            ctimeUTC0 = nowUTC0Time;
            assert (seed.getLastSeenUTC() - ctimeUTC0 < 100);
        }
        if (Math.abs(nowUTC0Time - ctimeUTC0) / 1000 / 60 > 60 * 6 ) {
            // the new connection is out-of-age, we reject the connection
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting out-dated peer '" + seed.getName() + "' from " + seed.getPublicAddress() + "; nowUTC0=" + nowUTC0Time + ", seedUTC0=" + ctimeUTC0 + ", TimeDiff=" + formatInterval(Math.abs(nowUTC0Time - ctimeUTC0)));
            return false;
        }

        // disconnection time
        long dtimeUTC0;
        final yacySeed disconnectedSeed = seedDB.getDisconnected(seed.hash);
        if (disconnectedSeed == null) {
            dtimeUTC0 = 0; // never disconnected: virtually disconnected maximum time ago
        } else {
            dtimeUTC0 = disconnectedSeed.getLong("dct", 0);
        }

        if (direct) {
            // remember the moment
            // Date applies the local UTC offset, which is wrong
            // we correct that by subtracting the local offset and adding
            // the remote offset.
            seed.setLastSeenUTC();
            seed.setFlagDirectConnect(true);
        } else {
            // set connection flag
            if (Math.abs(nowUTC0Time - ctimeUTC0) > 120000) seed.setFlagDirectConnect(false); // 2 minutes
        }

        // update latest version number
        if (seed.getVersion() > yacyVersion.latestRelease) yacyVersion.latestRelease = seed.getVersion();

        // prepare to update
        if (disconnectedSeed != null) {
            // if the indirect connect aims to announce a peer that we know
            // has been disconnected then we compare the dates:
            // if the new peer has a LastSeen date, and that date is before
            // the disconnection date, then we ignore the new peer
            if (!direct) {
                if (ctimeUTC0 < dtimeUTC0) {
                    // the disconnection was later, we reject the connection
                    if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting disconnected peer '" + seed.getName() + "' from " + seed.getPublicAddress());
                    return false;
                }
            }

            // this is a return of a lost peer
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: returned KNOWN " + peerType + " peer '" + seed.getName() + "' from " + seed.getPublicAddress());
            this.seedDB.addConnected(seed);
            return true;
        }
        final yacySeed connectedSeed = this.seedDB.getConnected(seed.hash);
        if (connectedSeed != null) {
            // the seed is known: this is an update
            try {
                // if the old LastSeen date is later then the other
                // info, then we reject the info
                if ((ctimeUTC0 < (connectedSeed.getLastSeenUTC())) && (!direct)) {
                    if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting old info about peer '" + seed.getName() + "'");
                    return false;
                }

                /*if (connectedSeed.getName() != seed.getName()) {
                    // TODO: update seed name lookup cache
                }*/
            } catch (final NumberFormatException e) {
                if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: rejecting wrong peer '" + seed.getName() + "' from " + seed.getPublicAddress() + ". Cause: " + e.getMessage());
                return false;
            }
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: updated KNOWN " + ((direct) ? "direct " : "") + peerType + " peer '" + seed.getName() + "' from " + seed.getPublicAddress());
            seedDB.addConnected(seed);
            return true;
        }
        
        // the seed is new
        if ((seedDB.mySeedIsDefined()) && (seed.getIP().equals(this.seedDB.mySeed().getIP()))) {
            // seed from the same IP as the calling client: can be
            // the case if there runs another one over a NAT
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: saved NEW seed (myself IP) " + seed.getPublicAddress());
        } else {
            // completely new seed
            if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: saved NEW " + peerType + " peer '" + seed.getName() + "' from " + seed.getPublicAddress());
        }
        this.seedDB.addConnected(seed);
        return true;
    }

    public boolean peerArrival(final yacySeed peer, final boolean direct) {
        if (peer == null) return false;
        final boolean res = connectPeer(peer, direct);
        if (res) {
            // perform all actions if peer is effective new
            this.processPeerArrival(peer);
            yacyChannel.channels(yacyChannel.PEERNEWS).addMessage(new RSSMessage(peer.getName() + " joined the network", "", ""));
        }
        return res;
    }
    
    public void peerDeparture(final yacySeed peer, final String cause) {
        if (peer == null) return;
        // we do this if we did not get contact with the other peer
        if (yacyCore.log.isFine()) yacyCore.log.logFine("connect: no contact to a " + peer.get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_VIRGIN) + " peer '" + peer.getName() + "' at " + peer.getPublicAddress() + ". Cause: " + cause);
        synchronized (seedDB) {
            if (!seedDB.hasDisconnected(UTF8.getBytes(peer.hash))) { disconnects++; }
            peer.put("dct", Long.toString(System.currentTimeMillis()));
            seedDB.addDisconnected(peer); // update info
        }
        yacyChannel.channels(yacyChannel.PEERNEWS).addMessage(new RSSMessage(peer.getName() + " left the network", "", ""));
    }
    
    public void peerPing(final yacySeed peer) {
        if (peer == null) return;
        // this is called only if the peer has junior status
        seedDB.addPotential(peer);
        // perform all actions
        processPeerArrival(peer);
        yacyChannel.channels(yacyChannel.PEERNEWS).addMessage(new RSSMessage(peer.getName() + " sent me a ping", "", ""));
    }
    
    private void processPeerArrival(final yacySeed peer) {
        final String recordString = peer.get("news", null);
        //System.out.println("### triggered news arrival from peer " + peer.getName() + ", news " + ((recordString == null) ? "empty" : "attached"));
        if ((recordString == null) || (recordString.length() == 0)) return;
        final String decodedString = de.anomic.tools.crypt.simpleDecode(recordString, "");
        final yacyNewsDB.Record record = this.newsPool.parseExternal(decodedString);
        if (record != null) {
            //System.out.println("### news arrival from peer " + peer.getName() + ", decoded=" + decodedString + ", record=" + recordString + ", news=" + record.toString());
            final String cre1 = MapTools.string2map(decodedString, ",").get("cre");
            final String cre2 = MapTools.string2map(record.toString(), ",").get("cre");
            if ((cre1 == null) || (cre2 == null) || (!(cre1.equals(cre2)))) {
                System.out.println("### ERROR - cre are not equal: cre1=" + cre1 + ", cre2=" + cre2);
                return;
            }
            try {
                synchronized (this.newsPool) {this.newsPool.enqueueIncomingNews(record);}
            } catch (final Exception e) {
                Log.logSevere("YACY", "processPeerArrival", e);
            }
        }
    }
    
    public void setUserAgent(final String IP, final String userAgent) {
        if (userAgents == null) return; // case can happen during shutdown
        userAgents.put(IP, userAgent);
    }
    
    public String getUserAgent(final String IP) {
        final String userAgent = userAgents.get(IP);
        return (userAgent == null) ? "" : userAgent;
    }

    /**
     * Format a time inteval in milliseconds into a String of the form
     * X 'day'['s'] HH':'mm
     */
    public static String formatInterval(final long millis) {
        try {
            final long mins = millis / 60000;
            
            final StringBuilder uptime = new StringBuilder(40);
            
            final int uptimeDays  = (int) (Math.floor(mins/1440.0));
            final int uptimeHours = (int) (Math.floor(mins/60.0)%24);
            final int uptimeMins  = (int) mins%60;
            
            uptime.append(uptimeDays)
                  .append(((uptimeDays == 1)?" day ":" days "))
                  .append((uptimeHours < 10)?"0":"")
                  .append(uptimeHours)
                  .append(':')
                  .append((uptimeMins < 10)?"0":"")
                  .append(uptimeMins);            
            
            return uptime.toString();       
        } catch (final Exception e) {
            return "unknown";
        }
    }
}
