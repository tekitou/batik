/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.batik.css.engine.SVGCSSEngine;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.util.CSSConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSPrimitiveValue;

/**
 * A collection of utility method for text.
 *
 * @author <a href="stephane@hillion.org">Stephane Hillion</a>
 * @author <a href="bill.haneman@ireland.sun.com>Bill Haneman</a>
 * @version $Id$
 */
public abstract class TextUtilities implements CSSConstants, ErrorConstants {

    /**
     * Returns the content of the given element.
     */
    public static String getElementContent(Element e) {
        StringBuffer result = new StringBuffer();
        for (Node n = e.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:
                result.append(getElementContent((Element)n));
                break;
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
                result.append(n.getNodeValue());
            }
        }
        return result.toString();
    }

    /**
     * Returns the float list that represents a set of horizontal
     * values or percentage.
     *
     * @param element the element that defines the specified coordinates
     * @param attrName the name of the attribute (used by error handling)
     * @param valueStr the delimited string containing values of the coordinate
     * @param ctx the bridge context
     */
    public static
        ArrayList svgHorizontalCoordinateArrayToUserSpace(Element element,
                                                          String attrName,
                                                          String valueStr,
                                                          BridgeContext ctx) {

        UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, element);
        ArrayList values = new ArrayList();
        StringTokenizer st = new StringTokenizer(valueStr, ", ", false);
        while (st.hasMoreTokens()) {
            values.add
                (new Float(UnitProcessor.svgHorizontalCoordinateToUserSpace
                           (st.nextToken(), attrName, uctx)));
        }
        return values;
    }

    /**
     * Returns the float list that represents a set of values or percentage.
     *
     *
     * @param element the element that defines the specified coordinates
     * @param attrName the name of the attribute (used by error handling)
     * @param valueStr the delimited string containing values of the coordinate
     * @param ctx the bridge context
     */
    public static
        ArrayList svgVerticalCoordinateArrayToUserSpace(Element element,
                                                        String attrName,
                                                        String valueStr,
                                                        BridgeContext ctx) {

        UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, element);
        ArrayList values = new ArrayList();
        StringTokenizer st = new StringTokenizer(valueStr, ", ", false);
        while (st.hasMoreTokens()) {
            values.add
                (new Float(UnitProcessor.svgVerticalCoordinateToUserSpace
                           (st.nextToken(), attrName, uctx)));
        }
        return values;
    }


    public static ArrayList svgRotateArrayToFloats(Element element,
                                                   String attrName,
                                                   String valueStr,
                                                   BridgeContext ctx) {

        StringTokenizer st = new StringTokenizer(valueStr, ", ", false);
        ArrayList values = new ArrayList();
        String s;
        while (st.hasMoreTokens()) {
            try {
                s = st.nextToken();
                values.add
                    (new Float(Math.toRadians
                               (SVGUtilities.convertSVGNumber(s))));
            } catch (NumberFormatException ex) {
                throw new BridgeException
                    (element, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object [] {attrName, valueStr});
            }
        }
        return values;
    }

    /**
     * Converts the font-size CSS value to a float value.
     * @param e the element
     */
    public static Float convertFontSize(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.FONT_SIZE_INDEX);
        return new Float(v.getFloatValue());
    }

    /**
     * Converts the font-style CSS value to a float value.
     * @param e the element
     */
    public static Float convertFontStyle(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.FONT_STYLE_INDEX);
        switch (v.getStringValue().charAt(0)) {
        case 'n':
            return TextAttribute.POSTURE_REGULAR;
        default:
            return TextAttribute.POSTURE_OBLIQUE;
        }
    }

    /**
     * Converts the font-stretch CSS value to a float value.
     * @param e the element
     */
    public static Float convertFontStretch(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.FONT_STRETCH_INDEX);
        String s = v.getStringValue();
        switch (s.charAt(0)) {
        case 'u':
            if (s.charAt(6) == 'c') {
                return TextAttribute.WIDTH_CONDENSED;
            } else {
                return TextAttribute.WIDTH_EXTENDED;
            }

        case 'e':
            if (s.charAt(6) == 'c') {
                return TextAttribute.WIDTH_CONDENSED;
            } else {
                if (s.length() == 8) {
                    return TextAttribute.WIDTH_SEMI_EXTENDED;
                } else {
                    return TextAttribute.WIDTH_EXTENDED;
                }
            }

        case 's':
            if (s.charAt(6) == 'c') {
                return TextAttribute.WIDTH_SEMI_CONDENSED;
            } else {
                return TextAttribute.WIDTH_SEMI_EXTENDED;
            }

        default:
            return TextAttribute.WIDTH_REGULAR;
        }
    }

    /**
     * Converts the font-weight CSS value to a float value.
     * @param e the element
     */
    public static Float convertFontWeight(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.FONT_WEIGHT_INDEX);
        float f = v.getFloatValue();
        switch ((int)f) {
        case 100:
            return TextAttribute.WEIGHT_EXTRA_LIGHT;
        case 200:
            return TextAttribute.WEIGHT_LIGHT;
        case 300:
            return TextAttribute.WEIGHT_DEMILIGHT;
        case 400:
            return TextAttribute.WEIGHT_REGULAR;
        case 500:
            return TextAttribute.WEIGHT_SEMIBOLD;
        default:
            return TextAttribute.WEIGHT_BOLD;
        }
    }

    /**
     * Converts the text-anchor CSS value to a TextNode.Anchor.
     * @param e the element
     */
    public static TextNode.Anchor convertTextAnchor(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.TEXT_ANCHOR_INDEX);
        switch (v.getStringValue().charAt(0)) {
        case 's':
            return TextNode.Anchor.START;
        case 'm':
            return TextNode.Anchor.MIDDLE;
        default:
            return TextNode.Anchor.END;
        }
    }

    /**
     * Converts a baseline-shift CSS value to a value usable as a text
     * attribute, or null.
     * @param e the element
     */
    public static Object convertBaselineShift(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.BASELINE_SHIFT_INDEX);
        if (v.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
            String s = v.getStringValue();
            switch (s.charAt(2)) {
            case 'p': //suPerscript
                return TextAttribute.SUPERSCRIPT_SUPER;

            case 'b': //suBscript
                return TextAttribute.SUPERSCRIPT_SUB;

            default:
                return null;
            }
        } else {
            return new Float(v.getFloatValue());
        }
    }

    /**
     * Converts a kerning CSS value to a value usable as a text
     * attribute, or null.
     * @param e the element
     */
    public static Float convertKerning(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.KERNING_INDEX);
        if (v.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
            return null;
        }
        return new Float(v.getFloatValue());
    }

    /**
     * Converts a letter-spacing CSS value to a value usable as a text
     * attribute, or null.
     * @param e the element
     */
    public static Float convertLetterSpacing(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.LETTER_SPACING_INDEX);
        if (v.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
            return null;
        }
        return new Float(v.getFloatValue());
    }

    /**
     * Converts a word-spacing CSS value to a value usable as a text
     * attribute, or null.
     * @param e the element
     */
    public static Float convertWordSpacing(Element e) {
        Value v = CSSUtilities.getComputedStyle
            (e, SVGCSSEngine.WORD_SPACING_INDEX);
        if (v.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
            return null;
        }
        return new Float(v.getFloatValue());
    }
}
