package com.sverko.ebnf4j.highlighting;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import java.awt.Color;
import java.awt.Font;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.sverko.ebnf4j.lexer.EbnfLexer;
import com.sverko.ebnf4j.lexer.EbnfTokenTypes;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

import java.util.HashMap;
import java.util.Map;


public class EbnfSyntaxHighlighter extends SyntaxHighlighterBase implements EbnfTokenTypes {

  private static final Logger LOG = Logger.getInstance(EbnfSyntaxHighlighter.class);
  private static final Map<IElementType, TextAttributesKey> KEYS = new HashMap<>();

  public static final TextAttributesKey IDENTIFIER_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_IDENTIFIER",
          new TextAttributes(new Color(0xAA78E3), null, null, null, Font.PLAIN));

  public static final TextAttributesKey NUMBER_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_NUMBER",
          new TextAttributes(new Color(0x12B4B4), null, null, null, Font.PLAIN));

  public static final TextAttributesKey CHAR_CLASS_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_CHAR_CLASS",
          new TextAttributes(new Color(0x6AAB73), null, null, null, Font.ITALIC));

  public static final TextAttributesKey STRING_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_STRING",
          new TextAttributes(new Color(0x6AAB73), null, null, null, Font.PLAIN));

  public static final TextAttributesKey OPERATOR_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_OPERATOR",
          new TextAttributes(new Color(0xF87220), null, null, null, Font.BOLD));

  public static final TextAttributesKey BRACES_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_BRACES",
          new TextAttributes(new Color(0xF87220), null, null, null, Font.PLAIN));

  public static final TextAttributesKey BRACKETS_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_BRACKETS",
          new TextAttributes(new Color(0xF87220), null, null, null, Font.PLAIN));

  public static final TextAttributesKey PARENTHESES_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_PARENTHESES",
          new TextAttributes(new Color(0xF87220), null, null, null, Font.PLAIN));

  public static final TextAttributesKey COMMA_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_COMMA",
          new TextAttributes(new Color(0xF87220), null, null, null, Font.PLAIN));

  public static final TextAttributesKey BAD_CHAR_KEY =
      TextAttributesKey.createTextAttributesKey("EBNF_BAD_CHAR",
          new TextAttributes(new Color(0xFF0000), null, null, null, Font.PLAIN));

  static {
    KEYS.put(IDENTIFIER, IDENTIFIER_KEY);
    KEYS.put(NUMBER, NUMBER_KEY);
    KEYS.put(CHAR_CLASS, CHAR_CLASS_KEY);
    KEYS.put(STRING, STRING_KEY);
    KEYS.put(ASSIGN, OPERATOR_KEY);
    KEYS.put(PIPE, OPERATOR_KEY);
    KEYS.put(MINUS, OPERATOR_KEY);
    KEYS.put(STAR, OPERATOR_KEY);
    KEYS.put(L_BRACE, BRACES_KEY);
    KEYS.put(R_BRACE, BRACES_KEY);
    KEYS.put(L_BRACKET, BRACKETS_KEY);
    KEYS.put(R_BRACKET, BRACKETS_KEY);
    KEYS.put(L_PAREN, PARENTHESES_KEY);
    KEYS.put(R_PAREN, PARENTHESES_KEY);
    KEYS.put(COMMA, COMMA_KEY);
    KEYS.put(SEMICOLON, COMMA_KEY);
    KEYS.put(BAD_CHAR, BAD_CHAR_KEY);
    KEYS.put(TokenType.BAD_CHARACTER, BAD_CHAR_KEY);
  }

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    LOG.info("EbnfSyntaxHighlighter: getHighlightingLexer called");
    return new EbnfLexer();
  }

  @Override
  public @NotNull TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    TextAttributesKey key = KEYS.get(tokenType);
    if (key == null) {
      return TextAttributesKey.EMPTY_ARRAY;
    }
    return new TextAttributesKey[]{key};
  }
}