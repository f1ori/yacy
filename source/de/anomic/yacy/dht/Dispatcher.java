// Dispatcher.java 
// ------------------------------
// part of YaCy
// (C) 2009 by Michael Peter Christen; mc@yacy.net
// first published on http://yacy.net
// Frankfurt, Germany, 28.01.2009
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

package de.anomic.yacy.dht;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.yacy.cora.document.UTF8;
import net.yacy.kelondro.data.meta.URIMetadataRow;
import net.yacy.kelondro.data.word.WordReference;
import net.yacy.kelondro.index.HandleSet;
import net.yacy.kelondro.index.RowSpaceExceededException;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.order.Base64Order;
import net.yacy.kelondro.rwi.ReferenceContainer;
import net.yacy.kelondro.util.ByteArray;
import net.yacy.kelondro.workflow.WorkflowProcessor;

import de.anomic.search.Segment;
import de.anomic.yacy.yacySeed;
import de.anomic.yacy.yacySeedDB;
import java.util.List;

public class Dispatcher {

    /**
     * the dispatcher class accumulates indexContainerCache objects before they are transfered
     * to other peers. A dispatcher holds several of such caches to enhance the transmission process.
     * Before a RWI is sent, the following process is applied:
     * - (1) a number of RWIs are selected and accumulated.
     *       When they are selected, they are removed from the index
     * - (2) the RWI collection is split into a number of partitions according to the vertical DHT.
     * - (3) the split RWIs are enqueued as Entry object in the entry 'cloud' of the dispatcher
     * - (4) more entries may be enqueued to the dispatcher and entries with the same primary target
     *       are accumulated.
     * - (5) the largest entries are selected from the dispatcher cloud and enqueued to the 'next' array
     *       which means that they are ready for transmission
     * - (6) the dispatcher takes some of the entries in the next queue and initiates
     *       transmission to other peers concurrently. As much transmissions are initiated concurrently
     *       as the redundancy factor.
     * - (7) a transmission thread executes the entry transmission.
     * - (8) the transmission thread initiates another transmission in case that it fails
     * - (9) when the wanted number of redundant peers have received the entries,
     *       they are removed from the next queue
     * Concurrency in this process:
     * 1-3 follow directly and should be synchronous because of the database operation that are used
     * 4   is a repeated action of 1-3 and should be done in a busyThread
     * 5&6 is a repeated action as (4), but must be executed multiple times of (4) in a busyThread,
     *     which idle is shorter than the idle time of (4)
     * 7&8 this is done concurrently with other transmission threads for the same entry and other entries
     *     for example, if the redundancy factor is 3 and 2 entries are in the 'next' queue, then 6
     *     transmissions are running concurrently
     * 9   concurrency ends for the transmission, if the wanted number of redundant peers received the entry,
     *     or the target queue runs out of entries. If the target queue is empty, the transmission is
     *     called failed. In case of a fail, the RWI fragment is put back into the backend index structure
     */
    
    // a cloud is a cache for the objects that wait to be transmitted
    // the String-key is the primary target as contained in the Entry
    private Map<ByteArray, Transmission.Chunk> transmissionCloud;
    
    // the segment backend is used to store the remaining indexContainers in case that the object is closed
    private final Segment segment;
    
    // the seed database
    private final yacySeedDB seeds;
    
    // the log
    private final Log log;
    
    // transmission process
    private WorkflowProcessor<Transmission.Chunk> indexingTransmissionProcessor;
   
    // transmission object
    private final Transmission transmission;
    
    public Dispatcher(
            final Segment segment,
            final yacySeedDB seeds,
            final boolean gzipBody, 
            final int timeout
            ) {
        this.transmissionCloud = new ConcurrentHashMap<ByteArray, Transmission.Chunk>();
        this.segment = segment;
        this.seeds = seeds;
        this.log = new Log("INDEX-TRANSFER-DISPATCHER");
        this.transmission = new Transmission(
            log,
            segment,
            seeds,
            gzipBody,
            timeout);
        
        int concurrentSender = Math.min(25, Math.max(10, WorkflowProcessor.useCPU * 2 + 1));
        indexingTransmissionProcessor = new WorkflowProcessor<Transmission.Chunk>(
                "storeDocumentIndex",
                "This is the RWI transmission process",
                new String[]{"RWI/Cache/Collections"},
                this, "storeDocumentIndex", concurrentSender * 2, null, concurrentSender);
    }
    
    public int cloudSize() {
    	return (this.transmissionCloud == null) ? 0 : this.transmissionCloud.size();
    }
    
    public int transmissionSize() {
    	return (this.indexingTransmissionProcessor == null) ? 0 : this.indexingTransmissionProcessor.queueSize();
    }
    
    /**
     * PROCESS(1)
     * select a number of index containers from the backend index.
     * Selected containers are removed from the backend.
     * @param hash
     * @param limitHash
     * @param maxContainerCount
     * @param maxtime
     * @return
     * @throws IOException
     */
    private ArrayList<ReferenceContainer<WordReference>> selectContainers(
            final byte[] hash,
            final byte[] limitHash,
            final int maxContainerCount,
            final int maxReferenceCount,
            final int maxtime) throws IOException {
        
    	// prefer file
        ArrayList<ReferenceContainer<WordReference>> containers = selectContainers(hash, limitHash, maxContainerCount, maxReferenceCount, maxtime, false);

        // if ram does not provide any result, take from file
        //if (containers.isEmpty()) containers = selectContainers(hash, limitHash, maxContainerCount, maxtime, false);
        return containers;
    }
    
    private ArrayList<ReferenceContainer<WordReference>> selectContainers(
            final byte[] hash,
            final byte[] limitHash,
            final int maxContainerCount,
            final int maxReferenceCount,
            final int maxtime,
            final boolean ram) throws IOException {
        
        final ArrayList<ReferenceContainer<WordReference>> containers = new ArrayList<ReferenceContainer<WordReference>>(maxContainerCount);
        
        final Iterator<ReferenceContainer<WordReference>> indexContainerIterator = this.segment.termIndex().references(hash, true, ram);
        ReferenceContainer<WordReference> container;
        int refcount = 0;

        // first select the container
        final long timeout = (maxtime < 0) ? Long.MAX_VALUE : System.currentTimeMillis() + maxtime;
        while (
                (containers.size() < maxContainerCount) &&
                (refcount < maxReferenceCount) &&
                (indexContainerIterator.hasNext()) &&
                (System.currentTimeMillis() < timeout) &&
                ((container = indexContainerIterator.next()) != null) &&
                ((containers.isEmpty()) ||
                 (Base64Order.enhancedCoder.compare(container.getTermHash(), limitHash) < 0))
                
        ) {
            if (container.isEmpty()) continue;
            refcount += container.size();
            containers.add(container);
        }
        // then remove the container from the backend
        final ArrayList<ReferenceContainer<WordReference>> rc;
        if (ram) {
            // selection was only from ram, so we have to carefully remove only the selected entries
            HandleSet urlHashes = new HandleSet(URIMetadataRow.rowdef.primaryKeyLength, URIMetadataRow.rowdef.objectOrder, 0);
            Iterator<WordReference> it;
            for (ReferenceContainer<WordReference> c: containers) {
                urlHashes.clear();
                it = c.entries();
                while (it.hasNext()) try { urlHashes.put(it.next().metadataHash()); } catch (RowSpaceExceededException e) { Log.logException(e); }
                if (this.log.isFine()) this.log.logFine("selected " + urlHashes.size() + " urls for word '" + UTF8.String(c.getTermHash()) + "'");
                if (!urlHashes.isEmpty()) this.segment.termIndex().remove(c.getTermHash(), urlHashes);
            }
            rc = containers;
        } else {
            // selection was from whole index, so we can just delete the whole container
            // but to avoid race conditions return the results from the deletes
            rc = new ArrayList<ReferenceContainer<WordReference>>(containers.size());
            for (ReferenceContainer<WordReference> c: containers) {
                container = this.segment.termIndex().delete(c.getTermHash()); // be aware this might be null!
                if (container != null && !container.isEmpty()) {
                    if (this.log.isFine()) this.log.logFine("selected " + container.size() + " urls for word '" + UTF8.String(c.getTermHash()) + "'");
                    rc.add(container);
                }
            }
        }
        
        // finished. The caller must take care of the containers and must put them back if not needed
        return rc;
    }

    /**
     * PROCESS(2)
     * split a list of containers into partitions according to the vertical distribution scheme
     * @param containers
     * @param scheme
     * @return
     * @throws RowSpaceExceededException 
     */
    @SuppressWarnings("unchecked")
    private List<ReferenceContainer<WordReference>>[] splitContainers(List<ReferenceContainer<WordReference>> containers) throws RowSpaceExceededException {
        
        // init the result vector
        int partitionCount = this.seeds.scheme.verticalPartitions();
        List<ReferenceContainer<WordReference>>[] partitions = (ArrayList<ReferenceContainer<WordReference>>[]) new ArrayList[partitionCount];
        for (int i = 0; i < partitions.length; i++) partitions[i] = new ArrayList<ReferenceContainer<WordReference>>();
        
        // check all entries and split them to the partitions
        ReferenceContainer<WordReference>[] partitionBuffer = new ReferenceContainer[partitionCount];
        WordReference re;
        for (ReferenceContainer<WordReference> container: containers) {
            // init the new partitions
            for (int j = 0; j < partitionBuffer.length; j++) {
                partitionBuffer[j] = new ReferenceContainer<WordReference>(Segment.wordReferenceFactory, container.getTermHash(), container.size() / partitionCount);
            }

            // split the container
            Iterator<WordReference> i = container.entries();
            while (i.hasNext()) {
                re = i.next();
                if (re == null) continue;
                partitionBuffer[this.seeds.scheme.verticalPosition(re.metadataHash())].add(re);
            }
            
            // add the containers to the result vector
            for (int j = 0; j < partitionBuffer.length; j++) {
                partitions[j].add(partitionBuffer[j]);
            }
        }
        return partitions;
    }
    
    /**
     * PROCESS(3) and PROCESS(4)
     * put containers into cloud. This needs information about the network,
     * because the possible targets are assigned here as well. The indexRepositoryReference
     * is the database of references which is needed here because this is the place where
     * finally is checked if the reference exists. If the entry does not exist for specific
     * entries in the indexContainer, then it is discarded. If it exists, then the entry is
     * stored in a cache of the Entry for later transmission to the targets, which means that
     * then no additional IO is necessary.
     */
    private void enqueueContainersToCloud(final List<ReferenceContainer<WordReference>>[] containers) {
        if (transmissionCloud == null) return;
        ReferenceContainer<WordReference> lastContainer;
        byte[] primaryTarget;
        ByteArray pTArray;
        Transmission.Chunk entry;
        for (int vertical = 0; vertical < containers.length; vertical++) {
            // the 'new' primary target is the word hash of the last container in the array
            lastContainer = containers[vertical].get(containers[vertical].size() - 1);
            primaryTarget = FlatWordPartitionScheme.positionToHash(this.seeds.scheme.dhtPosition(lastContainer.getTermHash(), vertical));
            assert primaryTarget[2] != '@';
            pTArray = new ByteArray(primaryTarget);
            
            // get or make a entry object
            entry = this.transmissionCloud.get(pTArray); // if this is not null, the entry is extended here
            List<yacySeed> targets = PeerSelection.getAcceptRemoteIndexSeedsList(
                    seeds,
                    primaryTarget,
                    seeds.redundancy() * 3,
                    true);
            this.log.logInfo("enqueueContainers: selected " + targets.size() + " targets for primary target key " + UTF8.String(primaryTarget) + "/" + vertical + " with " + containers[vertical].size() + " index containers.");
            if (entry == null) entry = transmission.newChunk(primaryTarget, targets, lastContainer.row());

            /*/ lookup targets
            int sc = 1;
            for (yacySeed seed : targets) {
            	if(seed == null) continue;
				if(seed == seeds.mySeed())	this.log.logInfo("enqueueContainers: myself-target at position " + sc);
				this.log.logInfo("enqueueContainers: primaryTarget distance at position " + sc + ": " + FlatWordPartitionScheme.std.dhtDistance(primaryTarget, null, seed));
				this.log.logInfo("enqueueContainers: distance to first container at position " + sc + ": " + FlatWordPartitionScheme.std.dhtDistance(FlatWordPartitionScheme.positionToHash(this.seeds.scheme.dhtPosition(containers[vertical].get(0).getTermHash(), vertical)), null, seed));
				sc++;
			}*/
            
            // fill the entry with the containers
            for (ReferenceContainer<WordReference> c: containers[vertical]) {
                try {
                    entry.add(c);
                } catch (RowSpaceExceededException e) {
                    Log.logException(e);
                    break;
                }
            }
            
            // put the entry into the cloud
            if (entry.containersSize() > 0) this.transmissionCloud.put(pTArray, entry);
        }
    }

    public boolean selectContainersEnqueueToCloud(
            final byte[] hash,
            final byte[] limitHash,
            final int maxContainerCount,
            final int maxReferenceCount,
            final int maxtime) {
        if (this.transmissionCloud == null) return false;

    	List<ReferenceContainer<WordReference>> selectedContainerCache;
        try {
            selectedContainerCache = selectContainers(hash, limitHash, maxContainerCount, maxReferenceCount, maxtime);
        } catch (IOException e) {
            this.log.logSevere("selectContainersEnqueueToCloud: selectedContainer failed", e);
            return false;
        }
        this.log.logInfo("selectContainersEnqueueToCloud: selectedContainerCache was filled with " + selectedContainerCache.size() + " entries");
        
        if (selectedContainerCache == null || selectedContainerCache.isEmpty()) {
        	this.log.logInfo("selectContainersEnqueueToCloud: selectedContainerCache is empty, cannot do anything here.");
        	return false;
        }

        List<ReferenceContainer<WordReference>>[] splitContainerCache;
        try {
            splitContainerCache = splitContainers(selectedContainerCache);
        } catch (RowSpaceExceededException e) {
            this.log.logSevere("selectContainersEnqueueToCloud: splitContainers failed because of too low RAM", e);
            return false;
        }
        selectedContainerCache = null;
        if (splitContainerCache == null) {
        	this.log.logInfo("selectContainersEnqueueToCloud: splitContainerCache is empty, cannot do anything here.");
        	return false;
        }
        this.log.logInfo("splitContainersFromCache: splitContainerCache filled with " + splitContainerCache.length + " partitions, deleting selectedContainerCache");
        if (splitContainerCache.length != this.seeds.scheme.verticalPartitions()) {
        	this.log.logWarning("selectContainersEnqueueToCloud: splitContainerCache has wrong length.");
        	return false;
        }
        enqueueContainersToCloud(splitContainerCache);
        splitContainerCache = null;
    	this.log.logInfo("selectContainersEnqueueToCloud: splitContainerCache enqueued to cloud array which has now " + this.transmissionCloud.size() + " entries.");
        return true;
    }
    
    /**
     * PROCESS(5)
     * take the largest container from the cloud and put it into the 'next' array,
     * where it waits to be processed.
     * This method returns true if a container was dequeued, false if not
     */
    public boolean dequeueContainer() {
    	if (transmissionCloud == null) return false;
        if (this.indexingTransmissionProcessor.queueSize() > indexingTransmissionProcessor.concurrency()) return false;
        ByteArray maxtarget = null;
        int maxsize = -1;
        for (Map.Entry<ByteArray, Transmission.Chunk> chunk: this.transmissionCloud.entrySet()) {
            if (chunk.getValue().containersSize() > maxsize) {
                maxsize = chunk.getValue().containersSize();
                maxtarget = chunk.getKey();
            }
        }
        if (maxsize < 0) return false;
        Transmission.Chunk chunk = this.transmissionCloud.remove(maxtarget);
        try {
            this.indexingTransmissionProcessor.enQueue(chunk);
        } catch (InterruptedException e) {
            Log.logException(e);
        }
        return true;
    }
    
    public Transmission.Chunk storeDocumentIndex(Transmission.Chunk chunk) {

        // do the transmission
        boolean success = chunk.transmit();
        
        if (success && chunk.isFinished()) {
            // finished with this queue!
            this.log.logInfo("STORE: Chunk " + UTF8.String(chunk.primaryTarget()) + " has FINISHED all transmissions!");
            return chunk;
        }
        
        if (!success) this.log.logInfo("STORE: Chunk " + UTF8.String(chunk.primaryTarget()) + " has failed to transmit index; marked peer as busy");
        
        if (chunk.canFinish()) {
            try {
                if (this.indexingTransmissionProcessor != null) this.indexingTransmissionProcessor.enQueue(chunk);
            } catch (InterruptedException e) {
                Log.logException(e);
                return null;
            }
            return chunk;
        }
        this.log.logInfo("STORE: Chunk " + UTF8.String(chunk.primaryTarget()) + " has not enough targets left. This transmission has failed, putting back index to backend");
        chunk.restore();
        return null;
    }

    public void close() {
        // removes all entries from the dispatcher and puts them back to a RAMRI
        if (indexingTransmissionProcessor != null) this.indexingTransmissionProcessor.announceShutdown();
        if (this.transmissionCloud != null) {
        	outerLoop: for (Map.Entry<ByteArray, Transmission.Chunk> e : this.transmissionCloud.entrySet()) {
        		for (ReferenceContainer<WordReference> i : e.getValue()) try {
        		    this.segment.termIndex().add(i);
        		} catch (Exception e1) {
        		    Log.logException(e1);
        		    break outerLoop;
        		}
        	}
        	this.transmissionCloud.clear();
        }
        this.transmissionCloud = null;
        if (indexingTransmissionProcessor != null) {
        	this.indexingTransmissionProcessor.awaitShutdown(10000);
        	this.indexingTransmissionProcessor.clear();
        }
        this.indexingTransmissionProcessor = null;
    }
    
}
