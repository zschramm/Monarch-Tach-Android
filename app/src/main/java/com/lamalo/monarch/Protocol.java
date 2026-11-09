// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Monarch Utility contributors
package com.lamalo.monarch;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.*;

/** Allowlisted commands only. Never retry or roll back an instrument write. */
public final class Protocol {
 public interface Link { String exchange(String command) throws IOException; }
 private final Link link;
 public Protocol(Link link) { this.link=link; }
 public static String value(String input) {
  if (!input.matches("[0-9]{1,5}(\\.[0-9]{1,4})?")) throw new IllegalArgumentException("Use 0-99999 with at most four decimals.");
  BigDecimal n=new BigDecimal(input);
  if(n.compareTo(new BigDecimal("99999"))>0) throw new IllegalArgumentException("Maximum supported by this app is 99999.");
  return n.stripTrailingZeros().toPlainString();
 }
 public static String pprScale(String input) {
  if(!input.matches("[0-9]{1,3}")||Integer.parseInt(input)<1)throw new IllegalArgumentException("Pulses per revolution must be an integer 1-999.");
  try{return new BigDecimal("60").divide(new BigDecimal(input),4,RoundingMode.UNNECESSARY).stripTrailingZeros().toPlainString();}
  catch(ArithmeticException e){throw new IllegalArgumentException("This PPR requires a rounded scale factor. Disabled until instrument precision is verified; no setting changed.");}
 }
 public static List<String> parse(String command,String raw) throws IOException {
  List<String> lines=new ArrayList<>();boolean echo=false;
  for(String s:raw.split("[\\r\\n]+")){s=s.trim();if(s.isEmpty())continue;
   if(s.equals(command)){if(echo||!lines.isEmpty())throw new IOException("Unexpected command echo");echo=true;continue;}
   if(s.startsWith("@"))throw new IOException("Unexpected command in reply");
   if(s.startsWith("ERR"))throw new IOException("Device rejected command: "+s);lines.add(s);
  }
  if(lines.isEmpty())throw new IOException("Missing reply");return lines;
 }
 List<String> ask(String cmd)throws IOException{return parse(cmd,link.exchange(cmd));}
 public String identify()throws IOException{List<String> p=ask("@PI");if(!p.contains("ACT-1B"))throw new IOException("Only ACT-1B is supported");return String.join(" / ",p);}
 public String display()throws IOException{List<String> p=ask("@D0");p.removeIf(s->s.equals("OK"));if(p.size()!=1||!p.get(0).matches("[+-]?[0-9]+(\\.[0-9]+)?"))throw new IOException("Unrecognized remote display response");return p.get(0);}
 public String settings()throws IOException{return String.join("\n",ask("@C2"));}
 public static final class Config {
  public final String text;
  public Config(String raw)throws IOException{text=raw.trim().replaceAll("\\s+"," ").replaceFirst("^OK ","").replaceAll("\\s+=\\s+", " ");for(String section:new String[]{"CH_A","DAC1"}){Matcher m=Pattern.compile("(?<!\\S)"+section+"(?!\\S)").matcher(text);if(!m.find()||m.find())throw new IOException("Missing or ambiguous section "+section);}for(String field:new String[]{"TYPE","SCALE","INPUT","LOEND","GATE","DECPT","0SCAL","FSCAL","DIAG","OUTPT","DISPR","SERNO"})get(field);}
  private Pattern pattern(String key){return Pattern.compile((key.equals("SCALE")?"(?<!TYPE )":"")+"(?<!\\S)"+Pattern.quote(key)+"\\s+(\\S+)");}
  public String get(String key)throws IOException{Matcher m=pattern(key).matcher(text);if(!m.find())throw new IOException("Missing setting "+key);String v=m.group(1);if(v.equals("="))throw new IOException("Missing value for "+key);if(m.find())throw new IOException("Ambiguous setting "+key);return v;}
  private String masked(Set<String> keys)throws IOException{String s=text;for(String key:keys){get(key);s=pattern(key).matcher(s).replaceAll(key+" <changed>");}return s;}
  public void unchangedExcept(Config after,String... keys)throws IOException{Set<String> allowed=new HashSet<>(Arrays.asList(keys));if(!masked(allowed).equals(after.masked(allowed)))throw new IOException("Unrelated settings changed; inspect current configuration. No rollback attempted.");}
  public void matches(Config other)throws IOException{unchangedExcept(other);}
  public void expect(String key,String expected)throws IOException{String actual=get(key);try{if(new BigDecimal(actual).compareTo(new BigDecimal(expected))==0)return;}catch(NumberFormatException ignored){}if(!actual.equals(expected))throw new IOException(key+" readback differs: "+actual);}
 }
 public Config config()throws IOException{return new Config(settings());}
 public BigDecimal read(String key)throws IOException{key(key);List<String> p=ask("@DAC1/"+key);p.removeIf(s->s.equals("OK"));if(p.size()!=1||!p.get(0).matches("[+-]?[0-9]+(\\.[0-9]+)?"))throw new IOException("Unrecognized scale response");return new BigDecimal(p.get(0));}
 private void ack(String command)throws IOException{List<String> p=ask(command);if(p.size()!=1||!p.get(0).equals("OK"))throw new IOException("Write acknowledgement uncertain");}
 public void write(String key,String input)throws IOException{key(key);String v=value(input);ack("@DAC1/"+key+" = "+v);if(read(key).compareTo(new BigDecimal(v))!=0)throw new IOException("Readback differs; do not resend automatically");}
 public void applyScale(Config reviewed,String key,String input)throws IOException{key(key);String v=value(input);Config before=config();reviewed.matches(before);BigDecimal proposed=new BigDecimal(v);if(key.equals("FSCAL")?proposed.compareTo(new BigDecimal(before.get("0SCAL")))<=0:proposed.compareTo(new BigDecimal(before.get("FSCAL")))>=0)throw new IOException("Require 0SCAL < FSCAL");write(key,v);Config after=config();before.unchangedExcept(after,key);after.expect(key,v);}
 public void applyDecimal(Config reviewed,String choice)throws IOException{if(!Arrays.asList("NONE","1","2","3").contains(choice))throw new IllegalArgumentException("Unsupported decimal choice");Config before=config();reviewed.matches(before);ack("@DECPT = "+choice);Config after=config();before.unchangedExcept(after,"DECPT");after.expect("DECPT",choice);}
 public void applyMode(Config reviewed,String mode)throws IOException{if(!Arrays.asList("RPM","FREQ").contains(mode))throw new IllegalArgumentException("Unsupported mode");Config before=config();reviewed.matches(before);ack("@CH_A/TYPE = "+mode);Config after=config();before.unchangedExcept(after,"TYPE","SCALE");after.expect("TYPE",mode);/* Firmware's C2 SCALE representation in RPM differs from the documented effective multiplier. Do not invent a raw comparison. */}
 public void applyPpr(Config reviewed,String input)throws IOException{String scale=pprScale(input);Config before=config();reviewed.matches(before);if(!before.get("TYPE").equals("SCALE")){ack("@CH_A/TYPE = SCALE");Config intermediate=config();before.unchangedExcept(intermediate,"TYPE","SCALE");intermediate.expect("TYPE","SCALE");before=intermediate;}ack("@CH_A/TYPE/SCALE = "+scale);Config after=config();before.unchangedExcept(after,"SCALE");after.expect("TYPE","SCALE");after.expect("SCALE",scale);}
 private static void key(String key){if(!key.equals("0SCAL")&&!key.equals("FSCAL"))throw new IllegalArgumentException("Unsupported setting");}
}



