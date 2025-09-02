package com.sverko.ebnf4j.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class EbnfPsiElement extends ASTWrapperPsiElement {
  public EbnfPsiElement(@NotNull ASTNode node) {
    super(node);
  }
}
