package com.sverko.ebnf4j.highlighting;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class EbnfColorSettingsPage implements ColorSettingsPage {

  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[] {
      new AttributesDescriptor("Identifier", EbnfSyntaxHighlighter.IDENTIFIER_KEY),
      new AttributesDescriptor("Number", EbnfSyntaxHighlighter.NUMBER_KEY),
      new AttributesDescriptor("Char class (?…?)", EbnfSyntaxHighlighter.CHAR_CLASS_KEY),
      new AttributesDescriptor("Operator (=, |, -, *)", EbnfSyntaxHighlighter.OPERATOR_KEY),
      new AttributesDescriptor("Braces { }", EbnfSyntaxHighlighter.BRACES_KEY),
      new AttributesDescriptor("Brackets [ ]", EbnfSyntaxHighlighter.BRACKETS_KEY),
      new AttributesDescriptor("Parentheses ( )", EbnfSyntaxHighlighter.PARENTHESES_KEY),
      new AttributesDescriptor("Comma / Semicolon", EbnfSyntaxHighlighter.COMMA_KEY),
      new AttributesDescriptor("Bad character", EbnfSyntaxHighlighter.BAD_CHAR_KEY)
  };

  @Override
  public @Nullable Icon getIcon() {
    return null;
  }

  @Override
  public @NotNull SyntaxHighlighter getHighlighter() {
    return new EbnfSyntaxHighlighter();
  }

  @Override
  public @NotNull String getDemoText() {
    return """
        OUTPUT = {KENNZEICHEN | NICHT_KENNZEICHEN};
        KENNZEICHEN = STADT_KUERZEL, "-", ERKENNUNGS_NUMMER, ZAHLEN;
        NICHT_KENNZEICHEN = ?BMP? - KENNZEICHEN;
        STADT_KUERZEL = BUCHSTABE, 2*[BUCHSTABE];
        ERKENNUNGS_NUMMER = BUCHSTABE, 2*[BUCHSTABE];
        BUCHSTABE = ?GERMAN_CAPITALS?;
        ZAHLEN = ZAHL, 3*[ZAHL];
        ZAHL = ?DIGIT?;
        """;
  }

  @Override
  public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @Override
  public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @Override
  public ColorDescriptor @NotNull [] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  public @NotNull String getDisplayName() {
    return "EBNF4J";
  }
}