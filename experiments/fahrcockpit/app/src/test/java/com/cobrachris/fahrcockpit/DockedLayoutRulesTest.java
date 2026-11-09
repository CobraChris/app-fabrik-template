package com.cobrachris.fahrcockpit;
import org.junit.Test;
import static org.junit.Assert.*;
public final class DockedLayoutRulesTest {
    @Test public void landscapeUsesLeftThirtyPercent() {
        assertTrue(DockedLayoutRules.landscape(1920,1080));
        assertEquals(576,DockedLayoutRules.width(1920,true));
        assertEquals(0,DockedLayoutRules.x(1920,576,true));
        assertEquals(0,DockedLayoutRules.collapsedX(1920,190,true));
        assertEquals(0,DockedLayoutRules.y(1080,1037,true));
    }
    @Test public void portraitUsesBottomThirtyPercent() {
        assertFalse(DockedLayoutRules.landscape(1080,1920));
        assertEquals(1080,DockedLayoutRules.width(1080,false));
        assertEquals(576,DockedLayoutRules.height(1920,false));
        assertEquals(1344,DockedLayoutRules.y(1920,576,false));
        assertEquals(890,DockedLayoutRules.collapsedX(1080,190,false));
    }
    @Test public void tinyDimensionsAreClamped() {
        assertTrue(DockedLayoutRules.width(0,true)>=1);
        assertTrue(DockedLayoutRules.height(0,false)>=1);
        assertEquals(0,DockedLayoutRules.x(0,1,true));
    }
}
