// SPDX-License-Identifier: GPL-3.0-only
package com.lamalo.monarch;
import java.math.BigDecimal;
/** Display-unit scaling labels without assumptions about fitted output hardware. */
final class OutputLabels {
 static String units(String mode){return mode.equals("RPM")?"RPM":mode.equals("FREQ")?"Hz":"unknown/custom units (SCALE)";}
 static String scaling(BigDecimal zero,BigDecimal full,String units){
  return "Analog Output - current settings\nZero Scale (0SCAL) = "+zero.stripTrailingZeros().toPlainString()+" "+units+"\nFull Scale (FSCAL) = "+full.stripTrailingZeros().toPlainString()+" "+units;
 }
}
