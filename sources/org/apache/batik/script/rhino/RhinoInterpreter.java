/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 *---------------------------------------------------------------------------*
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.script.rhino;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import org.apache.batik.script.Interpreter;
import org.apache.batik.script.InterpreterException;
import org.apache.batik.script.Window;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.w3c.dom.events.EventTarget;

/**
 * A simple implementation of <code>Interpreter</code> interface to use
 * Rhino ECMAScript interpreter.
 * @author <a href="mailto:cjolif@ilog.fr">Christophe Jolif</a>
 * @version $Id$
 */
public class RhinoInterpreter implements Interpreter {
    private static String[] TO_BE_IMPORTED = {
        "java.lang",
        "org.w3c.dom",
        "org.w3c.dom.css",
        "org.w3c.dom.events",
        "org.w3c.dom.smil",
        "org.w3c.dom.stylesheets",
        "org.w3c.dom.svg",
        "org.w3c.dom.views"
    };

    private static class Entry {
        String str;
        Script script;
        Entry(String str, Script script) {
            this.str = str;
            this.script = script;
        }
    };

    /**
     * store last 32 precompiled objects.
     */
    private static final int MAX_CACHED_SCRIPTS = 32;

    /**
     * Constant used to describe an SVG source
     */
    public static final String SOURCE_NAME_SVG = "<SVG>";

    /**
     * Name of the "window" object when referenced by scripts
     */
    public static final String BIND_NAME_WINDOW = "window";

    private ScriptableObject globalObject = null;
    private LinkedList compiledScripts = new LinkedList();
    private WrapFactory wrapFactory =
        new BatikWrapFactory(this);

    /**
     * The Rhino 'security domain'. We use the RhinoClassLoader
     * which will grant permissions to connect to the document
     * URL.
     */
    protected RhinoClassLoader rhinoClassLoader;

    /**
     * The SecurityController implementation for Batik,
     * which ensures scripts have access to the
     * server they were downloaded from
     */
    private SecurityController securityController
        = new BatikSecurityController();

    /**
     * Default Context for scripts. This is used only for efficiency
     * reason.
     */
    protected Context defaultContext;

    /**
     * Context vector, to make sure we are not
     * setting the security context too many times
     */
    protected Vector contexts = new Vector();

    /**
     * Build a <code>Interpreter</code> for ECMAScript using Rhino.
     *
     * @param documentURL the URL for the document which references
     *
     * @see org.apache.batik.script.Interpreter
     * @see org.apache.batik.script.InterpreterPool
     */
    public RhinoInterpreter(URL documentURL) {
        rhinoClassLoader = new RhinoClassLoader(documentURL,
                                                getClass().getClassLoader());
        Context.setCachingEnabled(false); // reset the cache
        Context.setCachingEnabled(true);  // enable caching again
        // entering a context
        Context ctx = enterContext();
        try {
            try {
                Scriptable scriptable = ctx.initStandardObjects(null, false);
                ScriptableObject.defineClass(scriptable, WindowWrapper.class);
            } catch (Exception e) {
                // cannot happen
            }
            // we now have the window object as the global object from the
            // launch of the interpreter.
            // 1. it works around a Rhino bug introduced in 15R4 (but fixed
            // by a later patch.
            // 2. it sounds cleaner.
            WindowWrapper wWrapper = new WindowWrapper(ctx);
            globalObject = wWrapper;
            // import Java lang package & DOM Level 2 & SVG DOM packages
            NativeJavaPackage[] p= new NativeJavaPackage[TO_BE_IMPORTED.length];
            for (int i = 0; i < TO_BE_IMPORTED.length; i++) {
                p[i] = new NativeJavaPackage(TO_BE_IMPORTED[i]);
            }
            wWrapper.importPackage(ctx, globalObject, p, null);
        } finally {
            Context.exit();
        }
    }

    /**
     * Returns the AccessControlContext associated with this Interpreter.
     * @see org.apache.batik.script.rhino.RhinoClassLoader
     */
    public AccessControlContext getAccessControlContext(){
        return rhinoClassLoader.getAccessControlContext();
    }

    /**
     * Implementation helper. Makes sure the proper security is set
     * on the context.
     */
    public Context enterContext(){
        Context ctx = Context.enter();
        if (ctx != defaultContext){
            // Set the SecurityController the Context should
            // use.
            if (!contexts.contains(ctx)) {
                ctx.setWrapFactory(wrapFactory);
                ctx.setSecurityController(securityController);
                contexts.add(ctx);

                // Hopefully, we are not switching threads too
                // often ....
                defaultContext = ctx;
            }
        }
        return ctx;
    }

    /**
     * This method returns the ECMAScript global object used by this interpreter.
     */
    protected ScriptableObject getGlobalObject() {
        return globalObject;
    }

    // org.apache.batik.script.Intepreter implementation

    /**
     * This method evaluates a piece of ECMAScript.
     * @param scriptreader a <code>java.io.Reader</code> on the piece of script
     * @return if no exception is thrown during the call, should return the
     * value of the last expression evaluated in the script.
     */
    public Object evaluate(Reader scriptreader)
        throws InterpreterException, IOException {
        return evaluate(scriptreader, SOURCE_NAME_SVG);
    }

    /**
     * This method evaluates a piece of ECMAScript.
     * @param scriptreader a <code>java.io.Reader</code> on the piece of script
     * @param description description which can be later used (e.g., for error
     *        messages).
     * @return if no exception is thrown during the call, should return the
     * value of the last expression evaluated in the script.
     */
    public Object evaluate(Reader scriptreader, String description)
        throws InterpreterException, IOException {

        Object rv = null;
        final Context ctx = enterContext();

        try {
          rv = ctx.evaluateReader(globalObject,
                                  scriptreader,
                                  description,
                                  1, rhinoClassLoader);
        } catch (JavaScriptException e) {
            // exception from JavaScript (possibly wrapping a Java Ex)
            if (e.getValue() instanceof Exception) {
                Exception ex = (Exception)e.getValue();
                throw new InterpreterException(ex, ex.getMessage(), -1, -1);
            } else
                throw new InterpreterException(e, e.getMessage(), -1, -1);
        } catch (WrappedException we) {
            // main Rhino RuntimeException
            Throwable w = we.getWrappedException();
            if (w instanceof Exception)
                throw
                    new InterpreterException((Exception)we.getWrappedException(),
                                             we.getWrappedException().getMessage(),
                                             -1, -1);
            else
                throw new InterpreterException(we.getWrappedException().getMessage(), -1, -1);
        } catch (RuntimeException re) {
            // other RuntimeExceptions
            throw new InterpreterException(re, re.getMessage(), -1, -1);
        } finally {
            Context.exit();
        }
        return rv;
    }

    /**
     * This method evaluates a piece of ECMA script.
     * The first time a String is passed, it is compiled and evaluated.
     * At next call, the piece of script will only be evaluated to
     * prevent from recompiling it.
     * @param scriptstr the piece of script
     * @return if no exception is thrown during the call, should return the
     * value of the last expression evaluated in the script.
     */
    public Object evaluate(final String scriptstr)
        throws InterpreterException {

        final Context ctx = enterContext();

        Script script = null;
        Entry et = null;
        Iterator it = compiledScripts.iterator();
        // between nlog(n) and log(n) because it is
        // an AbstractSequentialList
        while (it.hasNext()) {
            if ((et = (Entry)(it.next())).str.equals(scriptstr)) {
                // if it is not at the end, remove it because
                // it will change from place (it is faster
                // to remove it now)
                script = et.script;
                it.remove();
                break;
            }
        }

        if (script == null) {
            // this script has not been compiled yet or has been forgotten
            // since the compilation:
            // compile it and store it for future use.

            script = (Script)AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        try {
                            return ctx.compileReader(globalObject,
                                                     new StringReader(scriptstr),
                                                     SOURCE_NAME_SVG,
                                                     1, rhinoClassLoader);
                        } catch (IOException io) {
                            // Should never happen: we are using a string
                            throw new Error();
                        }
                    }
                });

            if (compiledScripts.size()+1 > MAX_CACHED_SCRIPTS) {
                // too many cached items - we should delete the oldest entry.
                // all of this is very fast on linkedlist
                compiledScripts.removeFirst();
            }
            // stroring is done here:
            compiledScripts.addLast(new Entry(scriptstr, script));
        } else {
            // this script has been compiled before,
            // just update it's index so it won't get deleted soon.
            compiledScripts.addLast(et);
        }
        Object rv = null;
        try {
            rv = script.exec(ctx, globalObject);
        } catch (JavaScriptException e) {
            // exception from JavaScript (possibly wrapping a Java Ex)
            if (e.getValue() instanceof Exception) {
                Exception ex = (Exception)e.getValue();
                throw new InterpreterException(ex, ex.getMessage(), -1, -1);
            } else
                throw new InterpreterException(e, e.getMessage(), -1, -1);
        } catch (WrappedException we) {
            // main Rhino RuntimeException
            throw
                new InterpreterException((Exception)we.getWrappedException(),
                                         we.getWrappedException().getMessage(),
                                         -1, -1);
        } catch (RuntimeException re) {
            // other RuntimeExceptions
            throw new InterpreterException(re, re.getMessage(), -1, -1);
        } finally {
            Context.exit();
        }
        return rv;
    }

    /**
     * For <code>RhinoInterpreter</code> this method does nothing.
     */
    public void dispose() {
    }

    /**
     * This method registers a particular Java <code>Object</code> in
     * the environment of the interpreter.
     * @param name the name of the script object to create
     * @param object the Java object
     */
    public void bindObject(String name, Object object) {
        Context ctx = enterContext();

        try {
            if (name.equals(BIND_NAME_WINDOW) && object instanceof Window) {
                ((WindowWrapper)globalObject).window = (Window)object;
            } else {
                Scriptable jsObject =  Context.toObject(object, globalObject);
                globalObject.put(name, globalObject, jsObject);
            }

        } finally {
            Context.exit();
        }
    }

    /**
     * To be used by <code>EventTargetWrapper</code>.
     */
    void callHandler(Function handler,
                     Object arg)
        throws JavaScriptException {
        Context ctx = enterContext();

        try {
            arg = Context.toObject(arg, globalObject);
            Object[] args = {arg};
            handler.call(ctx, globalObject, globalObject, args);
        } finally {
            Context.exit();
        }
    }

    /**
     * To be used by <code>WindowWrapper</code>.
     */
    void callMethod(ScriptableObject obj,
                    String methodName,
                    ArgumentsBuilder ab)
        throws JavaScriptException {
        Context ctx = enterContext();

        try {
            ScriptableObject.callMethod(obj, methodName, ab.buildArguments());
        } finally {
            Context.exit();
        }
    }

    /**
     * To be used by <code>WindowWrapper</code>.
     */
    void callHandler(Function handler,
                     Object[] args)
        throws JavaScriptException {
        Context ctx = enterContext();

        try {
            handler.call(ctx, globalObject, globalObject, args);
        } finally {
            Context.exit();
        }
    }

    /**
     * To be used by <code>WindowWrapper</code>.
     */
    void callHandler(Function handler,
                     ArgumentsBuilder ab)
        throws JavaScriptException {
        Context ctx = enterContext();

        try {
            handler.call(ctx, globalObject, globalObject, ab.buildArguments());
        } finally {
            Context.exit();
        }
    }

    /**
     * To build an argument list.
     */
    public interface ArgumentsBuilder {
        Object[] buildArguments();
    }

    /**
     * Build the wrapper for objects implement <code>EventTarget</code>.
     */
    Scriptable buildEventTargetWrapper(EventTarget obj) {
        return new EventTargetWrapper(globalObject, obj, this);
    }

    /**
     * By default Rhino has no output method in its language. That's why
     * this method does nothing.
     * @param output the new out <code>Writer</code>.
     */
    public void setOut(Writer out) {
        // no implementation of a default output function in Rhino
    }

    // org.apache.batik.i18n.Localizable implementation

    /**
     * Provides a way to the user to specify a locale which override the
     * default one. If null is passed to this method, the used locale
     * becomes the global one.
     * @param l The locale to set.
     */
    public Locale getLocale() {
        // <!> TODO : in Rhino the local is for a thread not a scope..
        return null;
    }

    /**
     * Returns the current locale or null if the locale currently used is
     * the default one.
     */
    public void setLocale(Locale locale) {
        // <!> TODO : in Rhino the local is for a thread not a scope..
    }

    /**
     * Creates and returns a localized message, given the key of the message, 0, data.length
     * in the resource bundle and the message parameters.
     * The messages in the resource bundle must have the syntax described in
     * the java.text.MessageFormat class documentation.
     * @param key  The key used to retreive the message from the resource
     *             bundle.
     * @param args The objects that compose the message.
     * @exception MissingResourceException if the key is not in the bundle.
     */
    public String formatMessage(String key, Object[] args) {
        return null;
    }
}
