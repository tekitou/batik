/*

   Copyright 1999-2003  The Apache Software Foundation 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.apache.batik.script;

import org.apache.batik.bridge.BridgeContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This interface represents the 'window' object defined in the global
 * environment of a SVG document.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public interface Window {
    /**
     * Evaluates the given string repeatedly after the given amount of
     * time.  This method does not stall the script: the evaluation is
     * scheduled and the script continues its execution.
     * @return an object representing the interval created.
     */
    Object setInterval(String script, long interval);

    /**
     * Calls the 'run' method of the given Runnable repeatedly after
     * the given amount of time.  This method does not stall the
     * script: the evaluation is scheduled and the script continues
     * its execution.
     * @return an object representing the interval created.
     */
    Object setInterval(Runnable r, long interval);

    /**
     * Cancels an interval that was set by a call to 'setInterval'.
     */
    void clearInterval(Object interval);

    /**
     * Evaluates the given string after the given amount of time.
     * This method does not stall the script: the evaluation is
     * scheduled and the script continues its execution.
     * @return an object representing the timeout created.
     */
    Object setTimeout(String script, long timeout);

    /**
     * Calls the 'run' method of the given Runnable after the given
     * amount of time.  This method does not stall the script: the
     * evaluation is scheduled and the script continues its execution.
     * @return an object representing the timeout created.
     */
    Object setTimeout(Runnable r, long timeout);

    /**
     * Cancels an timeout that was set by a call to 'setTimeout'.
     */
    void clearTimeout(Object timeout);

    /**
     * Parses the given XML string into a DocumentFragment of the
     * given document or a new document if 'doc' is null.
     * @return The document fragment or null on error.
     */
    Node parseXML(String text, Document doc);

    /**
     * Gets data from the given URI.
     * @param uri The URI where the data is located.
     * @param h A handler called when the data is available.
     */
    void getURL(String uri, GetURLHandler h);

    /**
     * Gets data from the given URI.
     * @param uri The URI where the data is located.
     * @param h A handler called when the data is available.
     * @param enc The character encoding of the data.
     */
    void getURL(String uri, GetURLHandler h, String enc);

    /**
     * To handle the completion of a 'getURL()' call.
     */
    public interface GetURLHandler {
        
        /**
         * Called before 'getURL()' returns.
         * @param success Whether the data was successfully retreived.
         * @param mime The data MIME type.
         * @param content The data.
         */
        void getURLDone(boolean success, String mime, String content);
    }

    /**
     * Displays an alert dialog box.
     */
    void alert(String message);

    /**
     * Displays a confirm dialog box.
     */
    boolean confirm(String message);

    /**
     * Displays an input dialog box.
     * @return The input of the user, or null if the dialog was cancelled.
     */
    String prompt(String message);

    /**
     * Displays an input dialog box, given the default value.
     * @return The input of the user, or null if the dialog was cancelled.
     */
    String prompt(String message, String defVal);

    /**
     * Returns the current BridgeContext. This object given a deep
     * access to the viewer internals.
     */
    BridgeContext getBridgeContext();

    /**
     * Returns the associated interpreter.
     */
    Interpreter getInterpreter();
}
