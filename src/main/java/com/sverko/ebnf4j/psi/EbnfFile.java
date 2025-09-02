package com.sverko.ebnf4j.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.sverko.ebnf4j.EbnfFileType;
import com.sverko.ebnf4j.EbnfLanguage;
import org.jetbrains.annotations.NotNull;

public class EbnfFile extends PsiFileBase {
  public EbnfFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, EbnfLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return EbnfFileType.INSTANCE;
  }
}