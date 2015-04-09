// IndexCreateDomainCrawl_p
// (C) 2004 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 02.12.2004 as IndexCreate_p.java on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
// $LastChangedRevision: 1986 $
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

import net.yacy.cora.protocol.RequestHeader;
import de.anomic.search.SwitchboardConstants;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class IndexCreateDomainCrawl_p {
    
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        //Switchboard sb = (Switchboard) env;
        serverObjects prop = new serverObjects();
        
        // define visible variables
        prop.put("proxyPrefetchDepth", env.getConfig("proxyPrefetchDepth", "0"));
        prop.put("crawlingDepth", Math.min(3, env.getConfigLong("crawlingDepth", 0)));
        prop.put("crawlingFilter", env.getConfig("crawlingFilter", "0"));
        
        int crawlingIfOlder = (int) env.getConfigLong("crawlingIfOlder", -1);
        prop.put("crawlingIfOlderCheck", (crawlingIfOlder == -1) ? "0" : "1");
        prop.put("crawlingIfOlderUnitYearCheck", "0");
        prop.put("crawlingIfOlderUnitMonthCheck", "0");
        prop.put("crawlingIfOlderUnitDayCheck", "0");
        prop.put("crawlingIfOlderUnitHourCheck", "0");
        prop.put("crawlingIfOlderUnitMinuteCheck", "0");
        if ((crawlingIfOlder == -1) || (crawlingIfOlder == Integer.MAX_VALUE)) {
            prop.put("crawlingIfOlderNumber", "-1");
            prop.put("crawlingIfOlderUnitYearCheck", "1");
        } else if (crawlingIfOlder >= 60*24*365) {
            prop.put("crawlingIfOlderNumber", Math.round((float)crawlingIfOlder / (float)(60*24*365)));
            prop.put("crawlingIfOlderUnitYearCheck", "1");
        } else if (crawlingIfOlder >= 60*24*30) {
            prop.put("crawlingIfOlderNumber", Math.round((float)crawlingIfOlder / (float)(60*24*30)));
            prop.put("crawlingIfOlderUnitMonthCheck", "1");
        } else if (crawlingIfOlder >= 60*24) {
            prop.put("crawlingIfOlderNumber", Math.round((float)crawlingIfOlder / (float)(60*24)));
            prop.put("crawlingIfOlderUnitDayCheck", "1");
        } else if (crawlingIfOlder >= 60) {
            prop.put("crawlingIfOlderNumber", Math.round(crawlingIfOlder / 60f));
            prop.put("crawlingIfOlderUnitHourCheck", "1");
        } else {
            prop.put("crawlingIfOlderNumber", crawlingIfOlder);
            prop.put("crawlingIfOlderUnitMinuteCheck", "1");
        }
        int crawlingDomFilterDepth = (int) env.getConfigLong("crawlingDomFilterDepth", -1);
        prop.put("crawlingDomFilterCheck", (crawlingDomFilterDepth == -1) ? "0" : "1");
        prop.put("crawlingDomFilterDepth", (crawlingDomFilterDepth == -1) ? 1 : crawlingDomFilterDepth);
        int crawlingDomMaxPages = (int) env.getConfigLong("crawlingDomMaxPages", -1);
        prop.put("crawlingDomMaxCheck", (crawlingDomMaxPages == -1) ? "0" : "1");
        prop.put("crawlingDomMaxPages", (crawlingDomMaxPages == -1) ? 10000 : crawlingDomMaxPages);
        prop.put("crawlingQChecked", env.getConfigBool("crawlingQ", false) ? "1" : "0");
        prop.put("storeHTCacheChecked", env.getConfigBool("storeHTCache", false) ? "1" : "0");
        prop.put("indexingTextChecked", env.getConfigBool("indexText", false) ? "1" : "0");
        prop.put("indexingMediaChecked", env.getConfigBool("indexMedia", false) ? "1" : "0");
        prop.put("crawlOrderChecked", env.getConfigBool("crawlOrder", false) ? "1" : "0");
        
        long LCbusySleep = env.getConfigLong(SwitchboardConstants.CRAWLJOB_LOCAL_CRAWL_BUSYSLEEP, 100L);
        int LCppm = (LCbusySleep == 0) ? 1000 : (int) (60000L / LCbusySleep);
        prop.put("crawlingSpeedMaxChecked", (LCppm >= 1000) ? "1" : "0");
        prop.put("crawlingSpeedCustChecked", ((LCppm > 10) && (LCppm < 1000)) ? "1" : "0");
        prop.put("crawlingSpeedMinChecked", (LCppm <= 10) ? "1" : "0");
        prop.put("customPPMdefault", ((LCppm > 10) && (LCppm < 1000)) ? Integer.toString(LCppm) : "");
        
        prop.put("xsstopwChecked", env.getConfigBool("xsstopw", false) ? "1" : "0");
        prop.put("xdstopwChecked", env.getConfigBool("xdstopw", false) ? "1" : "0");
        prop.put("xpstopwChecked", env.getConfigBool("xpstopw", false) ? "1" : "0");
        
        // return rewrite properties
        return prop;
    }
}