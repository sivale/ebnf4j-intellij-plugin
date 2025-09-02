package com.sverko.ebnf4j.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.sverko.ebnf4j.EbnfLanguage;
import com.sverko.ebnf4j.lexer.EbnfLexer;
import com.sverko.ebnf4j.lexer.EbnfTokenTypes;
import com.sverko.ebnf4j.psi.EbnfElementTypes;
import com.sverko.ebnf4j.psi.EbnfFile;
import com.sverko.ebnf4j.psi.EbnfPsiElement;
import org.jetbrains.annotations.NotNull;

public class EbnfParserDefinition implements ParserDefinition {

  public static final TokenSet WHITE_SPACES = TokenSet.create(EbnfTokenTypes.WHITESPACE);
  public static final TokenSet COMMENTS = TokenSet.EMPTY;
  public static final TokenSet STRINGS = TokenSet.create(EbnfTokenTypes.STRING);

  public static final IFileElementType FILE = new IFileElementType(EbnfLanguage.INSTANCE);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new EbnfLexer();
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new EbnfPsiParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public @NotNull TokenSet getWhitespaceTokens() {
    return WHITE_SPACES;
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return STRINGS;
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return new EbnfPsiElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new EbnfFile(viewProvider);
  }
}