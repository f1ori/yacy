//PerformaceQueues_p.java
//-----------------------
//part of YaCy
//(C) by Michael Peter Christen; mc@yacy.net
//first published on http://www.anomic.de
//Frankfurt, Germany, 2004, 2005
//last major change: 16.02.2005
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

//You must compile this file with
//javac -classpath .:../classes Network.java
//if the shell's current path is HTROOT

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.util.FileUtils;
import net.yacy.kelondro.util.Formatter;
import net.yacy.kelondro.util.MemoryControl;
import net.yacy.kelondro.util.OS;
import net.yacy.kelondro.workflow.BusyThread;
import net.yacy.kelondro.workflow.WorkflowThread;
import de.anomic.search.Segment;
import de.anomic.search.Segments;
import de.anomic.search.Switchboard;
import de.anomic.search.SwitchboardConstants;
import de.anomic.server.serverCore;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

public class PerformanceQueues_p {
    /**
     * list of pre-defined settings: filename -> description
     */
    private final static Map<String, String> performanceProfiles = new HashMap<String, String>(4, 0.9f);
    static {
        // no sorted output!
        performanceProfiles.put("defaults/yacy.init", "default (crawl)");
        performanceProfiles.put("defaults/performance_dht.profile", "prefer DHT");
    }
    
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        File defaultSettingsFile = new File(sb.getAppPath(), "defaults/yacy.init");
        
        // get segment
        Segment indexSegment = null;
        if (post != null && post.containsKey("segment")) {
            String segmentName = post.get("segment");
            if (sb.indexSegments.segmentExist(segmentName)) {
                indexSegment = sb.indexSegments.segment(segmentName);
            }
        } else {
            // take default segment
            indexSegment = sb.indexSegments.segment(Segments.Process.PUBLIC);
        }
        
        if(post != null) {
        	if(post.containsKey("defaultFile")){
	            // TODO check file-path!
	            final File value = new File(sb.getAppPath(), post.get("defaultFile", "defaults/yacy.init"));
	            // check if value is readable file
	            if(value.exists() && value.isFile() && value.canRead()) {
	                defaultSettingsFile = value;
	            }
        	}
            if (post.containsKey("Xmx")) {
                int xmx = post.getInt("Xmx", 500); // default maximum heap size
                if (OS.isWin32) xmx = Math.min(2000, xmx);
                int xms = xmx / 4;
	            sb.setConfig("javastart_Xmx", "Xmx" + xmx + "m");
	            sb.setConfig("javastart_Xms", "Xms" + xms + "m");
	            prop.put("setStartupCommit", "1");
            }
            if(post.containsKey("diskFree")) {
            	sb.setConfig(SwitchboardConstants.DISK_FREE, post.getInt("diskFree", 3000));
            }
            if(post.containsKey("diskFreeHardlimit")) {
            	sb.setConfig(SwitchboardConstants.DISK_FREE_HARDLIMIT, post.getInt("diskFreeHardlimit", 1000));
            }
            if(post.containsKey("memoryAcceptDHT")) {
            	sb.setConfig(SwitchboardConstants.MEMORY_ACCEPTDHT, post.getInt("memoryAcceptDHT", 50));
            }
            if(post.containsKey("resetObserver")) {
            	MemoryControl.setDHTallowed();
            }
        }
        final Map<String, String> defaultSettings = ((post == null) || (!(post.containsKey("submitdefault")))) ? null : FileUtils.loadMap(defaultSettingsFile);
        Iterator<String> threads = sb.threadNames();
        String threadName;
        BusyThread thread;
        
        final boolean xml = (header.get(HeaderFramework.CONNECTION_PROP_PATH)).endsWith(".xml");
        prop.setLocalized(!xml);
        
        // calculate totals
        long blocktime_total = 0, sleeptime_total = 0, exectime_total = 0;
        while (threads.hasNext()) {
            threadName = threads.next();
            thread = sb.getThread(threadName);
            blocktime_total += thread.getBlockTime();
            sleeptime_total += thread.getSleepTime();
            exectime_total += thread.getExecTime();
        }   
        if (blocktime_total == 0) blocktime_total = 1;
        if (sleeptime_total == 0) sleeptime_total = 1;
        if (exectime_total == 0) exectime_total = 1;
        
        // set templates for latest news from the threads
        long blocktime, sleeptime, exectime;
        long idlesleep, busysleep, memuse, memprereq;
        int queuesize;
        threads = sb.threadNames();
        int c = 0;
        long idleCycles, busyCycles, memshortageCycles;
        // set profile?
        final double multiplier = (post != null) && post.containsKey("profileSpeed") ? 100.0 / post.getFloat("profileSpeed", 100.0f) : 1.0;
        final boolean setProfile = (post != null && post.containsKey("submitdefault"));
        final boolean setDelay = (post != null) && (post.containsKey("submitdelay"));
        // save used settings file to config
        if (setProfile && post != null){
        	sb.setConfig("performanceProfile", post.get("defaultFile", "defaults/yacy.init"));
        	sb.setConfig("performanceSpeed", post.getInt("profileSpeed", 100));
        }
        
        while (threads.hasNext()) {
            threadName = threads.next();
            thread = sb.getThread(threadName);
            
            // set values to templates
            prop.put("table_" + c + "_threadname", threadName);

			prop.putHTML("table_" + c + "_hasurl_shortdescr", thread.getShortDescription());
			if(thread.getMonitorURL() == null) {
				prop.put("table_"+c+"_hasurl", "0");
			}else{
				prop.put("table_"+c+"_hasurl", "1");
				prop.put("table_" + c + "_hasurl_url", thread.getMonitorURL());
			}
            prop.putHTML("table_" + c + "_longdescr", thread.getLongDescription());
            queuesize = thread.getJobCount();
            prop.put("table_" + c + "_queuesize", (queuesize == Integer.MAX_VALUE) ? "unlimited" : Formatter.number(queuesize, !xml));
            
            blocktime = thread.getBlockTime();
            sleeptime = thread.getSleepTime();
            exectime = thread.getExecTime();
            memuse = thread.getMemoryUse();
            idleCycles = thread.getIdleCycles();
            busyCycles = thread.getBusyCycles();
            memshortageCycles = thread.getOutOfMemoryCycles();
            prop.putNum("table_" + c + "_blocktime", blocktime / 1000);
            prop.putNum("table_" + c + "_blockpercent", 100 * blocktime / blocktime_total);
            prop.putNum("table_" + c + "_sleeptime", sleeptime / 1000);
            prop.putNum("table_" + c + "_sleeppercent", 100 * sleeptime / sleeptime_total);
            prop.putNum("table_" + c + "_exectime", exectime / 1000);
            prop.putNum("table_" + c + "_execpercent", 100 * exectime / exectime_total);
            prop.putNum("table_" + c + "_totalcycles", idleCycles + busyCycles + memshortageCycles);
            prop.putNum("table_" + c + "_idlecycles", idleCycles);
            prop.putNum("table_" + c + "_busycycles", busyCycles);
            prop.putNum("table_" + c + "_memscycles", memshortageCycles);
            prop.putNum("table_" + c + "_sleeppercycle", ((idleCycles + busyCycles) == 0) ? -1 : sleeptime / (idleCycles + busyCycles));
            prop.putNum("table_" + c + "_execpercycle", (busyCycles == 0) ? -1 : exectime / busyCycles);
            prop.putNum("table_" + c + "_memusepercycle", (busyCycles == 0) ? -1 : memuse / busyCycles / 1024);
            
            // load with old values
            idlesleep = sb.getConfigLong(threadName + "_idlesleep" , 1000);
            busysleep = sb.getConfigLong(threadName + "_busysleep",   100);
            memprereq = sb.getConfigLong(threadName + "_memprereq",     0);
            if (setDelay && post != null) {
                // load with new values
                idlesleep = post.getLong(threadName + "_idlesleep", idlesleep);
                busysleep = post.getLong(threadName + "_busysleep", busysleep);
                memprereq = post.getLong(threadName + "_memprereq", memprereq) * 1024;
                if (memprereq == 0) memprereq = sb.getConfigLong(threadName + "_memprereq", 0);
                    
                // check values to prevent short-cut loops
                if (idlesleep < 1000) idlesleep = 1000;
                if (threadName.equals("10_httpd")) { idlesleep = 0; busysleep = 0; memprereq = 0; }
                
                sb.setThreadPerformance(threadName, idlesleep, busysleep, memprereq);
                idlesleep = sb.getConfigLong(threadName + "_idlesleep", idlesleep);
                busysleep = sb.getConfigLong(threadName + "_busysleep", busysleep);
            }
            if (setProfile) {
                if (threadName.equals(SwitchboardConstants.PEER_PING) ||
                    threadName.equals(SwitchboardConstants.SEED_UPLOAD) ||
                    threadName.equals(SwitchboardConstants.CLEANUP)) {
                    /* do not change any values */
                } else {
                    // load with new values
                    idlesleep = (long) (Long.parseLong(d(defaultSettings.get(threadName + "_idlesleep"), String.valueOf(idlesleep))) * multiplier);
                    busysleep = (long) (Long.parseLong(d(defaultSettings.get(threadName + "_busysleep"), String.valueOf(busysleep))) * multiplier);
                    //memprereq = (long) (Long.parseLong(d(defaultSettings.get(threadName + "_memprereq"), String.valueOf(memprereq))) * multiplier);

                    // check values to prevent short-cut loops
                    if (idlesleep < 1000) idlesleep = 1000;
                    if (threadName.equals("10_httpd")) { idlesleep = 0; busysleep = 0; memprereq = 0; }
                    //if (threadName.equals(plasmaSwitchboardConstants.CRAWLJOB_LOCAL_CRAWL) && (busysleep < 50)) busysleep = 50;
                    sb.setThreadPerformance(threadName, idlesleep, busysleep, memprereq);
                }
            }
            prop.put("table_" + c + "_idlesleep", idlesleep);
            prop.put("table_" + c + "_busysleep", busysleep);
            prop.put("table_" + c + "_memprereq", memprereq / 1024);
            // disallow setting of memprereq for indexer to prevent db from throwing OOMs
            prop.put("table_" + c + "_disabled", /*(threadName.endsWith("_indexing")) ? 1 :*/ "0");
            prop.put("table_" + c + "_recommendation", threadName.endsWith("_indexing") ? "1" : "0");
            prop.putNum("table_" + c + "_recommendation_value", threadName.endsWith("_indexing") ? (indexSegment.termIndex().minMem() / 1024) : 0);
            c++;
        }
        prop.put("table", c);
        
        // performance profiles
        c = 0;
        final String usedfile = sb.getConfig("performanceProfile", "defaults/yacy.init");
        for(final String filename: performanceProfiles.keySet()) {
            prop.put("profile_" + c + "_filename", filename);
            prop.put("profile_" + c + "_description", performanceProfiles.get(filename));
            prop.put("profile_" + c + "_used", usedfile.equalsIgnoreCase(filename) ? "1" : "0");
            c++;
        }
        prop.put("profile", c);
        
        c = 0;
        final int[] speedValues = {200,150,100,50,25,10};
        final int usedspeed = sb.getConfigInt("performanceSpeed", 100);
        for(final int speed: speedValues){
        	prop.put("speed_" + c + "_value", speed);
        	prop.put("speed_" + c + "_label", speed + " %");
        	prop.put("speed_" + c + "_used", (speed == usedspeed) ? "1" : "0");
        	c++;
        }
        prop.put("speed", c);
        
        if ((post != null) && (post.containsKey("cacheSizeSubmit"))) {
            final int wordCacheMaxCount = post.getInt("wordCacheMaxCount", 20000);
            sb.setConfig(SwitchboardConstants.WORDCACHE_MAX_COUNT, Integer.toString(wordCacheMaxCount));
            indexSegment.termIndex().setBufferMaxWordCount(wordCacheMaxCount);
        }
        
        if ((post != null) && (post.containsKey("poolConfig"))) {
            
            /* 
             * configuring the crawler pool 
             */
            // get the current crawler pool configuration
            int maxBusy = post.getInt("Crawler Pool_maxActive", 8);
            
            // storing the new values into configfile
            sb.setConfig(SwitchboardConstants.CRAWLER_THREADS_ACTIVE_MAX,maxBusy);
            //switchboard.setConfig("crawler.MinIdleThreads",minIdle);
            
            /* 
             * configuring the http pool 
             */
            final WorkflowThread httpd = sb.getThread("10_httpd");
            try {
                maxBusy = post.getInt("httpd Session Pool_maxActive", 8);
            } catch (final NumberFormatException e) {
                maxBusy = 8;
            }

            ((serverCore)httpd).setMaxSessionCount(maxBusy);    
            
            // storing the new values into configfile
            sb.setConfig("httpdMaxBusySessions",maxBusy);

        }        
        
        if ((post != null) && (post.containsKey("PrioritySubmit"))) {
        	sb.setConfig("javastart_priority",post.get("YaCyPriority","0"));
        }
        
        if ((post != null) && (post.containsKey("onlineCautionSubmit"))) {
            sb.setConfig(SwitchboardConstants.PROXY_ONLINE_CAUTION_DELAY, Integer.toString(post.getInt("crawlPauseProxy", 30000)));
            sb.setConfig(SwitchboardConstants.LOCALSEACH_ONLINE_CAUTION_DELAY, Integer.toString(post.getInt("crawlPauseLocalsearch", 30000)));
            sb.setConfig(SwitchboardConstants.REMOTESEARCH_ONLINE_CAUTION_DELAY, Integer.toString(post.getInt("crawlPauseRemotesearch", 30000)));
        }
        
        if ((post != null) && (post.containsKey("minimumDeltaSubmit"))) {
            final long minimumLocalDelta = post.getLong("minimumLocalDelta", sb.crawlQueues.noticeURL.getMinimumLocalDelta());
            final long minimumGlobalDelta = post.getLong("minimumGlobalDelta", sb.crawlQueues.noticeURL.getMinimumGlobalDelta());
            sb.setConfig("minimumLocalDelta", minimumLocalDelta);
            sb.setConfig("minimumGlobalDelta", minimumGlobalDelta);
            sb.crawlQueues.noticeURL.setMinimumDelta(minimumLocalDelta, minimumGlobalDelta);
        }
        
        // delta settings
        prop.put("minimumLocalDelta", sb.crawlQueues.noticeURL.getMinimumLocalDelta());
        prop.put("minimumGlobalDelta", sb.crawlQueues.noticeURL.getMinimumGlobalDelta());
        
        // table cache settings
        prop.putNum("urlCacheSize", indexSegment.urlMetadata().writeCacheSize());  
        prop.putNum("wordCacheSize", indexSegment.termIndex().getBufferSize());
        prop.putNum("wordCacheSizeKBytes", indexSegment.termIndex().getBufferSizeBytes()/1024);
        prop.putNum("maxURLinCache", indexSegment.termIndex().getBufferMaxReferences());
        prop.putNum("maxAgeOfCache", indexSegment.termIndex().getBufferMaxAge() / 1000 / 60); // minutes
        prop.putNum("minAgeOfCache", indexSegment.termIndex().getBufferMinAge() / 1000 / 60); // minutes
        prop.putNum("maxWaitingWordFlush", sb.getConfigLong("maxWaitingWordFlush", 180));
        prop.put("wordCacheMaxCount", sb.getConfigLong(SwitchboardConstants.WORDCACHE_MAX_COUNT, 20000));
        prop.put("crawlPauseProxy", sb.getConfigLong(SwitchboardConstants.PROXY_ONLINE_CAUTION_DELAY, 30000));
        prop.put("crawlPauseLocalsearch", sb.getConfigLong(SwitchboardConstants.LOCALSEACH_ONLINE_CAUTION_DELAY, 30000));
        prop.put("crawlPauseRemotesearch", sb.getConfigLong(SwitchboardConstants.REMOTESEARCH_ONLINE_CAUTION_DELAY, 30000));
        prop.putNum("crawlPauseProxyCurrent", (System.currentTimeMillis() - sb.proxyLastAccess) / 1000);
        prop.putNum("crawlPauseLocalsearchCurrent", (System.currentTimeMillis() - sb.localSearchLastAccess) / 1000);
        prop.putNum("crawlPauseRemotesearchCurrent", (System.currentTimeMillis() - sb.remoteSearchLastAccess) / 1000);
        
        // table thread pool settings
        prop.put("pool_0_name","Crawler Pool");
        prop.put("pool_0_maxActive", sb.getConfigLong("crawler.MaxActiveThreads", 0));
        prop.put("pool_0_numActive",sb.crawlQueues.workerSize());
        
        final WorkflowThread httpd = sb.getThread("10_httpd");
        prop.put("pool_1_name", "httpd Session Pool");
        prop.put("pool_1_maxActive", ((serverCore)httpd).getMaxSessionCount());
        prop.put("pool_1_numActive", ((serverCore)httpd).getJobCount());
        
        prop.put("pool", "2");
        
        final long curr_prio = sb.getConfigLong("javastart_priority",0);
        prop.put("priority_normal",(curr_prio == 0) ? "1" : "0");
        prop.put("priority_below",(curr_prio == 10) ? "1" : "0");
        prop.put("priority_low",(curr_prio == 20) ? "1" : "0");
        
        // parse initialization memory settings
        final String Xmx = sb.getConfig("javastart_Xmx", "Xmx500m").substring(3);
        prop.put("Xmx", Xmx.substring(0, Xmx.length() - 1));
        final String Xms = sb.getConfig("javastart_Xms", "Xms500m").substring(3);
        prop.put("Xms", Xms.substring(0, Xms.length() - 1));
        
        final long diskFree = sb.getConfigLong(SwitchboardConstants.DISK_FREE, 3000L);
        final long diskFreeHardlimit = sb.getConfigLong(SwitchboardConstants.DISK_FREE_HARDLIMIT, 1000L);
        final long memoryAcceptDHT = sb.getConfigLong(SwitchboardConstants.MEMORY_ACCEPTDHT, 50000L);
        final boolean observerTrigger = !MemoryControl.getDHTallowed();
        prop.put("diskFree", diskFree);
        prop.put("diskFreeHardlimit", diskFreeHardlimit);
        prop.put("memoryAcceptDHT", memoryAcceptDHT);
        if(observerTrigger) prop.put("observerTrigger", "1");
        
        // return rewrite values for templates
        return prop;
    }
    
    private static String d(final String a, final String b) {
        return (a == null) ? b : a;
    }
}
