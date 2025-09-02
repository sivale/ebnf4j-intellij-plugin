package com.sverko.ebnf4j.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.sverko.ebnf.EbnfParserGenerator;
import com.sverko.ebnf.Lexer;
import com.sverko.ebnf.Parser;
import com.sverko.ebnf4j.EbnfFileType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class EbnfLiveTesterPanel implements Disposable {

  private final Project project;
  private final SimpleToolWindowPanel mainPanel;

  private final JPanel centerPanel;
  private final Map<VirtualFile, Editor> testEditorsBySchema = new HashMap<>();
  private @Nullable Editor currentTestEditor;

  private @Nullable VirtualFile currentSchemaFile;
  private @Nullable Document currentSchemaDoc;

  private @Nullable DocumentListener schemaDocListener;
  private @Nullable DocumentListener testDocListener;

  // Highlighting
  private static final TextAttributes MATCH_ATTRIBUTES = new TextAttributes(
      Color.BLACK, new Color(144, 238, 144), null, null, Font.PLAIN); // hellgrün
  private static final TextAttributes ERROR_ATTRIBUTES = new TextAttributes(
      Color.WHITE, new Color(255, 182, 193), null, null, Font.PLAIN); // hellrot

  public EbnfLiveTesterPanel(Project project) {
    this.project = project;


    mainPanel = new SimpleToolWindowPanel(true, true);
    JPanel root = new JPanel(new BorderLayout());
    root.setBorder(BorderFactory.createEmptyBorder());

    JPanel leftStripe = new JPanel(new BorderLayout());
    leftStripe.setPreferredSize(new Dimension(JBUI.scale(10), 0));
    Color stripe = JBColor.namedColor("Separator.separatorColor", JBColor.border());
    leftStripe.setBackground(stripe);
    JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
    sep.setForeground(JBColor.border());
    sep.setOpaque(false);
    leftStripe.add(sep, BorderLayout.EAST);
    root.add(leftStripe, BorderLayout.WEST);

    centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBorder(BorderFactory.createEmptyBorder());
    centerPanel.add(placeholder("Öffne eine .ebnf-Datei – hier erscheint dann dein zugehöriger Testtext."),
        BorderLayout.CENTER);

    root.add(centerPanel, BorderLayout.CENTER);
    mainPanel.setContent(root);

    project.getMessageBus().connect(this).subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        new FileEditorManagerListener() {
          @Override public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            attachSchemaFileAsync(event.getNewFile());
          }
        });

    VirtualFile[] selected = FileEditorManager.getInstance(project).getSelectedFiles();
    attachSchemaFileAsync(selected.length > 0 ? selected[0] : null);
  }

  private static JComponent placeholder(String text) {
    JLabel lbl = new JLabel(text);
    lbl.setHorizontalAlignment(SwingConstants.LEFT);
    lbl.setVerticalAlignment(SwingConstants.TOP);
    JPanel p = new JPanel(new BorderLayout());
    p.setBorder(JBUI.Borders.empty(8));
    p.add(lbl, BorderLayout.NORTH);
    return p;
  }

  private void attachSchemaFileAsync(@Nullable VirtualFile vf) {
    ApplicationManager.getApplication().invokeLater(() -> attachSchemaFile(vf));
  }

  private void attachSchemaFile(@Nullable VirtualFile vf) {
    if (vf == null || !(vf.getFileType() instanceof EbnfFileType)) {
      detachSchemaListener();
      detachTestListener();
      currentSchemaFile = null;
      currentSchemaDoc = null;
      updateToolWindowTitle(null);
      swapRightEditor(null);
      clearHighlighting();
      return;
    }
    if (vf.equals(currentSchemaFile)) return;

    currentSchemaFile = vf;

    Document schemaDoc = FileDocumentManager.getInstance().getDocument(vf);
    if (schemaDoc == null) return;

    detachSchemaListener();
    currentSchemaDoc = schemaDoc;

    updateToolWindowTitle(vf.getName());

    schemaDocListener = new DocumentListener() {
      @Override public void documentChanged(@NotNull DocumentEvent event) { updateHighlighting(); }
    };
    currentSchemaDoc.addDocumentListener(schemaDocListener);

    // Test-Editor für dieses Schema holen/erstellen
    Editor testEditor = testEditorsBySchema.computeIfAbsent(vf, file -> {
      Document d = EditorFactory.getInstance().createDocument("");
      return EditorFactory.getInstance().createEditor(
          d, project, com.intellij.openapi.fileTypes.PlainTextFileType.INSTANCE, false);
    });
    swapRightEditor(testEditor);

    updateHighlighting();
  }

  private void updateToolWindowTitle(@Nullable String fileName) {
    ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("EBNF Live Tester");
    if (tw == null) return;
    try { tw.setTitle(fileName != null ? fileName : ""); } catch (Throwable ignored) {}
  }

  // -------- Test-Editor swappen & Listener managen --------
  private void swapRightEditor(@Nullable Editor newEditor) {
    detachTestListener();

    centerPanel.removeAll();
    currentTestEditor = newEditor;

    if (newEditor != null) {
      JComponent comp = newEditor.getComponent();
      comp.setBorder(BorderFactory.createEmptyBorder());
      centerPanel.add(comp, BorderLayout.CENTER);

      testDocListener = new DocumentListener() {
        @Override public void documentChanged(@NotNull DocumentEvent event) { updateHighlighting(); }
      };
      newEditor.getDocument().addDocumentListener(testDocListener);
    } else {
      centerPanel.add(placeholder("Öffne eine .ebnf-Datei – hier erscheint dann dein zugehöriger Testtext."),
          BorderLayout.CENTER);
    }

    centerPanel.revalidate();
    centerPanel.repaint();
  }

  private void detachSchemaListener() {
    if (currentSchemaDoc != null && schemaDocListener != null) {
      currentSchemaDoc.removeDocumentListener(schemaDocListener);
    }
    schemaDocListener = null;
  }

  private void detachTestListener() {
    if (currentTestEditor != null && testDocListener != null) {
      currentTestEditor.getDocument().removeDocumentListener(testDocListener);
    }
    testDocListener = null;
  }

  // -------- Parsing & Highlight --------
  private void updateHighlighting() {
    if (!ApplicationManager.getApplication().isDispatchThread()) {
      ApplicationManager.getApplication().invokeLater(this::updateHighlighting);
      return;
    }
    try {
      String schema = (currentSchemaDoc != null) ? currentSchemaDoc.getText() : "";
      String testText = (currentTestEditor != null) ? currentTestEditor.getDocument().getText() : "";

      if (schema.trim().isEmpty() || testText.trim().isEmpty()) {
        clearHighlighting();
        return;
      }

      List<MatchResult> matches = parseAndMatch(schema, testText);
      applyHighlighting(matches);

    } catch (Exception e) {
      System.err.println("Error in live updating: " + e.getMessage());
      clearHighlighting();
    }
  }

  private List<MatchResult> parseAndMatch(String schema, String testText) {
    List<MatchResult> results = new ArrayList<>();
    try {
      Lexer schemaLexer = Lexer.builder()
          .ignoreWhitespace(true)
          .preserveWhitespaceInQuotes(true)
          .build();
      List<String> schemaTokens = schemaLexer.lexText(schema);

      EbnfParserGenerator generator = new EbnfParserGenerator();
      Parser parser = generator.getParser(schemaTokens, true);

      int parseResult = parser.parse(testText);

      List<String> testTokens = parser.getLexer().lexText(testText);

      int matchedChars = offsetByScanningOriginalText(testText, testTokens, parseResult);

      if (parseResult > 0) {
        results.add(new MatchResult(0, Math.min(matchedChars, testText.length()), true));
      }
      if (matchedChars < testText.length() && parseResult >= 0) {
        results.add(new MatchResult(Math.max(0, matchedChars), testText.length(), false));
      }
    } catch (Exception e) {
      System.err.println("Error parsing: " + e.getMessage());
      results.clear();
      results.add(new MatchResult(0, testText.length(), false));
    }
    return results;
  }

  private int offsetByScanningOriginalText(String text, List<String> tokens, int parseResult) {
    if (parseResult <= 0 || text == null || text.isEmpty() || tokens == null || tokens.isEmpty()) {
      return 0;
    }
    int i = 0;
    int matched = 0;
    int consumed = 0;
    int max = Math.min(parseResult, tokens.size());

    while (consumed < max && i < text.length()) {
      while (i < text.length()) {
        int cp = text.codePointAt(i);
        int cc = Character.charCount(cp);
        if (!Character.isWhitespace(cp)) break;
        if (consumed > 0) matched += cc;
        i += cc;
      }

      if (consumed >= max) break;

      String tok = tokens.get(consumed);
      if (tok == null || tok.isEmpty()) break;

      if (i + tok.length() <= text.length() && text.startsWith(tok, i)) {
        matched += tok.length();
        i += tok.length();
        consumed++;
      } else {
        break;
      }
    }
    return matched;
  }

  private void applyHighlighting(List<MatchResult> matches) {
    if (currentTestEditor == null) return;
    clearHighlighting();
    MarkupModel markupModel = currentTestEditor.getMarkupModel();
    for (MatchResult m : matches) {
      markupModel.addRangeHighlighter(
          m.startOffset, m.endOffset,
          HighlighterLayer.SELECTION - 1,
          m.isMatch ? MATCH_ATTRIBUTES : ERROR_ATTRIBUTES,
          HighlighterTargetArea.EXACT_RANGE
      );
    }
  }

  private void clearHighlighting() {
    if (currentTestEditor != null) {
      currentTestEditor.getMarkupModel().removeAllHighlighters();
    }
  }

  public JComponent getComponent() { return mainPanel; }

  @Override
  public void dispose() {
    detachSchemaListener();
    detachTestListener();
    for (Editor ed : testEditorsBySchema.values()) {
      EditorFactory.getInstance().releaseEditor(ed);
    }
    testEditorsBySchema.clear();
  }

  private static class MatchResult {
    final int startOffset, endOffset; final boolean isMatch;
    MatchResult(int s, int e, boolean ok) { this.startOffset = s; this.endOffset = e; this.isMatch = ok; }
  }
}
