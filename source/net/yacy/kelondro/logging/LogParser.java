//LogParser.java 
//-------------------------------------
//part of YACY
//(C) by Michael Peter Christen; mc@yacy.net
//first published on http://www.anomic.de
//Frankfurt, Germany, 2004
//
//This file is contributed by Matthias Soehnholz
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
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

package net.yacy.kelondro.logging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class LogParser {
    
    /** the version of the LogParser - <strong>Double</strong>*/
    public static final String PARSER_VERSION           = "version";
    
    /** the amount of URLs received during DHT - <strong>Integer</strong> */
    public static final String URLS_RECEIVED            = "urlSum";
    
    /** the amount of URLs requested during DHT - <strong>Integer</strong> */
    public static final String URLS_REQUESTED           = "urlReqSum";
    
    /** the amount of URLs blocked during DHT because they match the peer's blacklist - <strong>Integer</strong> */
    public static final String URLS_BLOCKED             = "blockedURLSum";
    
    /** the amount of words received during DHT - <strong>Integer</strong> */
    public static final String WORDS_RECEIVED           = "wordsSum";
    
    /** the amount of RWIs received during DHT - <strong>Integer</strong> */
    public static final String RWIS_RECEIVED            = "rwiSum";
    
    /** the amount of RWIs blocked during DHT because their entries match the peer's blacklist - <strong>Integer</strong> */
    public static final String RWIS_BLOCKED             = "blockedRWISum";
    
    /** total time receiving RWIs during DHT in milli seconds - <strong>Long</strong> */
    public static final String RWIS_RECEIVED_TIME       = "rwiTimeSum";
    
    /** total time receiving URLs during DHT in milli seconds - <strong>Long</strong> */
    public static final String URLS_RECEIVED_TIME       = "urlTimeSum";
    
    /** the traffic sent during DHT in bytes - <strong>Long</strong> */
    public static final String DHT_TRAFFIC_SENT         = "DHTSendTraffic";
    
    /** the amount of URLs requested by other peers and sent by this one - <strong>Integer</strong> */
    public static final String DHT_URLS_SENT            = "DHTSendURLs";
    
    /** the amount of rejected DHT transfers from other peers (i.e. because this peer was busy) - <strong>Integer</strong> */
    public static final String DHT_REJECTED             = "RWIRejectCount";
    
    /** the peer-names from whose DHT transfers were rejected - <strong>HasSet</strong> */
    public static final String DHT_REJECTED_PEERS_NAME  = "DHTRejectPeerNames";
    
    /** the peer-hashes from whose DHT transfers were rejected - <strong>HasSet</strong> */
    public static final String DHT_REJECTED_PEERS_HASH  = "DHTRejectPeerHashs";
    
    /** the peer-names this peer sent DHT chunks to - <strong>HasSet</strong> */
    public static final String DHT_SENT_PEERS_NAME      = "DHTPeerNames";
    
    /** the peer-hashes this peer sent DHT chunks to - <strong>HasSet</strong> */
    public static final String DHT_SENT_PEERS_HASH      = "DHTPeerHashs";
    
    /** total amount of selected peers for index distribution - <strong>Integer</strong> */
    public static final String DHT_SELECTED             = "DHTSelectionTargetCount";
    
    /** total amount of words selected for index distribution - <strong>Integer</strong> */
    public static final String DHT_WORDS_SELECTED       = "DHTSelectionWordsCount";
    
    /** total time selecting words for index distribution - <strong>Integer</strong> */
    public static final String DHT_WORDS_SELECTED_TIME  = "DHTSelectionWordsTimeCount";
    
    /** the minimal DHT distance during peer-selection for index distribution - <strong>Long</strong> */
    public static final String DHT_DISTANCE_MIN         = "minDHTDist";
    
    /** the maximal DHT distance during peer-selection for index distribution - <strong>Long</strong> */
    public static final String DHT_DISTANCE_MAX         = "maxDHTDist";
    
    /** the average DHT distance during peer-selection for index distribution - <strong>Long</strong> */
    public static final String DHT_DISTANCE_AVERAGE     = "avgDHTDist";
    
    /** how many times remote peers were too busy to accept the index transfer - <strong>Integer</strong> */
    public static final String PEERS_BUSY               = "busyPeerCount";
    
    /** how many times not enough peers for index distribution were found - <strong>Integer</strong> */
    public static final String PEERS_TOO_LESS           = "notEnoughDHTPeers";
    
    /** how many times the index distribution failed (i.e. due to time-out or other reasons) - <strong>Integer</strong> */
    public static final String DHT_SENT_FAILED          = "failedIndexDistributionCount";
    
    /** how many times the error "<code>tried to create left child-node twice</code>" occured - <strong>Integer</strong> */
    public static final String ERROR_CHILD_TWICE_LEFT   = "leftChildTwiceCount";
    
    /** how many times the error "<code>tried to create right child-node twice</code>" occured - <strong>Integer</strong> */
    public static final String ERROR_CHILD_TWICE_RIGHT  = "rightChildTwiceCount";
    
    /** how many ranking distributions were executed successfully - <strong>Integer</strong> */
    public static final String RANKING_DIST             = "rankingDistributionCount";
    
    /** total time the ranking distributions took - <strong>Integer</strong> */
    public static final String RANKING_DIST_TIME        = "rankingDistributionTime";
    
    /** how many ranking distributions failed - <strong>Integer</strong> */
    public static final String RANKING_DIST_FAILED      = "rankingDistributionFailCount";
    
    /** how many times the error "<code>Malformed URL</code>" occured - <strong>Integer</strong> */
    public static final String ERROR_MALFORMED_URL      = "malformedURLCount";
    
    /** the amount of indexed sites - <strong>Integer</strong> */
    public static final String INDEXED_SITES            = "indexedSites";
    
    /** total amount of indexed words - <strong>Integer</strong> */
    public static final String INDEXED_WORDS            = "indexedWords";
    
    /** total size of all indexed sites - <strong>Integer</strong> */
    public static final String INDEXED_SITES_SIZE       = "indexedSiteSizeSum";
    
    /** total amount of indexed anchors - <strong>Integer</strong> */
    public static final String INDEXED_ANCHORS          = "indexedAnchors";
    
//    /** total time needed for stacking the site of an indexing - <strong>Integer</strong> */
//    public static final String INDEXED_STACK_TIME       = "indexedStackingTime";
//    
//    /** total time needed for parsing during indexing - <strong>Integer</strong> */
//    public static final String INDEXED_PARSE_TIME       = "indexedParsingTime";
//    
//    /** total time needed for the actual indexing during indexing - <strong>Integer</strong> */
//    public static final String INDEXED_INDEX_TIME       = "indexedIndexingTime";
//    
//    /** total time needed for storing the results of an indexing - <strong>Integer</strong> */
//    public static final String INDEXED_STORE_TIME       = "indexedStorageTime";
    
    /** total time needed for storing the results of a link indexing - <strong>Integer</strong> */
    public static final String INDEXED_LINKSTORE_TIME       = "indexedLinkStorageTime";
    
    /** total time needed for storing the results of a word indexing - <strong>Integer</strong> */
    public static final String INDEXED_INDEXSTORE_TIME       = "indexedIndexStorageTime";
    
    /** accumulated time needed to parse the log entries up to now (in ms)*/
    public static final String TOTAL_PARSER_TIME        = "totalParserTime";
    
    /** times the parser was called, respectively amount of independent log-lines */
    public static final String TOTAL_PARSER_RUNS        = "totalParserRuns";
    
    
    private static final float parserVersion = 0.1f;
    private static final String parserType = "PLASMA";

    //RegExp for LogLevel I
    private static final Pattern i1 = Pattern.compile("Received (\\d*) URLs from peer [\\w-_]{12}:[\\w-_]*/[\\w.-]* in (\\d*) ms, blocked (\\d*) URLs");
    private static final Pattern i2 = Pattern.compile("Received (\\d*) Entries (\\d*) Words \\[[\\w-_]{12} .. [\\w-_]{12}\\]/[\\w.-]* from [\\w-_]{12}:[\\w-_]*/[\\w.-]*, processed in (\\d*) milliseconds, requesting (\\d*)/(\\d*) URLs, blocked (\\d*) RWIs");
    private static final Pattern i2_2 = Pattern.compile("Received (\\d*) Entries (\\d*) Words \\[[\\w-_]{12} .. [\\w-_]{12}\\]/[\\w.-]* from [\\w-_]{12}:[\\w-_]*, processed in (\\d*) milliseconds, requesting (\\d*)/(\\d*) URLs, blocked (\\d*) RWIs");
    private static final Pattern i3 = Pattern.compile("Index transfer of (\\d*) words \\[[\\w-_]{12} .. [\\w-_]{12}\\] to peer ([\\w-_]*):([\\w-_]{12}) in (\\d*) seconds successful \\((\\d*) words/s, (\\d*) Bytes\\)");
    private static final Pattern i4 = Pattern.compile("Index transfer of (\\d*) entries (\\d*) words \\[[\\w-_]{12} .. [\\w-_]{12}\\] and (\\d*) URLs to peer ([\\w-_]*):([\\w-_]{12}) in (\\d*) seconds successful \\((\\d*) words/s, (\\d*) Bytes\\)");
    private static final Pattern i5 = Pattern.compile("Selected DHT target peer ([\\w-_]*):([\\w-_]{12}), distance2first = ([\\d]*), distance2last = ([\\d]*)");
    private static final Pattern i6 = Pattern.compile("Rejecting RWIs from peer ([\\w-_]{12}):([\\w-_]*)/([\\w.]*). ([\\w. ]*)");
    private static final Pattern i7 = Pattern.compile("DHT distribution: transfer to peer [\\w-]* finished.");
    private static final Pattern i8 = Pattern.compile("Index selection of (\\d*) words \\[[\\w-_]{12} .. [\\w-_]{12}\\] in (\\d*) seconds");
    private static final Pattern i9 = Pattern.compile("RankingDistribution - transmitted file [\\w\\s-:.\\\\]* to [\\w.]*:\\d* successfully in (\\d)* seconds");
    private static final Pattern i10 = Pattern.compile("RankingDistribution - error transmitting file");
    private static final Pattern i11 = Pattern.compile("Peer [\\w-_]*:[\\w-_]{12} is busy\\. Waiting \\d* ms\\.");
    //private static Pattern i12 = Pattern.compile("\\*Indexed \\d* words in URL [\\w:.&/%-~$\u00A7@=]* \\[[\\w-_]{12}\\]");
    private static final Pattern i13 = Pattern.compile("WROTE HEADER for |LOCALCRAWL\\[\\d*, \\d*, \\d*, \\d*\\]|REJECTED WRONG STATUS TYPE");
    //RegExp for LogLevel W
    private static final Pattern w1 = Pattern.compile("found not enough \\(\\d*\\) peers for distribution");
    private static final Pattern w2 = Pattern.compile("Transfer to peer ([\\w-_]*):([\\w-_]{12}) failed:'(\\w*)'");
    //RegExp for LogLevel E
    private static final Pattern e1 = Pattern.compile("INTERNAL ERROR AT plasmaCrawlLURL:store:de.anomic.kelondro.kelondroException: tried to create (\\w*) node twice in db");
    private static final Pattern e2 = Pattern.compile("INTERNAL ERROR [\\w./: ]* java.net.MalformedURLException");

    private Matcher m;
    //RegExp for advancedParser
    //private Pattern adv1 = Pattern.compile("\\*Indexed (\\d*) words in URL [\\w:.&?/%-=]* \\[[\\w-_]{12}\\]\\n\\tDescription: ([\\w- ]*)\\n\\tMimeType: ([\\w-_/]*) \\| Size: (\\d*) bytes \\| Anchors: (\\d*)\\n\\tStackingTime: (\\d*) ms \\| ParsingTime: (\\d*) ms \\| IndexingTime: (\\d*) ms \\| StorageTime: (\\d*) ms");
    private final static Pattern adv1 = Pattern.compile(
            "\\*Indexed (\\d+) words in URL [\\w:.&/%-~;$\u00A7@=]* \\[[\\w_-]{12}\\]\\r?\\n?" + 
            "\\tDescription: +([\\w-\\.,:!='\"|/+@\\(\\) \\t]*)\\r?\\n?" +
            "\\tMimeType: ([\\w_~/-]*) \\| Charset: ([\\w-]*) \\| Size: (\\d+) bytes \\| Anchors: (\\d+)\\r?\\n?" +
    		"\\tLinkStorageTime: (\\d+) ms \\| indexStorageTime: (\\d+) ms");
            //"\\tStackingTime:[ ]*(\\d+) ms \\| ParsingTime:[ ]*(\\d+) ms \\| IndexingTime: (\\d+) ms \\| StorageTime: (\\d+) ms");

    private int urlSum=0;
    private int urlReqSum=0;
    private int blockedURLSum=0;
    private int wordsSum=0;
    private int rwiSum=0;
    private int blockedRWISum=0;
    private long urlTimeSum=0;
    private long rwiTimeSum=0;
    private long DHTSendTraffic=0;
    private int DHTSendURLs=0;
    private int RWIRejectCount=0;
    private final Set<String> RWIRejectPeerNames = new HashSet<String>();
    private final Set<String> RWIRejectPeerHashs = new HashSet<String>();
    private final Set<String> DHTPeerNames = new HashSet<String>();
    private final Set<String> DHTPeerHashs = new HashSet<String>();
    private int DHTSelectionTargetCount = 1;
    private int DHTSelectionWordsCount = 0;
    private int DHTSelectionWordsTimeCount = 0;
    private long minDHTDist = Long.MAX_VALUE;
    private long maxDHTDist = 0;
    private long avgDHTDist = 0;
    private int busyPeerCount = 0;
    private int notEnoughDHTPeers = 0;
    private int failedIndexDistributionCount = 0;
    private int leftChildTwiceCount = 0;
    private int rightChildTwiceCount = 0;
    private int rankingDistributionCount = 0;
    private int rankingDistributionTime = 0;
    private int rankingDistributionFailCount = 0;
    private int malformedURLCount = 0;
    private int indexedSites = 0;
    private int indexedWordSum = 0;
    private int indexedSiteSizeSum = 0;
    private int indexedAnchorsCount = 0;
    private int indexedLinkStorageTime = 0;
    private int indexedIndexStorageTime = 0;
//    private int indexedStackingTime = 0;
//    private int indexedParsingTime = 0;
//    private int indexedIndexingTime = 0;
//    private int indexedStorageTime = 0;
    private long totalParserTime = 0;
    private int totalParserRuns = 0;
    
    public final int parse(final String logLevel, final String logLine) {
        final long start = System.currentTimeMillis();
        if ("INFO".equals(logLevel)){
            m = i1.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 3) {
                urlSum += Integer.parseInt(m.group(1));
                urlTimeSum += Integer.parseInt(m.group(2));
                blockedURLSum += Integer.parseInt(m.group(3));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i2.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 6) {
                rwiSum += Integer.parseInt(m.group(1));
                wordsSum += Integer.parseInt(m.group(2));
                rwiTimeSum += Integer.parseInt(m.group(3));
                urlReqSum += Integer.parseInt(m.group(4));
                blockedRWISum += Integer.parseInt(m.group(6));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i2_2.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 6) {
                rwiSum += Integer.parseInt(m.group(1));
                wordsSum += Integer.parseInt(m.group(2));
                rwiTimeSum += Integer.parseInt(m.group(3));
                urlReqSum += Integer.parseInt(m.group(4));
                blockedRWISum += Integer.parseInt(m.group(6));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i3.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 6) {
                DHTSendTraffic += Integer.parseInt(m.group(6));
                DHTPeerNames.add(m.group(2));
                DHTPeerHashs.add(m.group(3));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i4.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 8) {
                DHTSendTraffic += Integer.parseInt(m.group(8));
                DHTSendURLs += Integer.parseInt(m.group(3));
                DHTPeerNames.add(m.group(4));
                DHTPeerHashs.add(m.group(5));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i5.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 4) {
                minDHTDist = Math.min(minDHTDist, Math.min(Long.parseLong(m.group(3)), Long.parseLong(m.group(4))));
                maxDHTDist = Math.max(maxDHTDist, Math.max(Long.parseLong(m.group(3)), Long.parseLong(m.group(4))));
                avgDHTDist += Long.parseLong(m.group(3));
                DHTSelectionTargetCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i6.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 2) {
                RWIRejectPeerNames.add(m.group(2));
                RWIRejectPeerHashs.add(m.group(1));
                RWIRejectCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i7.matcher (logLine);
            
            if (m.find ()) {
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i8.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 2) {
                DHTSelectionWordsCount += Float.parseFloat(m.group(1));
                DHTSelectionWordsTimeCount += Float.parseFloat(m.group(2));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i9.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 1) {
                rankingDistributionCount++;
                rankingDistributionTime += Integer.parseInt(m.group(1));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i10.matcher (logLine);
            
            if (m.find ()) {
                rankingDistributionFailCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = i11.matcher (logLine);
            
            if (m.find ()) {
                busyPeerCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
//            m = i12.matcher (logLine);
//            
//            if (m.find ()) {
//                return 3;
//            }
            m = i13.matcher (logLine);
            
            if (m.find ()) {
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = adv1.matcher (logLine);
            
            if (m.find() && m.groupCount() >= 8) {
                indexedSites++;
                indexedWordSum += Integer.parseInt(m.group(1));
                indexedSiteSizeSum += Integer.parseInt(m.group(5));
                indexedAnchorsCount += Integer.parseInt(m.group(6));
                indexedLinkStorageTime += Integer.parseInt(m.group(7));
                indexedIndexStorageTime += Integer.parseInt(m.group(8));
//                indexedStackingTime += Integer.parseInt(m.group(7));
//                indexedParsingTime += Integer.parseInt(m.group(8));
//                indexedIndexingTime += Integer.parseInt(m.group(9));
//                indexedStorageTime += Integer.parseInt(m.group(10));
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }

        } else if ("WARNING".equals(logLevel)){
            m = w1.matcher (logLine);
            
            if (m.find ()) {
                notEnoughDHTPeers++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = w2.matcher (logLine);
            
            if (m.find ()) {
                failedIndexDistributionCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
        } else if ("SEVERE".equals(logLevel)){
            m = e1.matcher (logLine);
            
            if (m.find () && m.groupCount() >= 1) {
                if ("leftchild".equals(m.group(1))) leftChildTwiceCount++;
                else if ("rightchild".equals(m.group(1))) rightChildTwiceCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
            m = e2.matcher (logLine);
            
            if (m.find ()) {
                malformedURLCount++;
                totalParserTime += (System.currentTimeMillis() - start);
                totalParserRuns++;
                return 0;
            }
        }
        totalParserTime += (System.currentTimeMillis() - start);
        totalParserRuns++;
        return -1;
    }

    public final Map<String, Object> getResults() {
        final Map<String, Object> results = new HashMap<String, Object>();
        results.put(PARSER_VERSION          , Float.valueOf(parserVersion));
        results.put(URLS_RECEIVED           , Integer.valueOf(urlSum));
        results.put(URLS_REQUESTED          , Integer.valueOf(urlReqSum));
        results.put(URLS_BLOCKED            , Integer.valueOf(blockedURLSum));
        results.put(WORDS_RECEIVED          , Integer.valueOf(wordsSum));
        results.put(RWIS_RECEIVED           , Integer.valueOf(rwiSum));
        results.put(RWIS_BLOCKED            , Integer.valueOf(blockedRWISum));
        results.put(URLS_RECEIVED_TIME      , Long.valueOf(urlTimeSum));
        results.put(RWIS_RECEIVED_TIME      , Long.valueOf(rwiTimeSum));
        results.put(DHT_TRAFFIC_SENT        , Long.valueOf(DHTSendTraffic));
        results.put(DHT_URLS_SENT           , Integer.valueOf(DHTSendURLs));
        results.put(DHT_REJECTED            , Integer.valueOf(RWIRejectCount));
        results.put(DHT_REJECTED_PEERS_NAME , RWIRejectPeerNames);
        results.put(DHT_REJECTED_PEERS_HASH , RWIRejectPeerHashs);
        results.put(DHT_SENT_PEERS_NAME     , DHTPeerNames);
        results.put(DHT_SENT_PEERS_HASH     , DHTPeerHashs);
        results.put(DHT_SELECTED            , Integer.valueOf(DHTSelectionTargetCount));
        results.put(DHT_WORDS_SELECTED      , Integer.valueOf(DHTSelectionWordsCount));
        results.put(DHT_WORDS_SELECTED_TIME , Integer.valueOf(DHTSelectionWordsTimeCount));
        results.put(DHT_DISTANCE_MIN        , Long.valueOf(minDHTDist));
        results.put(DHT_DISTANCE_MAX        , Long.valueOf(maxDHTDist));
        results.put(DHT_DISTANCE_AVERAGE    , Long.valueOf(avgDHTDist / DHTSelectionTargetCount / Long.MAX_VALUE)); //FIXME: broken avg
        results.put(PEERS_BUSY              , Integer.valueOf(busyPeerCount));
        results.put(PEERS_TOO_LESS          , Integer.valueOf(notEnoughDHTPeers));
        results.put(DHT_SENT_FAILED         , Integer.valueOf(failedIndexDistributionCount));
        results.put(ERROR_CHILD_TWICE_LEFT  , Integer.valueOf(leftChildTwiceCount));
        results.put(ERROR_CHILD_TWICE_RIGHT , Integer.valueOf(rightChildTwiceCount));
        results.put(RANKING_DIST            , Integer.valueOf(rankingDistributionCount));
        results.put(RANKING_DIST_TIME       , Integer.valueOf(rankingDistributionTime));
        results.put(RANKING_DIST_FAILED     , Integer.valueOf(rankingDistributionFailCount));
        results.put(ERROR_MALFORMED_URL     , Integer.valueOf(malformedURLCount));
        results.put(INDEXED_SITES           , Integer.valueOf(indexedSites));
        results.put(INDEXED_WORDS           , Integer.valueOf(indexedWordSum));
        results.put(INDEXED_SITES_SIZE      , Integer.valueOf(indexedSiteSizeSum));
        results.put(INDEXED_ANCHORS         , Integer.valueOf(indexedAnchorsCount));
//        results.put(INDEXED_STACK_TIME      , new Integer(indexedStackingTime));
//        results.put(INDEXED_PARSE_TIME      , new Integer(indexedParsingTime));
//        results.put(INDEXED_INDEX_TIME      , new Integer(indexedIndexingTime));
//        results.put(INDEXED_STORE_TIME      , new Integer(indexedStorageTime));
        results.put(INDEXED_LINKSTORE_TIME , Integer.valueOf(indexedLinkStorageTime));
        results.put(INDEXED_INDEXSTORE_TIME, Integer.valueOf(indexedIndexStorageTime));
        results.put(TOTAL_PARSER_TIME      , Long.valueOf(totalParserTime));
        results.put(TOTAL_PARSER_RUNS      , Integer.valueOf(totalParserRuns));
        return results;
    }
    
    public final String getParserType() {
        return parserType;
    }

    public final double getParserVersion() {
        return parserVersion;
    }

    public final void printResults() {
        if(rankingDistributionCount == 0) rankingDistributionCount = 1;
        if(DHTSelectionWordsTimeCount == 0) DHTSelectionWordsTimeCount = 1;
        if(indexedSites != 0) indexedSites++;
        System.out.println("INDEXER: Indexed " + indexedSites + " sites in " + (indexedLinkStorageTime + indexedIndexStorageTime) + " milliseconds.");
        System.out.println("INDEXER: Indexed " + indexedWordSum + " words on " + indexedSites + " sites. (avg. words per site: " + (indexedWordSum / indexedSites) + ").");
        System.out.println("INDEXER: Total Size of indexed sites: " + indexedSiteSizeSum + " bytes (avg. size per site: " + (indexedSiteSizeSum / indexedSites) + " bytes).");
        System.out.println("INDEXER: Total Number of Anchors found: " + indexedAnchorsCount + "(avg. Anchors per site: " + (indexedAnchorsCount / indexedSites) + ").");
        System.out.println("INDEXER: Total LinkStorageTime: " + indexedLinkStorageTime + " milliseconds (avg. StorageTime: " + (indexedLinkStorageTime / indexedSites) + " milliseconds)."); 
        System.out.println("INDEXER: Total indexStorageTime: " + indexedIndexStorageTime + " milliseconds (avg. StorageTime: " + (indexedIndexStorageTime / indexedSites) + " milliseconds)."); 
//        System.out.println("INDEXER: Total StackingTime: " + indexedStackingTime + " milliseconds (avg. StackingTime: " + (indexedStackingTime / indexedSites) + " milliseconds).");
//        System.out.println("INDEXER: Total ParsingTime: " + indexedParsingTime + " milliseconds (avg. ParsingTime: " + (indexedParsingTime / indexedSites) + " milliseconds).");
//        System.out.println("INDEXER: Total IndexingTime: " + indexedIndexingTime + " milliseconds (avg. IndexingTime: " + (indexedIndexingTime / indexedSites) + " milliseconds).");
//        System.out.println("INDEXER: Total StorageTime: " + indexedStorageTime + " milliseconds (avg. StorageTime: " + (indexedStorageTime / indexedSites) + " milliseconds)."); 
        if(urlSum != 0) urlSum++;
        System.out.println("DHT: Recieved " + urlSum + " Urls in " + urlTimeSum + " ms. Blocked " + blockedURLSum + " URLs.");
        System.out.println("DHT: " + urlTimeSum / urlSum + " milliseconds per URL.");            
        if(rwiSum != 0) rwiSum++;
        System.out.println("DHT: Recieved " + rwiSum + " RWIs from " + wordsSum + " Words in " + rwiTimeSum + " ms. " + urlReqSum + " requested URLs.");
        System.out.println("DHT: Blocked " + blockedRWISum + " RWIs before requesting URLs, because URL-Hash was blacklisted.");
        System.out.println("DHT: " + rwiTimeSum / rwiSum + " milliseconds per RWI.");            
        System.out.println("DHT: Rejected " + RWIRejectCount + " Indextransfers from " + RWIRejectPeerNames.size() + " PeerNames with " + RWIRejectPeerHashs.size() + " PeerHashs.");
        System.out.println("DHT: " + DHTSendTraffic/(1024*1024l) + " MegaBytes (" + DHTSendTraffic + " Bytes) of DHT-Transfertraffic.");
        System.out.println("DHT: Sended " + DHTSendURLs + " URLs via DHT.");
        System.out.println("DHT: DHT Transfers send to " + DHTPeerNames.size() + " Peernames with " + DHTPeerHashs.size() + " Peerhashs.");
        System.out.println("DHT: Totally selected " + DHTSelectionWordsCount + " words in " + DHTSelectionWordsTimeCount + " seconds (" + (float)DHTSelectionWordsCount/DHTSelectionWordsTimeCount + " words/s)");
        System.out.println("DHT: Selected " + DHTSelectionTargetCount + " possible DHT Targets (min. Distance: " + minDHTDist + " max. Distance: " + maxDHTDist + " avg. Distance: " + (avgDHTDist/DHTSelectionTargetCount));
        System.out.println("DHT: " + busyPeerCount + " times a targetpeer was too busy to accept a transfer.");
        System.out.println("DHT: " + notEnoughDHTPeers + " times there were not enought targetpeers for the selected DHTChunk");
        System.out.println("DHT: IndexDistribution failed " + failedIndexDistributionCount + " times.");
        System.out.println("RANKING: Transmitted " + rankingDistributionCount + " Rankingfiles in " + rankingDistributionTime + " seconds (" + rankingDistributionTime/rankingDistributionCount + " seconds/file)");
        System.out.println("RANKING: RankingDistribution failed " + rankingDistributionFailCount + " times.");
        if (leftChildTwiceCount != 0)
            System.out.println("ERRORS: tried " + leftChildTwiceCount + " times to create leftchild node twice in db");
        if (rightChildTwiceCount != 0)
            System.out.println("ERRORS: tried " + rightChildTwiceCount + " times to create rightchild node twice in db");
        if (malformedURLCount != 0)
            System.out.println("ERRORS: " + malformedURLCount + " MalformedURLExceptions accord.");
    }

}
