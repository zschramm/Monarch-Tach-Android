// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Monarch Utility contributors
package com.lamalo.monarch;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
public class DisplayTest {
 @Test public void displayPreservesAlreadyScaledValue()throws Exception{List<String> commands=new ArrayList<>();String result=new Protocol(c->{commands.add(c);return c+"\r\nOK\r\n123.4\r\n";}).display();assertEquals("123.4",result);assertEquals(Collections.singletonList("@D0"),commands);}
 @Test public void malformedDisplayRejected(){for(String data:new String[]{"NaN","1\r\n2","ERR","RPM 12"})try{new Protocol(c->c+"\r\n"+data+"\r\n").display();fail();}catch(IOException expected){}}
 @Test public void incompleteSnapshotCannotAuthorizeWrite(){try{new Protocol.Config("TYPE RPM SCALE 1.0000 DECPT 1 0SCAL 0.0000 FSCAL 200.000");fail();}catch(IOException expected){}}
 @Test public void eachRequiredFieldAndSectionChecked(){for(String key:new String[]{"CH_A","DAC1","TYPE","SCALE","INPUT","LOEND","GATE","DECPT","0SCAL","FSCAL","DIAG","OUTPT","DISPR","SERNO"})try{new Protocol.Config(SettingsTest.BASE.replaceFirst(key,"MISSING"));fail(key);}catch(IOException expected){}}
}

