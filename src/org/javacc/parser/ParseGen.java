/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.javacc.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Generate the parser.
 */
public class ParseGen extends JavaCCGlobals implements JavaCCParserConstants {

  static public void start() throws MetaParseException {

    Token t = null;

    if (JavaCCErrors.get_error_count() != 0) throw new MetaParseException();

    if (Options.getBuildParser()) {

      try {
        ostr = new PrintWriter(
                  new BufferedWriter(
                     new FileWriter(
                       new File(Options.getOutputDirectory(), cu_name + ".java")
                     ),
                     8192
                  )
               );
      } catch (IOException e) {
        JavaCCErrors.semantic_error("Could not open file " + cu_name + ".java for writing.");
        throw new Error();
      }

      List tn = new ArrayList(toolNames);
      tn.add(toolName);
      ostr.println("/* " + getIdString(tn, cu_name + ".java") + " */");

      boolean implementsExists = false;

      if (cu_to_insertion_point_1.size() != 0) {
        boolean printImports = true;
        printTokenSetup((Token)(cu_to_insertion_point_1.get(0))); ccol = 1;
        for (Iterator it = cu_to_insertion_point_1.iterator(); it.hasNext();) {
          t = (Token)it.next();
          if (printImports && (t.kind == PUBLIC || t.kind == CLASS)) {
            printImports = false;
            ostr.println("");
            ostr.println("import com.intellij.psi.tree.IElementType;");
            ostr.println("import com.intellij.lang.PsiBuilder;");
            ostr.println("import java.util.ArrayList;");
          }
          if (t.kind == IMPLEMENTS) {
            implementsExists = true;
          } else if (t.kind == CLASS) {
            implementsExists = false;
          }
          printToken(t, ostr);
        }
      }
      if (implementsExists) {
        ostr.print(", ");
      } else {
        ostr.print(" implements ");
      }
      ostr.print(cu_name + "Constants ");
      if (cu_to_insertion_point_2.size() != 0) {
        printTokenSetup((Token)(cu_to_insertion_point_2.get(0)));
        for (Iterator it = cu_to_insertion_point_2.iterator(); it.hasNext();) {
          t = (Token)it.next();
          printToken(t, ostr);
        }
      }

      ostr.println("");
      ostr.println("");

      ParseEngine.build(ostr);

      ostr.println("  private final PsiBuilder builder;");
      ostr.println("  public " + cu_name + "(PsiBuilder builder) {");
      ostr.println("    this.builder = builder;");
      ostr.println("  }");

      if (jj2index != 0) {
        ostr.println("  " + staticOpt() + "private int jj_la;");
        ostr.println("  " + staticOpt() + "private ArrayList<IElementType> tokens = new ArrayList<IElementType>();");
        ostr.println("  " + staticOpt() + "private int currentIndex = 0;");
        ostr.println("  " + staticOpt() + "private int maxIndex = 0;");
        ostr.println("  " + staticOpt() + "private boolean reportEof = false;");
        if (lookaheadNeeded) {
          ostr.println("  /** Whether we are looking ahead. */");
          ostr.println("  " + staticOpt() + "private boolean jj_lookingAhead = false;");
          ostr.println("  " + staticOpt() + "private boolean jj_semLA;");
        }
      }
      ostr.println("");

      ostr.println("  " + staticOpt() + "private void rollbackTo(int scanpos) {");
      ostr.println("    currentIndex = scanpos;");
      ostr.println("  }");
      ostr.println("  " + staticOpt() + "private void init(int la) {");
      ostr.println("    jj_la = la;");
      ostr.println("    tokens.clear();");
      ostr.println("    tokens.add(builder.getTokenType());");
      ostr.println("    currentIndex = 0;");
      ostr.println("    maxIndex = 0;");
      ostr.println("  }");
      ostr.println("  " + staticOpt() + "private IElementType jj_consume_token(IElementType type) {");
      ostr.println("    IElementType actualType = builder.getTokenType();");
      ostr.println("    if (actualType == type) {");
      ostr.println("      builder.advanceLexer();");
      ostr.println("    } else {");
      if (Options.getAutomaticErrorRecovery()) {
        ostr.println("      if (builder.eof()) {");
        ostr.println("        if (!reportEof) {");
        ostr.println("          reportEof = true;");
        ostr.println("          builder.error(\"Unexpected end of file\");");
        ostr.println("        }");
        ostr.println("      } else {");
        ostr.println("        PsiBuilder.Marker errorMarker = builder.mark();");
        ostr.println("        String text = builder.getTokenText();");
        ostr.println("        builder.advanceLexer();");
        ostr.println("        errorMarker.error(\"Expected \" + type + \", but get: \" + text);");
        ostr.println("      }");
      } else {
	ostr.println("      throw new ParseException();");
      }
      ostr.println("    }");
      ostr.println("    return type;");

      ostr.println("  }");
      ostr.println("");
      if (jj2index != 0) {
        ostr.println("  static private final class LookaheadSuccess extends java.lang.Error { }");
        ostr.println("  " + staticOpt() + "final private LookaheadSuccess jj_ls = new LookaheadSuccess();");
        ostr.println("  " + staticOpt() + "private void jj_on_la1() {");
        ostr.println("    advanceLexer();");
        ostr.println("    jj_test_jj_la();");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private void jj_test_jj_la() {");
        ostr.println("    if (jj_la == 0 && maxIndex == currentIndex)");
        ostr.println("       throw jj_ls;");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private IElementType advanceLexer() {");
        ostr.println("    if (maxIndex == currentIndex) {");
	ostr.println("      IElementType result = tokens.get(currentIndex);");
        ostr.println("      builder.advanceLexer();");
        ostr.println("      tokens.add(builder.getTokenType());");
        ostr.println("      maxIndex++;");
        ostr.println("      currentIndex++;");
        ostr.println("      jj_la--;");
        ostr.println("      return result;");
        ostr.println("    }");
        ostr.println("    return tokens.get(currentIndex++);");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private boolean jj_scan_token(IElementType kind) {");
        ostr.println("    IElementType nextType = advanceLexer();");
        ostr.println("    if (nextType != kind) return true;");
        ostr.println("    jj_test_jj_la();");
        ostr.println("    return false;");
        ostr.println("  }");
        ostr.println("");
      }
      ostr.println("");
      ostr.println("/** Get the next Token. Use getNextTokenType instead. ");
      ostr.println("  " + staticOpt() + "final public Token getNextToken() {");
      if (Options.getCacheTokens()) {
        ostr.println("    if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
        ostr.println("    else jj_nt = jj_nt.next = token_source.getNextToken();");
      } else {
        ostr.println("    if (token.next != null) token = token.next;");
        ostr.println("    else token = token.next = token_source.getNextToken();");
        ostr.println("    jj_ntk = -1;");
      }
      if (Options.getErrorReporting()) {
        ostr.println("    jj_gen++;");
      }
      if (Options.getDebugParser()) {
        ostr.println("      trace_token(token, \" (in getNextToken)\");");
      }
      ostr.println("    return token;");
      ostr.println("  } */");
      ostr.println("");
      ostr.println("/** Get the specific Token. */");
      ostr.println("  " + staticOpt() + "final public IElementType getTokenType(int index) {");
      if (lookaheadNeeded) {
//        ostr.println("    Token t = jj_lookingAhead ? jj_scanpos : token;");
      } else {
        ostr.println("    IElementType t = null;");
      }
      ostr.println("    for (int i = 0; i < index; i++) {");
      ostr.println("      t = builder.getTokenType();");
      ostr.println("    }");
      ostr.println("    return t;");
      ostr.println("  }");
      ostr.println("");
      ostr.println("    private IElementType getType() {");
      ostr.println("      return builder.getTokenType();");
      ostr.println("    }");
      ostr.println("");

      if (Options.getDebugParser()) {
        ostr.println("  " + staticOpt() + "private int trace_indent = 0;");
        ostr.println("  " + staticOpt() + "private boolean trace_enabled = true;");
        ostr.println("");
        ostr.println("/** Enable tracing. */");
        ostr.println("  " + staticOpt() + "final public void enable_tracing() {");
        ostr.println("    trace_enabled = true;");
        ostr.println("  }");
        ostr.println("");
        ostr.println("/** Disable tracing. */");
        ostr.println("  " + staticOpt() + "final public void disable_tracing() {");
        ostr.println("    trace_enabled = false;");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private void trace_call(String s) {");
        ostr.println("    if (trace_enabled) {");
        ostr.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        ostr.println("      System.out.println(\"Call:   \" + s);");
        ostr.println("    }");
        ostr.println("    trace_indent = trace_indent + 2;");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private void trace_return(String s) {");
        ostr.println("    trace_indent = trace_indent - 2;");
        ostr.println("    if (trace_enabled) {");
        ostr.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        ostr.println("      System.out.println(\"Return: \" + s);");
        ostr.println("    }");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private void trace_token(Token t, String where) {");
        ostr.println("    if (trace_enabled) {");
        ostr.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        ostr.println("      System.out.print(\"Consumed token: <\" + tokenImage[t.kind]);");
        ostr.println("      if (t.kind != 0 && !tokenImage[t.kind].equals(\"\\\"\" + t.image + \"\\\"\")) {");
        ostr.println("        System.out.print(\": \\\"\" + t.image + \"\\\"\");");
        ostr.println("      }");
        ostr.println("      System.out.println(\" at line \" + t.beginLine + " +
                "\" column \" + t.beginColumn + \">\" + where);");
        ostr.println("    }");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  " + staticOpt() + "private void trace_scan(Token t1, int t2) {");
        ostr.println("    if (trace_enabled) {");
        ostr.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
        ostr.println("      System.out.print(\"Visited token: <\" + tokenImage[t1.kind]);");
        ostr.println("      if (t1.kind != 0 && !tokenImage[t1.kind].equals(\"\\\"\" + t1.image + \"\\\"\")) {");
        ostr.println("        System.out.print(\": \\\"\" + t1.image + \"\\\"\");");
        ostr.println("      }");
        ostr.println("      System.out.println(\" at line \" + t1.beginLine + \"" +
                " column \" + t1.beginColumn + \">; Expected token: <\" + tokenImage[t2] + \">\");");
        ostr.println("    }");
        ostr.println("  }");
        ostr.println("");
      } else {
        ostr.println("  /** Enable tracing. */");
        ostr.println("  " + staticOpt() + "final public void enable_tracing() {");
        ostr.println("  }");
        ostr.println("");
        ostr.println("  /** Disable tracing. */");
        ostr.println("  " + staticOpt() + "final public void disable_tracing() {");
        ostr.println("  }");
        ostr.println("");
      }


      if (cu_from_insertion_point_2.size() != 0) {
        printTokenSetup((Token)(cu_from_insertion_point_2.get(0))); ccol = 1;
        for (Iterator it = cu_from_insertion_point_2.iterator(); it.hasNext();) {
          t = (Token)it.next();
          printToken(t, ostr);
        }
        printTrailingComments(t, ostr);
      }
      ostr.println("");

      ostr.close();

    } // matches "if (Options.getBuildParser())"

  }

  static private PrintWriter ostr;

   public static void reInit()
   {
      ostr = null;
      lookaheadNeeded = false;
   }

}
