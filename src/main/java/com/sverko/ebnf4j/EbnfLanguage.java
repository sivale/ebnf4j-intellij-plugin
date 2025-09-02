package com.sverko.ebnf4j;
import com.intellij.lang.Language;


// Simple language identifier for EBNF
public class EbnfLanguage extends Language {
    public static final EbnfLanguage INSTANCE = new EbnfLanguage();
    private EbnfLanguage() {
        super("EBNF");
    }
}
