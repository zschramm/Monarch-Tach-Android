// SPDX-License-Identifier: GPL-3.0-only
package com.lamalo.monarch;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
public class DeviceTranscriptTest {
 // Exact hardware reply layout; serial number anonymized for public source.
 static final String C2="@C2\rOK\r\rCH_A \rTYPE  =    RPM \rSCALE =  1.0000\rINPUT =    NEG \rLOEND =   12SEC\rGATE  =    STD \rDECPT =       1\rDAC1 \r0SCAL =  0.0000\rFSCAL = 200.000\rDIAG  =   ZERO \rOUTPT =    NEG \rDISPR =    HALF\rSERNO = 0000001\r\r";
 @Test public void actualReadSequenceWithoutWrites()throws Exception{
  List<String> sent=new ArrayList<>();
  Protocol p=new Protocol(c->{sent.add(c);switch(c){case "@PI":return "@PI\rOK\rACT-1B\r1R0.91\r\n";case "@C2":return C2;case "@DAC1/0SCAL":return "@DAC1/0SCAL\r  0.0000\r";case "@DAC1/FSCAL":return "@DAC1/FSCAL\r200.0000\r";default:throw new IOException("Unexpected command");}});
  p.identify();Protocol.Config config=p.config();config.expect("0SCAL",p.read("0SCAL").toPlainString());config.expect("FSCAL",p.read("FSCAL").toPlainString());
  assertEquals(Arrays.asList("@PI","@C2","@DAC1/0SCAL","@DAC1/FSCAL"),sent);
  assertEquals("RPM",config.get("TYPE"));assertEquals("1.0000",config.get("SCALE"));assertEquals("1",config.get("DECPT"));
 }
 @Test public void delimiterFormattingIsEquivalent()throws Exception{Protocol.Config c=new Protocol.Config(String.join("\n",Protocol.parse("@C2",C2)));c.matches(new Protocol.Config(SettingsTest.BASE));}
 @Test public void scaleModeDoesNotConfuseScaleField()throws Exception{Protocol.Config c=new Protocol.Config(String.join("\n",Protocol.parse("@C2",C2.replace("TYPE  =    RPM","TYPE  =    SCALE"))));assertEquals("SCALE",c.get("TYPE"));assertEquals("1.0000",c.get("SCALE"));}
 @Test public void changedUnrelatedFieldStillRejected()throws Exception{Protocol.Config c=new Protocol.Config(String.join("\n",Protocol.parse("@C2",C2)));try{c.unchangedExcept(new Protocol.Config(c.text.replace("INPUT NEG","INPUT POS")),"FSCAL");fail();}catch(IOException expected){}}
 @Test public void timedOutIdentityDoesNotRetryOrWrite(){List<String> sent=new ArrayList<>();try{new Protocol(c->{sent.add(c);throw new IOException("startup timeout");}).identify();fail();}catch(IOException expected){}assertEquals(Arrays.asList("@PI"),sent);}
}
