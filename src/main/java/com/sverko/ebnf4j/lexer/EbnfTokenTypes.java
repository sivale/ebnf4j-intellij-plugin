package com.sverko.ebnf4j.lexer;

import com.intellij.psi.tree.IElementType;
import com.sverko.ebnf4j.EbnfLanguage;
import org.jetbrains.annotations.NonNls;

public interface EbnfTokenTypes {
  IElementType IDENTIFIER = type("IDENTIFIER");
  IElementType STRING = type("STRING");
  IElementType NUMBER = type("NUMBER");
  IElementType CHAR_CLASS = type("CHAR_CLASS"); // ?...?
  IElementType ASSIGN = type("ASSIGN");         // =
  IElementType COMMA = type("COMMA");           // ,
  IElementType PIPE = type("PIPE");             // |
  IElementType SEMICOLON = type("SEMICOLON");   // ;
  IElementType MINUS = type("MINUS");           // -
  IElementType STAR = type("STAR");             // *
  IElementType L_BRACE = type("L_BRACE");       // {
  IElementType R_BRACE = type("R_BRACE");       // }
  IElementType L_BRACKET = type("L_BRACKET");   // [
  IElementType R_BRACKET = type("R_BRACKET");   // ]
  IElementType L_PAREN = type("L_PAREN");       // (
  IElementType R_PAREN = type("R_PAREN");       // )
  IElementType WHITESPACE = type("WHITESPACE");
  IElementType BAD_CHAR = type("BAD_CHAR");

  static IElementType type(@NonNls String debugName) {
    return new IElementType(debugName, EbnfLanguage.INSTANCE);
  }
}
