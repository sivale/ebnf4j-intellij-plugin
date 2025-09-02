package com.sverko.ebnf4j;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.jetbrains.annotations.NotNull;

public class EbnfSpellcheckingStrategy extends SpellcheckingStrategy {
    
    private static final Logger LOG = Logger.getInstance(EbnfSpellcheckingStrategy.class);
    
    private static final Tokenizer<PsiElement> DISABLED_TOKENIZER = new Tokenizer<PsiElement>() {
        @Override
        public void tokenize(@NotNull PsiElement element, @NotNull TokenConsumer consumer) {
            LOG.info("EbnfSpellcheckingStrategy: tokenize called for element: " + element.getClass().getSimpleName());
        }
    };
    
    @Override
    public @NotNull Tokenizer<PsiElement> getTokenizer(PsiElement element) {
        LOG.info("EbnfSpellcheckingStrategy: getTokenizer called for element: " + element.getClass().getSimpleName());
        return DISABLED_TOKENIZER;
    }
}
