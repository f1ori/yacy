package net.yacy.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.yacy.cora.storage.HashARC;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.logging.Log;

/**
 * a URL filter engine for black and white lists
 * 
 * @TODO precompile regular expressions
 *
 */
public class FilterEngine {
	
	/** size of URL cache */
	protected static final int CACHE_SIZE = 100;
	
    public static final int ERR_TWO_WILDCARDS_IN_HOST = 1;
    public static final int ERR_SUBDOMAIN_XOR_WILDCARD = 2;
    public static final int ERR_PATH_REGEX = 3;
    public static final int ERR_WILDCARD_BEGIN_OR_END = 4;
    public static final int ERR_HOST_WRONG_CHARS = 5;
    public static final int ERR_DOUBLE_OCCURANCE = 6;
    public static final int ERR_HOST_REGEX = 7;

    protected enum listTypes { type1 };

    protected class FilterEntry implements Comparable<FilterEntry> {
        public String path;
        public EnumSet<listTypes> types;

        public FilterEntry(String path, EnumSet<listTypes>types) {
            this.path = path;
            this.types = types;
        }

        public int compareTo(FilterEntry fe) {
            return this.path.compareToIgnoreCase(fe.path);
        }
    }
	
    protected HashARC<DigestURI, EnumSet<listTypes>> cachedUrlHashs = null;
    protected Map<String, Set<FilterEntry>> hostpaths_matchable = null;
    protected Map<String, Set<FilterEntry>> hostpaths_notmatchable = null;

    
    public FilterEngine() {
        // prepare the data structure
        this.hostpaths_matchable = new HashMap<String, Set<FilterEntry>>();
        this.hostpaths_notmatchable = new HashMap<String, Set<FilterEntry>>();
        this.cachedUrlHashs = new HashARC<DigestURI, EnumSet<listTypes>>(CACHE_SIZE);
    }
    
    public void clear() {
    	this.cachedUrlHashs.clear();
    	this.hostpaths_matchable.clear();
    	this.hostpaths_notmatchable.clear();
    }

    public int size() {
    	return this.hostpaths_matchable.size() + this.hostpaths_notmatchable.size();
    }
    
    public void add(String entry, EnumSet<listTypes> types) {
    	assert entry != null;
    	int pos; // position between domain and path
    	if((pos = entry.indexOf('/')) > 0) {
    		String host = entry.substring(0, pos).trim().toLowerCase();
            String path = entry.substring(pos + 1).trim();

            // avoid PatternSyntaxException e
            if (!isMatchable(host) && host.length() > 0 && host.charAt(0) == '*')
            	host = "." + host;
            
            if(isMatchable(host)) {
            	if (!hostpaths_matchable.containsKey(host))
            		hostpaths_matchable.put(host, new TreeSet<FilterEntry>());
            	hostpaths_matchable.get(host).add(new FilterEntry(path, types));
            	// TODO: update type, if there is an element
            } else {
            	if (!hostpaths_notmatchable.containsKey(host))
            		hostpaths_notmatchable.put(host, new TreeSet<FilterEntry>());
            	hostpaths_notmatchable.get(host).add(new FilterEntry(path, types));            	
            }
    	}
    }
    
    public void loadList(final BufferedReader in, EnumSet<listTypes> types) throws IOException {
    	String line;
    	while((line = in.readLine()) != null) {
    		line = line.trim();
        	if (line.length() > 0 && line.charAt(0) != '#')
        		this.add(line, types);
    	}
    }
    
    public void removeAll(final String host) {
    	assert host != null;
    	this.hostpaths_matchable.remove(host);
    	this.hostpaths_notmatchable.remove(host);
    }
    
    public void remove(final String listType, final String host, final String path) {
    }
    
    public boolean isListed(final DigestURI url, final EnumSet<listTypes> type) {
    	// trival anwser
    	if (url.getHost() == null)
    		return false;
    	
    	if(cachedUrlHashs.containsKey(url)) {
    		// Cache Hit
    		EnumSet<listTypes> e = cachedUrlHashs.get(url);
    		return e.containsAll(type);
    	} else {
    		// Cache Miss
    		return isListed(url.getHost().toLowerCase(), url.getFile(), type);
    	}
    }
    
    public static boolean isMatchable (final String host) {
        try {
            if(Pattern.matches("^[a-z0-9.-]*$", host)) // simple Domain (yacy.net or www.yacy.net)
                return true;
            if(Pattern.matches("^\\*\\.[a-z0-9-.]*$", host)) // start with *. (not .* and * must follow a dot)
                return true;
            if(Pattern.matches("^[a-z0-9-.]*\\.\\*$", host)) // ends with .* (not *. and befor * must be a dot)
                return true;
        } catch (final PatternSyntaxException e) {
            //System.out.println(e.toString());
            return false;
        }
       return false;
    }

    public boolean isListed(final String host, String path, EnumSet<listTypes> type) {
        if (host == null) throw new NullPointerException();
        if (path == null) throw new NullPointerException();

        if (path.length() > 0 && path.charAt(0) == '/') path = path.substring(1);
        Set<FilterEntry> app;

        // try to match complete domain
        if ((app = hostpaths_matchable.get(host)) != null) {
        	for(FilterEntry e: app) {
        		if (e.path.indexOf("?*") > 0) {
                    // prevent "Dangling meta character '*'" exception
                    Log.logWarning("FilterEngine", "ignored blacklist path to prevent 'Dangling meta character' exception: " + e);
                    continue;
                }
                if((e.path.equals("*")) || (path.matches(e.path)))
                	return true;
        	}
        }
        // first try to match the domain with wildcard '*'
        // [TL] While "." are found within the string
        int index = 0;
        while ((index = host.indexOf('.', index + 1)) != -1) {
            if ((app = hostpaths_matchable.get(host.substring(0, index + 1) + "*")) != null) {
            	for(FilterEntry e: app) {
            		if((e.path.equals("*")) || (path.matches(e.path)))
            			return true;
            	}
            }
            if ((app = hostpaths_matchable.get(host.substring(0, index))) != null) {
            	for(FilterEntry e: app) {
            		if((e.path.equals("*")) || (path.matches(e.path)))
            			return true;
            	}
            }
        }
        index = host.length();
        while ((index = host.lastIndexOf('.', index - 1)) != -1) {
            if ((app = hostpaths_matchable.get("*" + host.substring(index, host.length()))) != null) {
            	for(FilterEntry e: app) {
                    if((e.path.equals("*")) || (path.matches(e.path)))
                    	return true;
            		
            	}
            }
            if ((app = hostpaths_matchable.get(host.substring(index +1, host.length()))) != null) {
            	for(FilterEntry e: app) {
                    if((e.path.equals("*")) || (path.matches(e.path)))
                    	return true;
            	}
            }
        }


        // loop over all Regexentrys
        for(final Entry<String, Set<FilterEntry>> entry: hostpaths_notmatchable.entrySet()) {
            try {
                if(Pattern.matches(entry.getKey(), host)) {
                    app = entry.getValue();
                    for(FilterEntry e: app) {
                        if(Pattern.matches(e.path, path)) 
                            return true;
                    }
                }
            } catch (final PatternSyntaxException e) {
                System.out.println(e.toString());
            }
        }
        return false;
    }

    public int checkError(String element, Map<String, String> properties) {

        final boolean allowRegex = (properties != null) && properties.get("allowRegex").equalsIgnoreCase("true");
        int slashPos;
        String host, path;

        if ((slashPos = element.indexOf('/')) == -1) {
            host = element;
            path = ".*";
        } else {
            host = element.substring(0, slashPos);
            path = element.substring(slashPos + 1);
        }

        if (!allowRegex || !isValidRegex(host)) {
            final int i = host.indexOf('*');

            // check whether host begins illegally
            if (!host.matches("([A-Za-z0-9_-]+|\\*)(\\.([A-Za-z0-9_-]+|\\*))*")) {
                if (i == 0 && host.length() > 1 && host.charAt(1) != '.') {
                    return ERR_SUBDOMAIN_XOR_WILDCARD;
                }
                return ERR_HOST_WRONG_CHARS;
            }

            // in host-part only full sub-domains may be wildcards
            if (host.length() > 0 && i > -1) {
                if (!(i == 0 || i == host.length() - 1)) {
                    return  ERR_WILDCARD_BEGIN_OR_END;
                }

                if (i == host.length() - 1 && host.length() > 1 && host.charAt(i - 1) != '.') {
                    return ERR_SUBDOMAIN_XOR_WILDCARD;
                }
            }

            // check for double-occurences of "*" in host
            if (host.indexOf("*", i + 1) > -1) {
                return ERR_TWO_WILDCARDS_IN_HOST;
            }
        } else if (allowRegex && !isValidRegex(host)) {
            return ERR_HOST_REGEX;
        }

        // check for errors on regex-compiling path
        if (!isValidRegex(path) && !path.equals("*")) {
            return ERR_PATH_REGEX;
        }

        return 0;
    }

    /**
     * Checks if a given expression is a valid regular expression.
     * @param expression The expression to be checked.
     * @return True if the expression is a valid regular expression, else false.
     */
    private static boolean isValidRegex(String expression) {
        boolean ret = true;
        try {
            Pattern.compile(expression);
        } catch (final PatternSyntaxException e) {

            ret = false;
        }
        return ret;
    }
    

}
