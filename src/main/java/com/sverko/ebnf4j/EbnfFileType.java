package com.sverko.ebnf4j;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EbnfFileType extends LanguageFileType {
  public static final EbnfFileType INSTANCE = new EbnfFileType();

  private EbnfFileType() {
    super(EbnfLanguage.INSTANCE);
  }

  @Override
  public @NonNls @NotNull String getName() {
    return "EBNF file";
  }

  @Override
  public @NotNull String getDescription() {
    return "EBNF grammar file";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "ebnf";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return EbnfIcons.FILE;
  }
}
