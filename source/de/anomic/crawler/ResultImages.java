// ResultImages.java
// (C) 2008 by by Detlef Reichl; detlef!reichl()gmx!org and Michael Peter Christen; mc@yacy.net
// first published 13.04.2008 on http://yacy.net
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

package de.anomic.crawler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import net.yacy.cora.document.MultiProtocolURI;
import net.yacy.document.Document;
import net.yacy.document.parser.html.ImageEntry;
import net.yacy.kelondro.data.meta.DigestURI;
import net.yacy.kelondro.util.MemoryControl;


public class ResultImages {

    // we maintain two different queues for private and public crawls and divide both into two halves:
    // such images that appear to be good quality for a image monitor bacause their size is known, and other images
    // that are not declared with sizes.
    private static final ConcurrentLinkedQueue<OriginEntry> privateImageQueueHigh = new ConcurrentLinkedQueue<OriginEntry>();
    private static final ConcurrentLinkedQueue<OriginEntry> privateImageQueueLow = new ConcurrentLinkedQueue<OriginEntry>();
    private static final ConcurrentLinkedQueue<OriginEntry> publicImageQueueHigh = new ConcurrentLinkedQueue<OriginEntry>();
    private static final ConcurrentLinkedQueue<OriginEntry> publicImageQueueLow = new ConcurrentLinkedQueue<OriginEntry>();

    // we also check all links for a double-check so we don't get the same image more than once in any queue
    // image links may appear double here even if the pages where the image links are embedded already are checked for double-occurrence:
    // the same images may be linked from different pages
    private static final ConcurrentMap<MultiProtocolURI, Long> doubleCheck = new ConcurrentHashMap<MultiProtocolURI, Long>(); // (url, time) when the url appeared first
    
    public static void registerImages(final DigestURI source, final Document document, final boolean privateEntry) {
        if (document == null) return;
        if (source == null) return;

        if (MemoryControl.shortStatus()) clearQueues();
        limitQueues(1000);
        
        final Map<MultiProtocolURI, ImageEntry> images = document.getImages();
        for (final ImageEntry image: images.values()) {
            // do a double-check; attention: this can be time-consuming since this possibly needs a DNS-lookup
            if (image == null || image.url() == null) continue;
            if (doubleCheck.containsKey(image.url())) continue;
            doubleCheck.put(image.url(), System.currentTimeMillis());
            
            final String name = image.url().getFile();
            boolean good = false;
            if (image.width() > 120 &&
                image.height() > 100 &&
                image.width() < 1200 &&
                image.height() < 1000 &&
                name.lastIndexOf(".gif") == -1) {
                // && ((urlString.lastIndexOf(".jpg") != -1)) ||
                // ((urlString.lastIndexOf(".png") != -1)){
                
                good = true;
                float ratio;
                if (image.width() > image.height()) {
                    ratio = (float) image.width() / (float) image.height();
                } else {
                    ratio = (float) image.height() / (float) image.width();
                }
                good = !(ratio < 1.0f || ratio > 2.0f);
            }
            if (good) {
                if (privateEntry) {
                    privateImageQueueHigh.add(new OriginEntry(image, source));
                } else {
                    publicImageQueueHigh.add(new OriginEntry(image, source));
                }
            } else {
                if (privateEntry) {
                    privateImageQueueLow.add(new OriginEntry(image, source));
                } else {
                    publicImageQueueLow.add(new OriginEntry(image, source));
                }
            }
        }
    }
    
    public static OriginEntry next(final boolean privateEntryOnly) {
        OriginEntry e = null;
        if (privateEntryOnly) {
            e = privateImageQueueHigh.poll();
            if (e == null) e = privateImageQueueLow.poll();
        } else {
            e = publicImageQueueHigh.poll();
            if (e == null) e = privateImageQueueHigh.poll();
            if (e == null) e = publicImageQueueLow.poll();
            if (e == null) e = privateImageQueueLow.poll();
        }
        return e;
    }
    
    public static int queueSize(final boolean privateEntryOnly) {
        int publicSize = 0;
        if (!privateEntryOnly) {
            publicSize = publicImageQueueHigh.size() + publicImageQueueLow.size();
        }
        return privateImageQueueHigh.size() + privateImageQueueLow.size() + publicSize;
    }
    
    public static int privateQueueHighSize() {
        return privateImageQueueHigh.size();
    }
    
    public static int privateQueueLowSize() {
        return privateImageQueueLow.size();
    }
    
    public static int publicQueueHighSize() {
        return publicImageQueueHigh.size();
    }
    
    public static int publicQueueLowSize() {
        return publicImageQueueLow.size();
    }

    public static void clearQueues() {
        privateImageQueueHigh.clear();
        privateImageQueueLow.clear();
        publicImageQueueHigh.clear();
        publicImageQueueLow.clear();
        doubleCheck.clear();
    }
    
    public static void limitQueues(int limit) {
        while (privateImageQueueHigh.size() > limit) privateImageQueueHigh.poll();
        while (privateImageQueueLow.size() > limit) privateImageQueueLow.poll();
        while (publicImageQueueHigh.size() > limit) publicImageQueueHigh.poll();
        while (publicImageQueueLow.size() > limit) publicImageQueueLow.poll();
    }
    
    public static class OriginEntry {
        public ImageEntry imageEntry;
        public MultiProtocolURI baseURL;
        public OriginEntry(final ImageEntry imageEntry, final MultiProtocolURI baseURL) {
            this.imageEntry = imageEntry;
            this.baseURL = baseURL;
        }
    }
    
}
