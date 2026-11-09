// SPDX-License-Identifier: GPL-3.0-only
package com.lamalo.monarch;
import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;
public class OutputLabelsTest {
 @Test public void actualValuesAtScalingEndpoints(){assertEquals("Analog Output - current settings\nZero Scale (0SCAL) = 0 RPM\nFull Scale (FSCAL) = 500 RPM",OutputLabels.scaling(new BigDecimal("0.0000"),new BigDecimal("500.0000"),"RPM"));}
 @Test public void modeUnits(){assertEquals("RPM",OutputLabels.units("RPM"));assertEquals("Hz",OutputLabels.units("FREQ"));assertEquals("unknown/custom units (SCALE)",OutputLabels.units("SCALE"));}
 @Test public void noHardcodedEndpoints(){assertTrue(OutputLabels.scaling(new BigDecimal("10"),new BigDecimal("750"),"Hz").contains("Zero Scale (0SCAL) = 10 Hz"));assertTrue(OutputLabels.scaling(new BigDecimal("10"),new BigDecimal("750"),"Hz").contains("Full Scale (FSCAL) = 750 Hz"));}
 @Test public void noOutputHardwareAssumptions(){String label=OutputLabels.scaling(new BigDecimal("0"),new BigDecimal("500"),"RPM");assertFalse(label.contains(" V"));assertFalse(label.contains("mA"));assertFalse(label.contains("AO model"));}
}
