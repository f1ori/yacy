// IndexReader.java
// (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 2.4.2008 on http://yacy.net
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

package net.yacy.kelondro.rwi;

import net.yacy.kelondro.index.HandleSet;
import net.yacy.kelondro.order.CloneableIterator;


public interface IndexReader<ReferenceType extends Reference> {

    public int size();
    public boolean has(byte[] wordHash); // should only be used if in case that true is returned the getContainer is NOT called
    public ReferenceContainer<ReferenceType> get(byte[] wordHash, HandleSet urlselection); 
    public CloneableIterator<ReferenceContainer<ReferenceType>> references(byte[] startWordHash, boolean rot);
    public void close();
    
}
