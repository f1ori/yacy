/**
 *  Accumulator
 *  Copyright 2010 by Michael Peter Christen
 *  First released 07.01.2011 at http://yacy.net
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

package net.yacy.cora.services.federated;

/**
 * place-holder class to provide a object declaration for threads in Search object
 */
public interface SearchAccumulator extends Runnable {

    /**
     * join this accumulator: wait until it terminates
     * @throws InterruptedException
     */
    public void join() throws InterruptedException;

    /**
     * test if the accumulator is still running
     * @return
     */
    public boolean isAlive();
    
}
