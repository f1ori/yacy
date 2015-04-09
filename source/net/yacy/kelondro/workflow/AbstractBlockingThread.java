// AbstractBlockingThread.java
// (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 27.03.2008 on http://yacy.net
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

package net.yacy.kelondro.workflow;

import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.MemoryControl;

public abstract class AbstractBlockingThread<J extends WorkflowJob> extends AbstractThread implements BlockingThread<J> {

    private WorkflowProcessor<J> manager = null;
    private final static Log log = new Log("BlockingThread");
    
    public void setManager(final WorkflowProcessor<J> manager) {
        this.manager = manager;
    }
    public WorkflowProcessor<J> getManager() {
        return this.manager;
    }

    @Override
    public void run() {
        this.open();
        if (log != null) {
            logSystem("thread '" + this.getName() + "' deployed, starting loop.");
        }
        long timestamp;
        long memstamp0, memstamp1;
        long busyCycles = 0;
        
        while (running) {
            try {
                // do job
                timestamp = System.currentTimeMillis();
                memstamp0 = MemoryControl.used();
                final J in = this.manager.take();
                if ((in == null) || (in == WorkflowJob.poisonPill) || (in.status == WorkflowJob.STATUS_POISON)) {
                    // the poison pill: shutdown
                    // a null element is pushed to the queue on purpose to signal
                    // that a termination should be made
                    //this.manager.enQueueNext((J) serverProcessorJob.poisonPill); // pass on the pill
                    this.running = false;
                    break;
                }
                final J out = this.job(in);
                if (out != null) this.manager.passOn(out);
                // do memory and busy/idle-count/time monitoring
                memstamp1 = MemoryControl.used();
                if (memstamp1 >= memstamp0) {
                    // no GC in between. this is not shure but most probable
                    memuse += memstamp1 - memstamp0;
                } else {
                    // GC was obviously in between. Add an average as simple heuristic
                    if (busyCycles > 0) memuse += memuse / busyCycles;
                }
                busytime += System.currentTimeMillis() - timestamp;
            } catch (final InterruptedException e) {
                // don't ignore this: shut down
                this.running = false;
                break;
            } catch (final Exception e) {
                Log.logException(e);
                // handle exceptions: thread must not die on any unexpected exceptions
                // if the exception is too bad it should call terminate()
                this.jobExceptionHandler(e);
            } finally {
                busyCycles++;
            }
        }
        this.close();
        logSystem("thread '" + this.getName() + "' terminated.");
    }
    
    private void logSystem(final String text) {
        if (log == null) Log.logConfig("THREAD-CONTROL", text);
        else log.logConfig(text);
    }
}
