// CrawlResults.java
// (C) 2005 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 09.03.2005 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.data.meta.URIMetadataRow;
import net.yacy.kelondro.logging.Log;

import de.anomic.crawler.ResultURLs;
import de.anomic.crawler.ResultURLs.EventOrigin;
import de.anomic.crawler.ResultURLs.InitExecEntry;
import de.anomic.search.Segments;
import de.anomic.search.Switchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.tools.nxTools;
import de.anomic.yacy.yacySeed;

public class CrawlResults {

    public static serverObjects respond(final RequestHeader header, serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();

        int lines = 500;
        boolean showInit = env.getConfigBool("IndexMonitorInit", false);
        boolean showExec = env.getConfigBool("IndexMonitorExec", false);
        boolean showDate = env.getConfigBool("IndexMonitorDate", true);
        boolean showWords = env.getConfigBool("IndexMonitorWords", true);
        boolean showTitle = env.getConfigBool("IndexMonitorTitle", true);
        boolean showURL = env.getConfigBool("IndexMonitorURL", true);

        if (post == null) {
            post = new serverObjects();
            post.put("process", "0");
        }

        // find process number
        EventOrigin tabletype;
        try {
            tabletype = EventOrigin.getEvent(post.getInt("process", 0));
        } catch (final NumberFormatException e) {
            tabletype = EventOrigin.UNKNOWN;
        }

        if (
            post != null &&
            post.containsKey("autoforward") &&
            tabletype == EventOrigin.LOCAL_CRAWLING &&
            ResultURLs.getStackSize(EventOrigin.LOCAL_CRAWLING) == 0) {
            // the main menu does a request to the local crawler page, but in case this table is empty, the overview page is shown
            tabletype = (ResultURLs.getStackSize(EventOrigin.SURROGATES) == 0) ? EventOrigin.UNKNOWN : EventOrigin.SURROGATES;
        }
        
        // check if authorization is needed and/or given
        if (tabletype != EventOrigin.UNKNOWN ||
            (post != null && (post.containsKey("clearlist") ||
            post.containsKey("deleteentry")))) {
            final String authorization = (header.get(RequestHeader.AUTHORIZATION, "xxxxxx"));
            if (authorization.length() != 0) {
                if (! sb.verifyAuthentication(header, true)){
                    // force log-in (again, because wrong password was given)
                    prop.put("AUTHENTICATE", "admin log-in");
                    return prop;
                }
            } else {
                // force log-in
                prop.put("AUTHENTICATE", "admin log-in");
                return prop;
            }
        }

        if (post != null) {
            // custom number of lines
            if (post.containsKey("count")) {
                lines = post.getInt("count", 500);
            }

            // do the commands
            if (post.containsKey("clearlist")) ResultURLs.clearStack(tabletype);

            if (post.containsKey("deleteentry")) {
                final String hash = post.get("hash", null);
                if (hash != null) {
                    // delete from database
                    sb.indexSegments.urlMetadata(Segments.Process.LOCALCRAWLING).remove(hash.getBytes());
                }
            }

            if (post.containsKey("deletedomain")) {
                final String hashpart = post.get("hashpart", null);
                final String domain = post.get("domain", null);
                if (hashpart != null) {
                    // delete all urls for this domain from database
                    try {
                        sb.indexSegments.urlMetadata(Segments.Process.LOCALCRAWLING).deleteDomain(hashpart);
                        ResultURLs.deleteDomain(tabletype, domain, hashpart);
                    } catch (IOException e) {
                        Log.logException(e);
                    }
                }
            }

            if (post.containsKey("moreIndexed")) {
                lines = post.getInt("showIndexed", 500);
            }

            if (post.get("si") != null) showInit = !("0".equals(post.get("si")));
            if (post.get("se") != null) showExec = !("0".equals(post.get("se")));
            if (post.get("sd") != null) showDate = !("0".equals(post.get("sd")));
            if (post.get("sw") != null) showWords = !("0".equals(post.get("sw")));
            if (post.get("st") != null) showTitle = !("0".equals(post.get("st")));
            if (post.get("su") != null) showURL = !("0".equals(post.get("su")));
        } // end != null

        // create table
        if (tabletype == EventOrigin.UNKNOWN) {
            prop.put("table", "2");
        } else if (ResultURLs.getStackSize(tabletype) == 0 && ResultURLs.getDomainListSize(tabletype) == 0) {
            prop.put("table", "0");
        } else {
            prop.put("table", "1");
            if (lines > ResultURLs.getStackSize(tabletype)) lines = ResultURLs.getStackSize(tabletype);
            if (lines == ResultURLs.getStackSize(tabletype)) {
                prop.put("table_size", "0");
            } else {
                prop.put("table_size", "1");
                prop.put("table_size_count", lines);
            }
            prop.put("table_size_all", ResultURLs.getStackSize(tabletype));
            
            prop.putHTML("table_feedbackpage", "CrawlResults.html");
            prop.put("table_tabletype", tabletype.getCode());
            prop.put("table_showInit", (showInit) ? "1" : "0");
            prop.put("table_showExec", (showExec) ? "1" : "0");
            prop.put("table_showDate", (showDate) ? "1" : "0");
            prop.put("table_showWords", (showWords) ? "1" : "0");
            prop.put("table_showTitle", (showTitle) ? "1" : "0");
            prop.put("table_showURL", (showURL) ? "1" : "0");

            boolean dark = true;
            String urlstr, urltxt;
            yacySeed initiatorSeed, executorSeed;
            URIMetadataRow urle;
            URIMetadataRow.Components metadata;

            int cnt = 0;
            final Iterator<Map.Entry<String, InitExecEntry>> i = ResultURLs.results(tabletype);
            Map.Entry<String, InitExecEntry> entry;
            while (i.hasNext()) {
                entry = i.next();
                try {
                    urle = sb.indexSegments.urlMetadata(Segments.Process.LOCALCRAWLING).load(UTF8.getBytes(entry.getKey()));
                    if (urle == null) {
                        Log.logWarning("PLASMA", "CrawlResults: URL not in index with url hash " + entry.getKey());
                        urlstr = null;
                        urltxt = null;
                        metadata = null;
                        continue;
                    }
                    metadata = urle.metadata();
                    urlstr = metadata.url().toNormalform(false, true);
                    urltxt = nxTools.shortenURLString(urlstr, 72); // shorten the string text like a URL
                    
                    initiatorSeed = entry.getValue() == null || entry.getValue().initiatorHash == null ? null : sb.peers.getConnected(UTF8.String(entry.getValue().initiatorHash));
                    executorSeed = entry.getValue() == null || entry.getValue().executorHash == null ? null : sb.peers.getConnected(UTF8.String(entry.getValue().executorHash));

                    prop.put("table_indexed_" + cnt + "_dark", (dark) ? "1" : "0");
                    prop.put("table_indexed_" + cnt + "_feedbackpage", "CrawlResults.html");
                    prop.put("table_indexed_" + cnt + "_tabletype", tabletype.getCode());
                    prop.put("table_indexed_" + cnt + "_urlhash", entry.getKey());

                    if (showInit) {
                        prop.put("table_indexed_" + cnt + "_showInit", "1");
                        prop.put("table_indexed_" + cnt + "_showInit_initiatorSeed", (initiatorSeed == null) ? "unknown" : initiatorSeed.getName());
                    } else
                        prop.put("table_indexed_" + cnt + "_showInit", "0");

                    if (showExec) {
                        prop.put("table_indexed_" + cnt + "_showExec", "1");
                        prop.put("table_indexed_" + cnt + "_showExec_executorSeed", (executorSeed == null) ? "unknown" : executorSeed.getName());
                    } else
                        prop.put("table_indexed_" + cnt + "_showExec", "0");

                    if (showDate && urle != null) {
                        prop.put("table_indexed_" + cnt + "_showDate", "1");
                        prop.put("table_indexed_" + cnt + "_showDate_modified", daydate(urle.moddate()));
                    } else
                        prop.put("table_indexed_" + cnt + "_showDate", "0");

                    if (showWords && urle != null) {
                        prop.put("table_indexed_" + cnt + "_showWords", "1");
                        prop.put("table_indexed_" + cnt + "_showWords_count", urle.wordCount());
                    } else
                        prop.put("table_indexed_" + cnt + "_showWords", "0");

                    if (showTitle) {
                        prop.put("table_indexed_" + cnt + "_showTitle", (showTitle) ? "1" : "0");
                            prop.put("table_indexed_" + cnt + "_showTitle_available", "1");

                            if (metadata == null || metadata.dc_title() == null || metadata.dc_title().trim().length() == 0)
                                prop.put("table_indexed_" + cnt + "_showTitle_available_nodescr", "0");
                            else {
                                prop.put("table_indexed_" + cnt + "_showTitle_available_nodescr", "1");
                                prop.putHTML("table_indexed_" + cnt + "_showTitle_available_nodescr_urldescr", metadata.dc_title());
                            }

                            prop.put("table_indexed_" + cnt + "_showTitle_available_urlHash", entry.getKey());
                            prop.putHTML("table_indexed_" + cnt + "_showTitle_available_urltitle", urlstr);
                    } else
                        prop.put("table_indexed_" + cnt + "_showTitle", "0");

                    if (showURL) {
                        prop.put("table_indexed_" + cnt + "_showURL", "1");
                            prop.put("table_indexed_" + cnt + "_showURL_available", "1");

                            prop.put("table_indexed_" + cnt + "_showURL_available_urlHash", entry.getKey());
                            prop.putHTML("table_indexed_" + cnt + "_showURL_available_urltitle", urlstr);
                            prop.put("table_indexed_" + cnt + "_showURL_available_url", urltxt);
                    } else
                        prop.put("table_indexed_" + cnt + "_showURL", "0");

                    dark = !dark;
                    cnt++;
                } catch (final Exception e) {
                    Log.logSevere("PLASMA", "genTableProps", e);
                }
            }
            prop.put("table_indexed", cnt);
            
            cnt = 0;
            dark = true;
            final Iterator<String> j = ResultURLs.domains(tabletype);
            String domain;
            while (j.hasNext() && cnt < 100) {
                domain = j.next();
                if (domain == null) break;
                prop.put("table_domains_" + cnt + "_dark", (dark) ? "1" : "0");
                prop.put("table_domains_" + cnt + "_feedbackpage", "CrawlResults.html");
                prop.put("table_domains_" + cnt + "_tabletype", tabletype.getCode());
                prop.put("table_domains_" + cnt + "_domain", domain);
                prop.put("table_domains_" + cnt + "_hashpart", DigestURI.hosthash6(domain));
                prop.put("table_domains_" + cnt + "_count", ResultURLs.domainCount(tabletype, domain));
                dark = !dark;
                cnt++;
            }
            prop.put("table_domains", cnt);
        }
        prop.put("process", tabletype.getCode());
        // return rewrite properties
        return prop;
    }

    private static SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
    private static String daydate(final Date date) {
        if (date == null) {
            return "";
        }
        return dayFormatter.format(date);
    }
}
