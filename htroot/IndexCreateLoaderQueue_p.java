// IndexCreateLoaderQueue_p.java
// -----------------------------
// part of the AnomicHTTPD caching proxy
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004, 2005
//
//$LastChangedDate$
//$LastChangedRevision$
//$LastChangedBy$
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

// You must compile this file with
// javac -classpath .:../classes IndexCreate_p.java
// if the shell's current path is HTROOT

import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.RequestHeader;
import de.anomic.crawler.retrieval.Request;
import de.anomic.search.Switchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.yacy.yacySeed;

public class IndexCreateLoaderQueue_p {
    
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        

        if (sb.crawlQueues.workerSize() == 0) {
            prop.put("loader-set", "0");
        } else {
            prop.put("loader-set", "1");
            boolean dark = true;
            final Request[] w = sb.crawlQueues.activeWorkerEntries();
            yacySeed initiator;
            int count = 0;
            for (int i = 0; i < w.length; i++)  {
                if (w[i] == null) continue;
                
                initiator = sb.peers.getConnected((w[i].initiator() == null) ? "" : UTF8.String(w[i].initiator()));
                prop.put("loader-set_list_"+count+"_dark", dark ? "1" : "0");
                prop.putHTML("loader-set_list_"+count+"_initiator", ((initiator == null) ? "proxy" : initiator.getName()));
                prop.put("loader-set_list_"+count+"_depth", w[i].depth());
                prop.put("loader-set_list_"+count+"_status", w[i].getStatus());
                prop.putHTML("loader-set_list_"+count+"_url", w[i].url().toNormalform(true, false));
                dark = !dark;
                count++;
            }
            prop.put("loader-set_list", count);
            prop.put("loader-set_num", count);
        }
                
        // return rewrite properties
        return prop;
    }
}
