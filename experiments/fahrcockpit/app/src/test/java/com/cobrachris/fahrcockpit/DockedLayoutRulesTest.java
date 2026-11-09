package com.cobrachris.fahrcockpit;
import org.junit.Test;
import static org.junit.Assert.*;
public final class DockedLayoutRulesTest {
    @Test public void landscapeDefaultsRightAndSupportsLeft() {
        assertTrue(DockedLayoutRules.landscape(1920,1080));
        assertEquals(576,DockedLayoutRules.width(1920,true));
        assertEquals(1344,DockedLayoutRules.x(1920,576,true,true));
        assertEquals(0,DockedLayoutRules.x(1920,576,true,false));
        assertEquals(1730,DockedLayoutRules.collapsedX(1920,190,true,true));
        assertEquals(0,DockedLayoutRules.collapsedX(1920,190,true,false));
        assertEquals(0,DockedLayoutRules.y(1080,1037,true));
    }
    @Test public void portraitUsesBottomThirtyPercent() {
        assertFalse(DockedLayoutRules.landscape(1080,1920));
        assertEquals(1080,DockedLayoutRules.width(1080,false));
        assertEquals(576,DockedLayoutRules.height(1920,false));
        assertEquals(1344,DockedLayoutRules.y(1920,576,false));
        assertEquals(0,DockedLayoutRules.x(1080,1080,false,true));
        assertEquals(890,DockedLayoutRules.collapsedX(1080,190,false,true));
        assertEquals(890,DockedLayoutRules.collapsedX(1080,190,false,false));
    }
    @Test public void tinyDimensionsAreClamped() {
        assertTrue(DockedLayoutRules.width(0,true)>=1);
        assertTrue(DockedLayoutRules.height(0,false)>=1);
        assertEquals(0,DockedLayoutRules.x(0,1,true,true));
        assertEquals(0,DockedLayoutRules.x(0,1,true,false));
        assertEquals(0,DockedLayoutRules.collapsedX(0,1,true,true));
    }
}
