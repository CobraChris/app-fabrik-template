package com.cobrachris.sanftwecker;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.TimeZone;
public final class DynamicAlarmTest {
    @Test public void acceptsMoreThanTwentyProfiles() {
        for(int id=1;id<=25;id++) {
            AlarmProfile p=AlarmProfile.defaults(id);
            assertEquals(id,p.id);
            assertFalse(p.enabled);
            assertTrue(p.days>0);
        }
    }
    @Test public void copiesWithoutAccidentallyEnabling() {
        AlarmProfile p=AlarmProfile.defaults(7).withEnabled(true)
                .withExceptions(12345,23456,12345);
        AlarmProfile copy=p.duplicateAs(24);
        assertEquals(24,copy.id);
        assertFalse(copy.enabled);
        assertEquals(0,copy.skipAt);
        assertEquals(0,copy.oneTimeAt);
        assertTrue(copy.name.contains("Kopie"));
    }
    @Test public void equalityIncludesSnoozeAndExceptions() {
        AlarmProfile p=AlarmProfile.defaults(4);
        assertTrue(p.sameConfiguration(p.withEnabled(false)));
        assertFalse(p.sameConfiguration(p.withEnabled(true)));
        assertFalse(p.sameConfiguration(p.withExceptions(1,0,0)));
    }
    @Test public void snoozeCanBeDisabled() {
        AlarmProfile p=new AlarmProfile(10,"Probe",false,7,15,127,
                0,600,35,6,20,5,0,true,20,0,0,0,false);
        assertFalse(p.snoozeEnabled);
        assertFalse(p.duplicateAs(11).snoozeEnabled);
        assertFalse(p.withEnabled(true).snoozeEnabled);
    }
}
