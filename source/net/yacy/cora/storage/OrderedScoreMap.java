/**
 *  ScoreMap
 *  Copyright 2010 by Michael Peter Christen, mc@yacy.net, Frankfurt am Main, Germany
 *  First released 14.10.2010 at http://yacy.net
 *
 *  $LastChangedDate$
 *  $LastChangedRevision$
 *  $LastChangedBy$
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.cora.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;


public class OrderedScoreMap<E> extends AbstractScoreMap<E> implements ScoreMap<E> {
    
    protected final Map<E, AtomicInteger> map; // a mapping from a reference to the cluster key
    
    public OrderedScoreMap(Comparator<? super E> comparator)  {
        if (comparator == null) {
            map = new HashMap<E, AtomicInteger>();
        } else {
            map = new TreeMap<E, AtomicInteger>(comparator);
        }
    }

    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }
    
    public synchronized void clear() {
        map.clear();
    }
    
    /**
     * shrink the cluster to a demanded size
     * @param maxsize
     */
    public void shrinkToMaxSize(int maxsize) {
        if (this.map.size() <= maxsize) return;
        int minScore = getMinScore();
        while (this.map.size() > maxsize) {
            minScore++;
            shrinkToMinScore(minScore);
        }
    }
    
    /**
     * shrink the cluster in such a way that the smallest score is equal or greater than a given minScore
     * @param minScore
     */
    public void shrinkToMinScore(int minScore) {
        synchronized (map) {
            Iterator<Map.Entry<E, AtomicInteger>> i = this.map.entrySet().iterator();
            Map.Entry<E, AtomicInteger> entry;
            while (i.hasNext()) {
                entry = i.next();
                if (entry.getValue().intValue() < minScore) i.remove();
            }
        }
    }
    
    public int size() {
        synchronized (map) {
            return map.size();
        }
    }
    
    /**
     * return true if the size of the score map is smaller then the given size
     * @param size
     * @return
     */
    public boolean sizeSmaller(int size) {
        if (map.size() < size) return true;
        synchronized (map) {
            return map.size() < size;
        }
    }
    
    public boolean isEmpty() {
        if (map.isEmpty()) return true;
        synchronized (map) {
            return map.isEmpty();
        }
    }
    
    public void inc(final E obj) {
        if (obj == null) return;
        AtomicInteger score = this.map.get(obj);
        if (score != null) {
            score.incrementAndGet();
            return;
        }
        synchronized (map) {
            score = this.map.get(obj);
            if (score == null) {
                this.map.put(obj, new AtomicInteger(1));
                return;
            }
        }
        score.incrementAndGet();
    }
    
    public void dec(final E obj) {
        if (obj == null) return;
        AtomicInteger score;
        synchronized (map) {
            score = this.map.get(obj);
            if (score == null) {
                this.map.put(obj, new AtomicInteger(-1));
                return;
            }
        }
        score.decrementAndGet();
    }
    
    public void set(final E obj, final int newScore) {
        if (obj == null) return;
        AtomicInteger score;
        synchronized (map) {
            score = this.map.get(obj);
            if (score == null) {
                this.map.put(obj, new AtomicInteger(newScore));
                return;
            }
        }       
        score.getAndSet(newScore);
    }
    
    public void inc(final E obj, final int incrementScore) {
        if (obj == null) return;
        AtomicInteger score;
        synchronized (map) {
            score = this.map.get(obj);
            if (score == null) {
                this.map.put(obj, new AtomicInteger(incrementScore));
            }
        }
        score.addAndGet(incrementScore);
    }
    
    public void dec(final E obj, final int incrementScore) {
        inc(obj, -incrementScore);
    }
    
    public int delete(final E obj) {
        // deletes entry and returns previous score
        if (obj == null) return 0;
        final AtomicInteger score;
        synchronized (map) {
            score = map.remove(obj);
            if (score == null) return 0;
        }
        return score.intValue();
    }

    public boolean containsKey(final E obj) {
        synchronized (map) {
            return map.containsKey(obj);
        }
    }
    
    public int get(final E obj) {
        if (obj == null) return 0;
        final AtomicInteger score;
        synchronized (map) {
            score = map.get(obj);
        }
        if (score == null) return 0;
        return score.intValue();
    }
    
    public SortedMap<E, AtomicInteger> tailMap(E obj) {
        if (this.map instanceof TreeMap) {
            return ((TreeMap<E, AtomicInteger>) this.map).tailMap(obj);
        }
        throw new UnsupportedOperationException("map must have comparator");
    }
    
    private int getMinScore() {
        if (map.isEmpty()) return -1;
        int minScore = Integer.MAX_VALUE;
        synchronized (map) {
            for (Map.Entry<E, AtomicInteger> entry: this.map.entrySet()) if (entry.getValue().intValue() < minScore) {
                minScore = entry.getValue().intValue();
            }
        }
        return minScore;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    public Iterator<E> keys(boolean up) {
        synchronized (map) {
            // re-organize entries
            TreeMap<Integer, Set<E>> m = new TreeMap<Integer, Set<E>>();
            Set<E> s;
            for (Map.Entry<E, AtomicInteger> entry: this.map.entrySet()) {
                s = m.get(entry.getValue().intValue());
                if (s == null) {
                    s = this.map instanceof TreeMap ? new TreeSet<E>(((TreeMap<E, AtomicInteger>) this.map).comparator()) : new HashSet<E>();
                    s.add(entry.getKey());
                    m.put(entry.getValue().intValue(), s);
                } else {
                    s.add(entry.getKey());
                }
            }
            
            // flatten result
            List<E> l = new ArrayList<E>(this.map.size());
            for (Set<E> f: m.values()) {
                for (E e: f) l.add(e);
            }
            if (up) return l.iterator();
            
            // optionally reverse list
            List<E> r = new ArrayList<E>(l.size());
            for (int i = l.size() - 1; i >= 0; i--) r.add(l.get(i));
            return r.iterator();
        }
    }
    
}
