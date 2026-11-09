// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Monarch Utility contributors
package com.lamalo.monarch;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
public class SettingsTest {
 static final String BASE="CH_A TYPE RPM SCALE 1.0000 INPUT NEG LOEND 12SEC GATE STD DECPT 1 DAC1 0SCAL 0.0000 FSCAL 200.000 DIAG ZERO OUTPT NEG DISPR HALF SERNO 0000001";
 static class Mock implements Protocol.Link {
  String state=BASE;List<String> sent=new ArrayList<>();String fail="",extra="";
  public String exchange(String c)throws IOException{sent.add(c);if(c.equals(fail))throw new IOException("timeout");String data="OK";if(c.equals("@C2"))data=state;else if(c.startsWith("@DECPT = "))state=state.replace("DECPT 1","DECPT "+c.substring(c.lastIndexOf(' ')+1));else if(c.startsWith("@CH_A/TYPE = "))state=state.replaceFirst("TYPE \\S+","TYPE "+c.substring(c.lastIndexOf(' ')+1));else if(c.startsWith("@CH_A/TYPE/SCALE = "))state=state.replaceFirst("(?<!TYPE )SCALE \\S+","SCALE "+c.substring(c.lastIndexOf(' ')+1));if(!extra.isEmpty()&&c.contains(" = "))state=state.replace("INPUT NEG",extra);return c+"\r\n"+data+"\r\n";}
  long writes(){return sent.stream().filter(c->c.contains(" = ")).count();}
 }
 @Test public void pprExact(){assertEquals("60",Protocol.pprScale("1"));assertEquals("30",Protocol.pprScale("2"));assertEquals("0.9375",Protocol.pprScale("64"));}
 @Test public void pprLimitsAndRounding(){for(String s:new String[]{"0","1000","-1","1.5","7","999","NaN","1\n@RE 96"})try{Protocol.pprScale(s);fail(s);}catch(IllegalArgumentException expected){}}
 @Test public void configReads()throws Exception{Protocol.Config c=new Protocol.Config("OK\n"+BASE);assertEquals("RPM",c.get("TYPE"));c.expect("FSCAL","200");}
 @Test public void missingAndDuplicateRefused(){for(String s:new String[]{"TYPE RPM","TYPE RPM TYPE FREQ"})try{new Protocol.Config(s).get(s.contains("FREQ")?"TYPE":"SCALE");fail();}catch(IOException expected){}}
 @Test public void decimalOnly()throws Exception{Mock m=new Mock();new Protocol(m).applyDecimal(new Protocol.Config(BASE),"3");assertEquals(1,m.writes());assertTrue(m.state.contains("DECPT 3"));assertTrue(m.state.contains("TYPE RPM SCALE 1.0000"));}
 @Test public void unsupportedDecimalNoIO(){Mock m=new Mock();try{new Protocol(m).applyDecimal(new Protocol.Config(BASE),"4");fail();}catch(IllegalArgumentException expected){}catch(IOException e){fail();}assertTrue(m.sent.isEmpty());}
 @Test public void frequencyLeavesAnalogNumbers()throws Exception{Mock m=new Mock();new Protocol(m).applyMode(new Protocol.Config(BASE),"FREQ");assertEquals(1,m.writes());assertTrue(m.state.contains("0SCAL 0.0000 FSCAL 200.000"));}
 @Test public void pprOrdersTypeThenScale()throws Exception{Mock m=new Mock();new Protocol(m).applyPpr(new Protocol.Config(BASE),"2");assertEquals(Arrays.asList("@C2","@CH_A/TYPE = SCALE","@C2","@CH_A/TYPE/SCALE = 30","@C2"),m.sent);assertTrue(m.state.contains("TYPE SCALE SCALE 30"));}
 @Test public void alreadyScaleSkipsType()throws Exception{Mock m=new Mock();m.state=BASE.replace("TYPE RPM","TYPE SCALE");new Protocol(m).applyPpr(new Protocol.Config(m.state),"1");assertEquals(1,m.writes());}
 @Test public void partialPprStopsWithoutRollback()throws Exception{Mock m=new Mock();m.fail="@CH_A/TYPE/SCALE = 30";try{new Protocol(m).applyPpr(new Protocol.Config(BASE),"2");fail();}catch(IOException expected){}assertEquals(2,m.writes());assertTrue(m.state.contains("TYPE SCALE"));assertEquals(m.fail,m.sent.get(m.sent.size()-1));}
 @Test public void preflightChangesPreventAnyWrite()throws Exception{Mock m=new Mock();m.state=BASE.replace("FSCAL 200.000","FSCAL 500");try{new Protocol(m).applyDecimal(new Protocol.Config(BASE),"2");fail();}catch(IOException expected){}assertEquals(0,m.writes());}
 @Test public void unrelatedChangeStopsPprBeforeSecondWrite()throws Exception{Mock m=new Mock();m.extra="INPUT POS";try{new Protocol(m).applyPpr(new Protocol.Config(BASE),"2");fail();}catch(IOException expected){}assertEquals(1,m.writes());}
 @Test public void scaleChangesAllowedOnlyWithType()throws Exception{Protocol.Config a=new Protocol.Config(BASE),b=new Protocol.Config(BASE.replace("SCALE 1.0000","SCALE 60"));a.unchangedExcept(b,"TYPE","SCALE");try{a.unchangedExcept(b,"DECPT");fail();}catch(IOException expected){}}
}



