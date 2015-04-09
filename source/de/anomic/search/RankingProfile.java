// RankingProfile.java 
// -------------------------------
// part of YACY
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2006
// Created: 05.02.2006
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

package de.anomic.search;

import java.util.HashMap;
import java.util.Map;

import net.yacy.kelondro.logging.Log;

public class RankingProfile {

    // pre-sort attributes
    public static final String DOMLENGTH          = "domlength";
    public static final String YBR                = "ybr";
    public static final String DATE               = "date";
    public static final String WORDSINTITLE       = "wordsintitle";
    public static final String WORDSINTEXT        = "wordsintext";
    public static final String PHRASESINTEXT      = "phrasesintext";
    public static final String LLOCAL             = "llocal";
    public static final String LOTHER             = "lother";
    public static final String URLLENGTH          = "urllength";
    public static final String URLCOMPS           = "urlcomps";
    public static final String HITCOUNT           = "hitcount";
    public static final String POSINTEXT          = "posintext";
    public static final String POSOFPHRASE        = "posofphrase";
    public static final String POSINPHRASE        = "posinphrase";
    public static final String AUTHORITY          = "authority";
    public static final String WORDDISTANCE       = "worddistance";
    public static final String APPURL             = "appurl";
    public static final String APP_DC_TITLE       = "appdescr";   // title of page
    public static final String APP_DC_CREATOR     = "appauthor";  // the author field
    public static final String APP_DC_SUBJECT     = "apptags";    // tags
    public static final String APP_DC_DESCRIPTION = "appref";     // references to the source (content of <a> tag)
    public static final String APPEMPH            = "appemph";
    public static final String CATINDEXOF         = "catindexof";
    public static final String CATHASIMAGE        = "cathasimage";
    public static final String CATHASAUDIO        = "cathasaudio";
    public static final String CATHASVIDEO        = "cathasvideo";
    public static final String CATHASAPP          = "cathasapp";
    public static final String TERMFREQUENCY      = "tf";
    public static final String LANGUAGE           = "language";   // ranking of preferred language

    // post-sort predicates
    public static final String URLCOMPINTOPLIST   = "urlcompintoplist";
    public static final String DESCRCOMPINTOPLIST = "descrcompintoplist";
    public static final String PREFER = "prefer";

    // coefficient max/min values
    public static final int COEFF_MIN =  0;
    public static final int COEFF_MAX = 15;
    
    public int
        coeff_domlength, coeff_ybr, coeff_date, coeff_wordsintitle, coeff_wordsintext, coeff_phrasesintext,
        coeff_llocal, coeff_lother, coeff_urllength, coeff_urlcomps, coeff_hitcount, 
        coeff_posintext, coeff_posofphrase, coeff_posinphrase, coeff_authority, coeff_worddistance,
        coeff_appurl, coeff_app_dc_title, coeff_app_dc_creator, coeff_app_dc_subject, coeff_app_dc_description, coeff_appemph,
        coeff_catindexof, coeff_cathasimage, coeff_cathasaudio, coeff_cathasvideo, coeff_cathasapp,
        coeff_urlcompintoplist, coeff_descrcompintoplist, coeff_prefer,
        coeff_termfrequency, coeff_language;
    
    public RankingProfile(final ContentDomain mediatype) {
        // set default-values
        coeff_domlength          = 11;
        coeff_ybr                = 9;
        coeff_date               = 4;
        coeff_wordsintitle       = 4;
        coeff_wordsintext        = 2;
        coeff_phrasesintext      = 3;
        coeff_llocal             = 2;
        coeff_lother             = 3;
        coeff_urllength          = 13;
        coeff_urlcomps           = 12;
        coeff_hitcount           = 9;
        coeff_posintext          = 10;
        coeff_posofphrase        = 8;
        coeff_posinphrase        = 1;
        coeff_authority          = 11;
        coeff_worddistance       = 12;
        coeff_appurl             = 14;
        coeff_app_dc_title       = 14;
        coeff_app_dc_creator     = 12;
        coeff_app_dc_subject     = 13;
        coeff_app_dc_description = 13;
        coeff_appemph            = 10;
        coeff_catindexof         = (mediatype == ContentDomain.TEXT) ? 0 : 15;
        coeff_cathasimage        = (mediatype == ContentDomain.IMAGE) ? 15 : 0;
        coeff_cathasaudio        = (mediatype == ContentDomain.AUDIO) ? 15 : 0;
        coeff_cathasvideo        = (mediatype == ContentDomain.VIDEO) ? 15 : 0;
        coeff_cathasapp          = (mediatype == ContentDomain.APP) ? 15 : 0;
        coeff_termfrequency      = 14;
        coeff_urlcompintoplist   = 3;
        coeff_descrcompintoplist = 2;
        coeff_prefer             = 14;
        coeff_language           = 13;
    }
    
    public RankingProfile(final String prefix, String profile) {
        this(ContentDomain.TEXT); // set defaults
        if ((profile != null) && (profile.length() > 0)) {
            //parse external form
            final Map<String, Integer> coeff = new HashMap<String, Integer>(40);
            final String[] elts;
            if (profile.length() > 0 && profile.charAt(0) == '{' && profile.endsWith("}")) {
                profile = profile.substring(1, profile.length() - 1);
            }
            profile = profile.trim();
            if (profile.indexOf('&') > 0) elts = profile.split("&"); else elts = profile.split(","); 
            int p;
            final int s = (prefix == null) ? 0 : prefix.length();
            String e;
            
            for (int i = 0; i < elts.length; i++) {
                e = elts[i].trim();
                if ((s == 0) || (e.startsWith(prefix))) {
                    p = e.indexOf('=');
                    if (p < 0) System.out.println("DEBUG: bug in plasmaSearchRankingProfile: e = " + e);
                    if ((p > 0) && (e.length() > p + 1)) try {
                        coeff.put(e.substring(s, p), Integer.valueOf(Integer.parseInt(e.substring(p + 1))));
                    } catch (NumberFormatException e1) {
                        System.out.println("wrong parameter: " + e.substring(s, p) + "=" + e.substring(p + 1));
                        Log.logException(e1);
                    }
                }
            }
            coeff_domlength          = parseMap(coeff, DOMLENGTH, coeff_domlength);
            coeff_ybr                = parseMap(coeff, YBR, coeff_ybr);
            coeff_date               = parseMap(coeff, DATE, coeff_date);
            coeff_wordsintitle       = parseMap(coeff, WORDSINTITLE, coeff_wordsintitle);
            coeff_wordsintext        = parseMap(coeff, WORDSINTEXT, coeff_wordsintext);
            coeff_phrasesintext      = parseMap(coeff, PHRASESINTEXT, coeff_phrasesintext);
            coeff_llocal             = parseMap(coeff, LLOCAL, coeff_llocal);
            coeff_lother             = parseMap(coeff, LOTHER, coeff_lother);
            coeff_urllength          = parseMap(coeff, URLLENGTH, coeff_urllength);
            coeff_urlcomps           = parseMap(coeff, URLCOMPS, coeff_urlcomps);
            coeff_hitcount           = parseMap(coeff, HITCOUNT, coeff_hitcount);
            coeff_posintext          = parseMap(coeff, POSINTEXT, coeff_posintext);
            coeff_posofphrase        = parseMap(coeff, POSOFPHRASE, coeff_posofphrase);
            coeff_posinphrase        = parseMap(coeff, POSINPHRASE, coeff_posinphrase);
            coeff_authority          = parseMap(coeff, AUTHORITY, coeff_authority);
            coeff_worddistance       = parseMap(coeff, WORDDISTANCE, coeff_worddistance);
            coeff_appurl             = parseMap(coeff, APPURL, coeff_appurl);
            coeff_app_dc_title       = parseMap(coeff, APP_DC_TITLE, coeff_app_dc_title);
            coeff_app_dc_creator     = parseMap(coeff, APP_DC_CREATOR, coeff_app_dc_creator);
            coeff_app_dc_subject     = parseMap(coeff, APP_DC_SUBJECT, coeff_app_dc_subject);
            coeff_app_dc_description = parseMap(coeff, APP_DC_DESCRIPTION, coeff_app_dc_description);
            coeff_appemph            = parseMap(coeff, APPEMPH, coeff_appemph);
            coeff_catindexof         = parseMap(coeff, CATINDEXOF, coeff_catindexof);
            coeff_cathasimage        = parseMap(coeff, CATHASIMAGE, coeff_cathasimage);
            coeff_cathasaudio        = parseMap(coeff, CATHASAUDIO, coeff_cathasaudio);
            coeff_cathasvideo        = parseMap(coeff, CATHASVIDEO, coeff_cathasvideo);
            coeff_cathasapp          = parseMap(coeff, CATHASAPP, coeff_cathasapp);
            coeff_termfrequency      = parseMap(coeff, TERMFREQUENCY, coeff_termfrequency);
            coeff_urlcompintoplist   = parseMap(coeff, URLCOMPINTOPLIST, coeff_urlcompintoplist);
            coeff_descrcompintoplist = parseMap(coeff, DESCRCOMPINTOPLIST, coeff_descrcompintoplist);
            coeff_prefer             = parseMap(coeff, PREFER, coeff_prefer);
            coeff_language           = parseMap(coeff, LANGUAGE, coeff_language);
        }
    }
    
    private static int parseMap(final Map<String, Integer> coeff, final String attr, final int dflt) {
        if (!coeff.containsKey(attr))
            return dflt;
        return (coeff.get(attr)).intValue();
    }

    public String toExternalString() {
        return toExternalMap("").toString();
    }
    
    public Map<String, String> toExternalMap(final String prefix) {
    	final Map<String, String> ext = preToExternalMap(prefix);
    	ext.putAll(postToExternalMap(prefix));
    	return ext;
    }
    
    public Map<String, String> preToExternalMap(final String prefix) {
        final Map<String, String> ext = new HashMap<String, String>(40);
        ext.put(prefix + DOMLENGTH, Integer.toString(coeff_domlength));
        ext.put(prefix + YBR, Integer.toString(coeff_ybr));
        ext.put(prefix + DATE, Integer.toString(coeff_date));
        ext.put(prefix + WORDSINTITLE, Integer.toString(coeff_wordsintitle));
        ext.put(prefix + WORDSINTEXT, Integer.toString(coeff_wordsintext));
        ext.put(prefix + PHRASESINTEXT, Integer.toString(coeff_phrasesintext));
        ext.put(prefix + LLOCAL, Integer.toString(coeff_llocal));
        ext.put(prefix + LOTHER, Integer.toString(coeff_lother));
        ext.put(prefix + URLLENGTH, Integer.toString(coeff_urllength));
        ext.put(prefix + URLCOMPS, Integer.toString(coeff_urlcomps));
        ext.put(prefix + HITCOUNT, Integer.toString(coeff_hitcount));
        ext.put(prefix + POSINTEXT, Integer.toString(coeff_posintext));
        ext.put(prefix + POSOFPHRASE, Integer.toString(coeff_posofphrase));
        ext.put(prefix + POSINPHRASE, Integer.toString(coeff_posinphrase));
        ext.put(prefix + AUTHORITY, Integer.toString(coeff_authority));
        ext.put(prefix + WORDDISTANCE, Integer.toString(coeff_worddistance));
        ext.put(prefix + APPURL, Integer.toString(coeff_appurl));
        ext.put(prefix + APP_DC_TITLE, Integer.toString(coeff_app_dc_title));
        ext.put(prefix + APP_DC_CREATOR, Integer.toString(coeff_app_dc_creator));
        ext.put(prefix + APP_DC_SUBJECT, Integer.toString(coeff_app_dc_subject));
        ext.put(prefix + APP_DC_DESCRIPTION, Integer.toString(coeff_app_dc_description));
        ext.put(prefix + APPEMPH, Integer.toString(coeff_appemph));
        ext.put(prefix + CATINDEXOF, Integer.toString(coeff_catindexof));
        ext.put(prefix + CATHASIMAGE, Integer.toString(coeff_cathasimage));
        ext.put(prefix + CATHASAUDIO, Integer.toString(coeff_cathasaudio));
        ext.put(prefix + CATHASVIDEO, Integer.toString(coeff_cathasvideo));
        ext.put(prefix + CATHASAPP, Integer.toString(coeff_cathasapp));
        ext.put(prefix + TERMFREQUENCY, Integer.toString(coeff_termfrequency));
        ext.put(prefix + LANGUAGE, Integer.toString(coeff_language));
        return ext;
    }
    
    public Map<String, String> postToExternalMap(final String prefix) {
    	final Map<String, String> ext = new HashMap<String, String>();
        ext.put(prefix + URLCOMPINTOPLIST, Integer.toString(coeff_urlcompintoplist));
        ext.put(prefix + DESCRCOMPINTOPLIST, Integer.toString(coeff_descrcompintoplist));
        ext.put(prefix + PREFER, Integer.toString(coeff_prefer));
        return ext;
    }
    
    public String toExternalURLGet(final String prefix) {
    	final Map<String, String> emap = toExternalMap("");
        final StringBuilder ext = new StringBuilder(emap.size() * 40);
        for (Map.Entry<String, String> entry: emap.entrySet()) {
            ext.append('&');
            ext.append(prefix);
            ext.append(entry.getKey());
            ext.append('=');
            ext.append(entry.getValue());
        }
        return ext.toString();
    }
    
}
