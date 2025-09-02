package com.sverko.ebnf4j.highlighting;

import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import org.jetbrains.annotations.NotNull;

public class EbnfSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {
  @Override
  protected @NotNull SyntaxHighlighter createHighlighter() {
    return new EbnfSyntaxHighlighter();
  }
}
