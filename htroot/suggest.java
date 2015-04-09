// suggest.java
// -----------------------
// (C) 2010 by Michael Peter Christen; mc@yacy.net
// first published 11.10.2010 in Frankfurt, Germany on http://yacy.net
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


import java.util.Iterator;

import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.protocol.ResponseHeader;
import net.yacy.kelondro.data.word.Word;

import de.anomic.data.DidYouMean;
import de.anomic.search.Segment;
import de.anomic.search.Segments;
import de.anomic.search.Switchboard;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.server.servletProperties;

/**
 * for json format:
 * implementation of the opensearch suggestion extension, see
 * http://www.opensearch.org/Specifications/OpenSearch/Extensions/Suggestions/1.1
 * or
 * https://wiki.mozilla.org/Search_Service/Suggestions
 * 
 * for xml format:
 * see Microsoft Search Suggestion Format
 * http://msdn.microsoft.com/en-us/library/cc848863%28VS.85%29.aspx
 * and
 * http://msdn.microsoft.com/en-us/library/cc848862%28v=VS.85%29.aspx
 */
public class suggest {
    
    private static final int meanMax = 30;
    
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        final Switchboard sb = (Switchboard) env;
        final servletProperties prop = new servletProperties();

        final String ext = header.get("EXT", "");
        final boolean json = ext.equals("json");
        final boolean xml = ext.equals("xml");
        final boolean more = post != null && post.containsKey("more");
        
        // get query
        final String originalquerystring = (post == null) ? "" : post.get("query", post.get("q", "")).trim();
        final String querystring =  originalquerystring.replace('+', ' ');
        final int timeout = (post == null) ? 300 : post.getInt("timeout", 300);
        final int count = (post == null) ? 20 : post.getInt("count", 20);
        
        // get segment
        final Segment indexSegment;
        if (post != null && post.containsKey("segment")) {
            String segmentName = post.get("segment");
            if (sb.indexSegments.segmentExist(segmentName)) {
                indexSegment = sb.indexSegments.segment(segmentName);
            } else {
                // take default segment
                indexSegment = sb.indexSegments.segment(Segments.Process.PUBLIC);
            }
        } else {
            // take default segment
            indexSegment = sb.indexSegments.segment(Segments.Process.PUBLIC);
        }
        
        int c = 0;
        if (more ||
                (indexSegment != null &&
                !indexSegment.termIndex().has(Word.word2hash(querystring))))
        {
            final DidYouMean didYouMean = new DidYouMean(indexSegment.termIndex(), querystring);
            final Iterator<String> meanIt = didYouMean.getSuggestions(timeout, count).iterator();
            String suggestion;
            //[#[query]#,[#{suggestions}##[text]##(eol)#,::#(/eol)##{/suggestions}#]]
            while (c < meanMax && meanIt.hasNext()) {
                suggestion = meanIt.next();
                if (json) {
                    prop.putJSON("suggestions_" + c + "_text", suggestion);
                } else if (xml) {
                    prop.putXML("suggestions_" + c + "_text", suggestion);
                } else {
                    prop.putHTML("suggestions_" + c + "_text", suggestion);
                }
                prop.put("suggestions_" + c + "_eol", 0);
                c++;
            }
        }
        
        if (c > 0) {
            prop.put("suggestions_" + (c - 1) + "_eol", 1);
        }
        prop.put("suggestions", c);
        if (json) {
            prop.putJSON("query", originalquerystring);
        } else if (xml) {
            prop.putXML("query", originalquerystring);
        } else {
            prop.putHTML("query", originalquerystring);
        }

        // Adding CORS Access header for xml output
        if (xml) {
            final ResponseHeader outgoingHeader = new ResponseHeader();
            outgoingHeader.put(HeaderFramework.CORS_ALLOW_ORIGIN, "*");
            prop.setOutgoingHeader(outgoingHeader);
        }
        
        // return rewrite properties
        return prop;
    }
    
}
