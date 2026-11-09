// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Monarch Utility contributors
package com.lamalo.monarch;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
public class ProtocolTest {
 @Test public void canonicalNumbers(){assertEquals("200",Protocol.value("200.0000"));assertEquals("0",Protocol.value("00000.0000"));assertEquals("99999",Protocol.value("99999"));}
 @Test public void invalidNumbers(){for(String v:new String[]{"-1","NaN","1e3","1,2","100000","99999.1","1.00001","1\r\n@RE 96",""}){try{Protocol.value(v);fail(v);}catch(IllegalArgumentException expected){}}}
 @Test public void echoedResponse()throws Exception{assertEquals(Arrays.asList("OK","ACT-1B","1R0.91"),Protocol.parse("@PI","@PI\r\nOK\r\nACT-1B\r\n1R0.91\r\n"));}
 @Test public void rejectsUncorrelatedReplies(){for(String r:new String[]{"\r\n","@C2\r\nOK\r\n","0.0\r\n@PI\r\nOK\r\n","@PI\r\nERR\r\n","@PI\r\n@PI\r\nOK"})try{Protocol.parse("@PI",r);fail(r);}catch(IOException expected){}}

 @Test public void noEchoIdentity()throws Exception{assertTrue(new Protocol(c->"OK\r\nACT-1B\r\n1R0.91\r\n").identify().contains("ACT-1B"));}
 @Test public void noEchoWriteReadback()throws Exception{List<String> sent=new ArrayList<>();new Protocol(c->{sent.add(c);return c.contains(" = ")?"OK\r\n":"5000.0000\r\n";}).write("FSCAL","5000");assertEquals(2,sent.size());}
 @Test public void ackAloneIsNotIdentity(){try{new Protocol(c->"OK\r\n").identify();fail();}catch(IOException expected){}}
 @Test public void readValue()throws Exception{assertEquals("200.0000",new Protocol(c->c+"\r\n200.0000\r\n").read("FSCAL").toPlainString());}
 @Test public void wrongModelBlocked()throws Exception{try{new Protocol(c->c+"\r\nOK\r\nACT-3X\r\n").identify();fail();}catch(IOException expected){}}
 @Test public void singleWriteAndReadback()throws Exception{List<String> sent=new ArrayList<>();new Protocol(c->{sent.add(c);return c+"\r\n"+(c.contains(" = ")?"OK":"5000.0000")+"\r\n";}).write("FSCAL","5000");assertEquals(Arrays.asList("@DAC1/FSCAL = 5000","@DAC1/FSCAL"),sent);}
 @Test public void timeoutDoesNotRetry()throws Exception{List<String> sent=new ArrayList<>();try{new Protocol(c->{sent.add(c);throw new IOException("timeout");}).write("FSCAL","5000");fail();}catch(IOException expected){}assertEquals(1,sent.size());}
 @Test public void mismatchDoesNotRollback()throws Exception{List<String> sent=new ArrayList<>();try{new Protocol(c->{sent.add(c);return c+"\r\n"+(c.contains(" = ")?"OK":"200")+"\r\n";}).write("FSCAL","5000");fail();}catch(IOException expected){}assertEquals(2,sent.size());}
 @Test public void onlyScaleKeys(){try{new Protocol(c->"").write("DIAG","0");fail();}catch(IllegalArgumentException expected){}catch(IOException wrong){fail();}}
 @Test public void dataOnlyWriteAckNotAccepted(){try{new Protocol(c->c+"\r\n5000\r\n").write("FSCAL","5000");fail();}catch(IOException expected){}}
 @Test public void extraNumericReplyRejected(){try{new Protocol(c->c+"\r\n200\r\n300\r\n").read("FSCAL");fail();}catch(IOException expected){}}
}

