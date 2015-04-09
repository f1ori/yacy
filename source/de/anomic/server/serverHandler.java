// serverHandler.java
// -------------------------------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
// last major change: 05.04.2004
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

/*
  serverHandler:

  A Generic Server becomes a server for s specific protocol by impementation of
  a corresponding handler class. The handler class provides methods for each
  command of the protocol that is implemented.
  The Handler class is assigned to the serverCore by passing the handlers
  name to the serverCore upon initialization.
  Example:
  serverCore server = new serverCore(port, 1000, 0, false, "ftpdProtocol", null, 0);
  In this example the protocol handler "ftpdProtocol" is assigned. There a class
  named ftpdProtocol.java must be implemented, that implements this interface,
  a serverHandler.
  Any protocol command can be implemented in either way:

    public String      COMMAND(String arg) throws IOException;
    public InputStream COMMAND(String arg) throws IOException;
    public void        COMMAND(String arg) throws IOException;

  ..where COMMAND is the command that had been passed to the server
  on the terminal connection. The 'arg' argument is the remaining part of
  the command on the terminal connection.
  If the handler method returns a NULL value, which is especially
  the case if the method implements a 'void' return-value method,
  then the server disconnects the connection.
  Any other return value (String or an InputStream) is returned to
  the client on it's own line through the terminal connection.
  If it is wanted that the server terminates right after submitting
  a last line, then this can be indicated by prefixing the return
  value by a '!'-character.

  If one of the command methods throws a IOException, then the
  server asks the error - method for a return value on the terminal
  connection.

  The greeting-method is used to request a string that is transmitted
  to the client as terminal output at the beginning of a connection
  session.
*/

package de.anomic.server;

import java.io.IOException;

import de.anomic.server.serverCore.Session;


public interface serverHandler {

    // a response line upon connection is send to client
    // if no response line is wanted, return "" or null
    public String greeting();

    // return string in case of any error that occurs during communication
    // is always (but not only) called if an IO-dependent exception occurs.
    public String error(Throwable e);
    
    // clone method for the handler prototype
    // each time a server makes a new connection it clones the hanlder prototype
    // the clone method does not need to clone every detail of a handler connection,
    // but only the necessary one for a newly initialized instance
    public serverHandler clone();
    
    /** 
     * Instead of using clone this function can be used to reset an existing 
     * handler prototype so that it can e reused 
     */
    public void reset();
    
    /**
     * Tthis function will be called by the {@link serverCore}.listen() function
     * if the whole request line is empty and therefore no function of this
     * serverHandlerClass can be called because of the missing command name
     */
    public Boolean EMPTY(String arg, Session session) throws IOException;
    
    /** 
     * This function will be called by the {@link serverCore}.listen() function
     * if no corresponding funktion of the serverHandler class can be
     * found for the received command.
     */
    public Boolean UNKNOWN(String requestLine, Session session) throws IOException;    
}
