/**
 *  yacyChannel
 *  Copyright 2010 by Michael Peter Christen
 *  First released 29.6.2010 at http://yacy.net
 *
 * $LastChangedDate$
 * $LastChangedRevision$
 * $LastChangedBy$
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

package de.anomic.yacy;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.yacy.cora.document.RSSFeed;
import net.yacy.cora.document.RSSMessage;

public enum yacyChannel {
    TEST,
    PEERNEWS,
    REMOTESEARCH,
    LOCALSEARCH,
    REMOTEINDEXING,
    LOCALINDEXING,
    DHTRECEIVE,
    DHTSEND;
    
    
    // test:
    // http://localhost:8090/xml/feed.rss?set=PEERNEWS,REMOTESEARCH,LOCALSEARCH,REMOTEINDEXING,LOCALINDEXING
    
    /**
     * the following private channels are declared to prevent that an access to the feed servlet
     * gets results from news channels that are not for the public
     */
    public static final Set<yacyChannel> privateChannels = EnumSet.of(yacyChannel.LOCALSEARCH, yacyChannel.LOCALINDEXING);

    /**
     * the following static channels object is used to organize a storage array for RSS feeds
     */
    private static final ConcurrentMap<yacyChannel, RSSFeed> channels = new ConcurrentHashMap<yacyChannel, RSSFeed>();
    public static RSSFeed channels(final String channelName) {
        for (final yacyChannel channel : yacyChannel.values()) {
            if (channel.name().equals(channelName)) return channels(channel);
        }
        return null;
    }
    
    public static RSSFeed channels(final yacyChannel channel) {
        RSSFeed feed = channels.get(channel);
        if (feed != null) return feed;
        feed = new RSSFeed(200);
        feed.setChannel(new RSSMessage(channel.name(), "", ""));
        channels.put(channel, feed);
        return feed;
    }
}