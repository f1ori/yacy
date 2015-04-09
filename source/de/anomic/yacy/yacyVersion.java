package de.anomic.yacy;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.yacy.kelondro.logging.Log;

import de.anomic.search.Switchboard;

public class yacyVersion implements Comparator<yacyVersion>, Comparable<yacyVersion> {

    public static final float YACY_SUPPORTS_PORT_FORWARDING = (float) 0.383;
    public static final float YACY_SUPPORTS_GZIP_POST_REQUESTS_CHUNKED = (float) 0.58204761;
    public static final float YACY_HANDLES_COLLECTION_INDEX = (float) 0.486;
    public static final float YACY_POVIDES_REMOTECRAWL_LISTS = (float) 0.550;
    public static final float YACY_STANDARDREL_IS_PRO = (float) 0.557;
    private static yacyVersion thisVersion = null;
    
    /**
     * information about latest release, retrieved by other peers release version,
     * this value is overwritten when a peer with later version appears*/
    public static double latestRelease = 0.1; // 

    private float releaseNr;
    private final String dateStamp;
    private int svn;
    private final boolean mainRelease;

    private final String name;

    /**
     *  parse a release file name
     *  <p>the have the following form:
     *  <ul>
     *  <li>yacy_dev_v${releaseVersion}_${DSTAMP}_${releaseNr}.tar.gz</li>
     *  <li>yacy_v${releaseVersion}_${DSTAMP}_${releaseNr}.tar.gz</li>
     *  </ul>
     *  i.e. yacy_v0.51_20070321_3501.tar.gz
     * @param release
     */
    public yacyVersion(String release, String host) {

        this.name = release;
        if (release == null || !(release.endsWith(".tar.gz") || release.endsWith(".tar"))) {
            throw new RuntimeException("release file name '" + release + "' is not valid, no tar.gz");
        }
        // cut off tail
        release = release.substring(0, release.length() - ((release.endsWith(".gz")) ? 7 : 4));
        if (release.startsWith("yacy_v")) {
            release = release.substring(6);
        } else {
            throw new RuntimeException("release file name '" + release + "' is not valid, wrong prefix");
        }
        // now all release names have the form
        // ${releaseVersion}_${DSTAMP}_${releaseNr}
        final String[] comp = release.split("_"); // should be 3 parts
        if (comp.length != 3) {
            throw new RuntimeException("release file name '" + release + "' is not valid, 3 information parts expected");
        }
        try {
            this.releaseNr = Float.parseFloat(comp[0]);
        } catch (final NumberFormatException e) {
            throw new RuntimeException("release file name '" + release + "' is not valid, '" + comp[0] + "' should be a float number");
        }
        this.mainRelease = ((int) (this.getReleaseNr() * 100)) % 10 == 0 || (host != null && host.endsWith("yacy.net"));
        //System.out.println("Release version " + this.releaseNr + " is " + ((this.mainRelease) ? "main" : "std"));
        this.dateStamp = comp[1];
        if (this.getDateStamp().length() != 8) {
            throw new RuntimeException("release file name '" + release + "' is not valid, '" + comp[1] + "' should be a 8-digit date string");
        }
        try {
            this.svn = Integer.parseInt(comp[2]);
        } catch (final NumberFormatException e) {
            throw new RuntimeException("release file name '" + release + "' is not valid, '" + comp[2] + "' should be a integer number");
        }
        // finished! we parsed a relase string
    }

    
    public static final yacyVersion thisVersion() {
        // construct a virtual release name for this release
        if (thisVersion == null) {
            final Switchboard sb = Switchboard.getSwitchboard();
            if (sb == null) return null;
            thisVersion = new yacyVersion(
                "yacy" +
                "_v" + yacyBuildProperties.getVersion() + "_" +
                yacyBuildProperties.getBuildDate() + "_" +
                yacyBuildProperties.getSVNRevision() + ".tar.gz", null);
        }
        return thisVersion;
    }

    /**
     * returns 0 if this object is equal to the obj, -1 if this is smaller
     * than obj and 1 if this is greater than obj
     */
    public int compareTo(final yacyVersion obj) {
        return compare(this, obj);
    }

    /**
     * compare-operator for two yacyVersion objects
     * must be implemented to make it possible to put this object into
     * a ordered structure, like TreeSet or TreeMap
     */
    public int compare(final yacyVersion v0, final yacyVersion v1) {
        return (Integer.valueOf(v0.getSvn())).compareTo(Integer.valueOf(v1.getSvn()));
    }

    public boolean equals(final Object obj) {
        if (obj instanceof yacyVersion) {
            final yacyVersion v = (yacyVersion) obj;
            return (this.getSvn() == v.getSvn()) && (this.getName().equals(v.getName()));
        }
        return false;
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Converts combined version-string to a pretty string, e.g. "0.435/01818" or "dev/01818" (development version) or "dev/00000" (in case of wrong input)
     *
     * @param ver Combined version string matching regular expression:  "\A(\d+\.\d{3})(\d{4}|\d{5})\z" <br>
     *  (i.e.: start of input, 1 or more digits in front of decimal point, decimal point followed by 3 digits as major version, 4 or 5 digits for SVN-Version, end of input) 
     * @return If the major version is &lt; 0.11  - major version is separated from SVN-version by '/', e.g. "0.435/01818" <br>
     *         If the major version is &gt;= 0.11 - major version is replaced by "dev" and separated SVN-version by '/', e.g."dev/01818" <br> 
     *         "dev/00000" - If the input does not matcht the regular expression above 
     */
    public static String combined2prettyVersion(final String ver) {
         return combined2prettyVersion(ver, "");
     }

    public static String combined2prettyVersion(final String ver, final String computerName) {
         final Matcher matcher = Pattern.compile("\\A(\\d+\\.\\d{1,3})(\\d{0,5})\\z").matcher(ver); 
    
         if (!matcher.find()) { 
             Log.logWarning("STARTUP", "Peer '"+computerName+"': wrong format of version-string: '" + ver + "'. Using default string 'dev/00000' instead");   
             return "dev/00000";
         }
         
         final String mainversion = (Float.parseFloat(matcher.group(1)) < 0.11 ? "dev" : matcher.group(1));
        String revision = matcher.group(2);
        for(int i=revision.length();i<5;++i) revision += "0";
        return mainversion+"/"+revision;
     }

    /**
     * Combines the version of YaCy with the versionnumber from SVN to a
     * combined version
     *
     * @param version Current given version.
     * @param svn Current version given from SVN.
     * @return String with the combined version.
     */
    public static double versvn2combinedVersion(final double version, final int svn) {
        return (Math.rint((version*100000000.0) + (svn))/100000000);
     }

    /**
     * Timestamp of this version
     * @return timestamp
     */
    public String getDateStamp() {
        return dateStamp;
    }

    /**
     * SVN revision of release
     * @return svn revision as integer
     */
    public int getSvn() {
        return svn;
    }

    /**
     * Whether this is a stable main release or not
     * @return 
     */
    public boolean isMainRelease() {
        return mainRelease;
    }

    /**
     * release number as float (e. g. 7.04)
     * @return
     */
    public float getReleaseNr() {
        return releaseNr;
    }


    public String getName() {
        return name;
    }

}
