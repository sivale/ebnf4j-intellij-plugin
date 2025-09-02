package com.sverko.ebnf4j.psi;

import com.intellij.psi.tree.IElementType;
import com.sverko.ebnf4j.EbnfLanguage;

public interface EbnfElementTypes {
  IElementType FILE = new IElementType("EBNF_FILE", EbnfLanguage.INSTANCE);
  IElementType RULE = new IElementType("EBNF_RULE", EbnfLanguage.INSTANCE);
}
