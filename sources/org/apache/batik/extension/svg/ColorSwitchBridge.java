/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.extension.svg;

import java.awt.Color;
import java.awt.Paint;

import org.apache.batik.bridge.AbstractSVGBridge;
import org.apache.batik.bridge.Bridge;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.PaintBridge;
import org.apache.batik.bridge.SVGUtilities;
import org.apache.batik.gvt.GraphicsNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bridge class for a regular polygon element.
 *
 * @author <a href="mailto:thomas.deweese@kodak.com">Thomas Deweese</a>
 */
public class ColorSwitchBridge 
    extends AbstractSVGBridge
    implements PaintBridge, BatikExtConstants {

    /**
     * Constructs a new bridge for the &lt;rect> element.
     */
    public ColorSwitchBridge() { /* nothing */ }

    /**
     * Returns the SVG namespace URI.
     */
    public String getNamespaceURI() {
        return BATIK_EXT_NAMESPACE_URI;
    }

    /**
     * Returns 'rect'.
     */
    public String getLocalName() {
        return BATIK_EXT_COLOR_SWITCH_TAG;
    }

    /**
     * Creates a <tt>Paint</tt> according to the specified parameters.
     *
     * @param ctx the bridge context to use
     * @param paintElement the element that defines a Paint
     * @param paintedElement the element referencing the paint
     * @param paintedNode the graphics node on which the Paint will be applied
     * @param opacity the opacity of the Paint to create
     */
    public Paint createPaint(BridgeContext ctx,
                             Element paintElement,
                             Element paintedElement,
                             GraphicsNode paintedNode,
                             float opacity) {
        Element clrDef = null;
        for (Node n = paintElement.getFirstChild(); 
             n != null; 
             n = n.getNextSibling()) {
            if ((n.getNodeType() != Node.ELEMENT_NODE))
                continue;
            Element ref = (Element)n;
            if ( // (ref instanceof SVGTests) &&
                SVGUtilities.matchUserAgent(ref, ctx.getUserAgent())) {
                clrDef = ref;
                break;
            }
        }

        if (clrDef == null)
            return Color.black;

        Bridge bridge = ctx.getBridge(clrDef);
        if (bridge == null || !(bridge instanceof PaintBridge))
            return Color.black;

        return ((PaintBridge)bridge).createPaint(ctx, clrDef, 
                                                 paintedElement,
                                                 paintedNode,
                                                 opacity);
    }
}
