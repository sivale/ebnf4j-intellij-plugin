package com.sverko.ebnf4j.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EbnfLexer extends LexerBase implements EbnfTokenTypes {
  private CharSequence buffer;
  private int endOffset;
  private int tokenStart;
  private int tokenEnd;
  private IElementType tokenType;
  private int state;

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    this.buffer = buffer;
    this.endOffset = endOffset;
    this.tokenStart = startOffset;
    this.tokenEnd = startOffset;
    this.tokenType = null;
    this.state = initialState;
    advance();
  }

  @Override
  public int getState() {
    return state;
  }

  @Override
  public @Nullable IElementType getTokenType() {
    return tokenType;
  }

  @Override
  public int getTokenStart() {
    return tokenStart;
  }

  @Override
  public int getTokenEnd() {
    return tokenEnd;
  }

  @Override
  public void advance() {
    if (tokenEnd >= endOffset) {
      tokenType = null;
      return;
    }

    tokenStart = tokenEnd;
    char c = buffer.charAt(tokenStart);

    // Whitespace
    if (Character.isWhitespace(c)) {
      int i = tokenStart + 1;
      while (i < endOffset && Character.isWhitespace(buffer.charAt(i))) i++;
      tokenEnd = i;
      tokenType = WHITESPACE;
      return;
    }

    // Operators / delimiters
    switch (c) {
      case '=': tokenEnd = tokenStart + 1; tokenType = ASSIGN; return;
      case ',': tokenEnd = tokenStart + 1; tokenType = COMMA; return;
      case '|': tokenEnd = tokenStart + 1; tokenType = PIPE; return;
      case ';': tokenEnd = tokenStart + 1; tokenType = SEMICOLON; return;
      case '-': tokenEnd = tokenStart + 1; tokenType = MINUS; return;
      case '*': tokenEnd = tokenStart + 1; tokenType = STAR; return;
      case '{': tokenEnd = tokenStart + 1; tokenType = L_BRACE; return;
      case '}': tokenEnd = tokenStart + 1; tokenType = R_BRACE; return;
      case '[': tokenEnd = tokenStart + 1; tokenType = L_BRACKET; return;
      case ']': tokenEnd = tokenStart + 1; tokenType = R_BRACKET; return;
      case '(': tokenEnd = tokenStart + 1; tokenType = L_PAREN; return;
      case ')': tokenEnd = tokenStart + 1; tokenType = R_PAREN; return;
      case '"': {
        scanQuotedString('"');
        return;
      }
      case '\'': {
        scanQuotedString('\'');
        return;
      }
      case '?':
        // ?...?-style char class
        int i = tokenStart + 1;
        while (i < endOffset && buffer.charAt(i) != '?') i++;
        if (i < endOffset && buffer.charAt(i) == '?') {
          tokenEnd = i + 1;
          tokenType = CHAR_CLASS;
          return;
        }
        // Unclosed ?... treat as bad
        tokenEnd = Math.min(tokenStart + 1, endOffset);
        tokenType = BAD_CHAR;
        return;
      default:
        // Identifier (A-Z_ and digits), or number
        if (Character.isDigit(c)) {
          int j = tokenStart + 1;
          while (j < endOffset && Character.isDigit(buffer.charAt(j))) j++;
          tokenEnd = j;
          tokenType = NUMBER;
          return;
        }
        if (Character.isLetter(c) || c == '_') {
          int j = tokenStart + 1;
          while (j < endOffset) {
            char cj = buffer.charAt(j);
            if (Character.isLetterOrDigit(cj) || cj == '_') j++;
            else break;
          }
          tokenEnd = j;
          tokenType = IDENTIFIER;
          return;
        }
        tokenEnd = tokenStart + 1;
        tokenType = BAD_CHAR;
    }
  }

  private void scanQuotedString(char quote) {
    int i = tokenStart + 1;
    boolean closed = false;
    while (i < endOffset) {
      char ch = buffer.charAt(i);
      // stop only at the matching quote; the opposite quote is part of the content
      if (ch == quote) {
        i++;
        closed = true;
        break;
      }
      i++;
    }
    tokenEnd = i;
    tokenType = closed ? STRING : BAD_CHAR;  // Use BAD_CHAR consistently instead of TokenType.BAD_CHARACTER
  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return buffer;
  }

  @Override
  public int getBufferEnd() {
    return endOffset;
  }
}