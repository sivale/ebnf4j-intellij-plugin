package com.sverko.ebnf4j.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class EbnfPsiParser implements PsiParser {

  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker rootMarker = builder.mark();

    while (!builder.eof()) {
      builder.advanceLexer();
    }

    rootMarker.done(root);
    return builder.getTreeBuilt();
  }
}
