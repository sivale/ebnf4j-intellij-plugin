package com.sverko.ebnf4j.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

class EbnfDefinitionInsertHandler implements InsertHandler<LookupElement> {
  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = editor.getDocument();
    int offset = context.getTailOffset();

    // Add " = ;" template after the identifier
    document.insertString(offset, " = ;");

    // Position cursor between = and ;
    editor.getCaretModel().moveToOffset(offset + 3);
  }
}

// Handler for new rule definitions
class EbnfNewRuleInsertHandler implements InsertHandler<LookupElement> {
  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    Document document = editor.getDocument();
    int startOffset = context.getStartOffset();
    int endOffset = context.getTailOffset();

    // Replace "NEW_RULE" with template
    document.replaceString(startOffset, endOffset, "NEW_RULE = ;");

    // Select "NEW_RULE" so user can type over it
    editor.getSelectionModel().setSelection(startOffset, startOffset + 8);
  }
}
