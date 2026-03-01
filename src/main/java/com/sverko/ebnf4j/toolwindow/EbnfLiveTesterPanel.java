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
import com.sverko.ebnf.TokenQueue;
import com.sverko.ebnf4j.EbnfFileType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.util.Alarm;
import com.intellij.openapi.editor.markup.RangeHighlighter;

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

  private static final int ERROR_DEBOUNCE_MS = 1000;
  private final Alarm errorAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

  private final List<RangeHighlighter> greenHighlighters = new ArrayList<>();
  private final List<RangeHighlighter> redHighlighters   = new ArrayList<>();

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

  private static boolean isWsToken(String tok) {
    if (tok == null || tok.isEmpty()) return false;
    for (int i = 0; i < tok.length(); i++) {
      if (!Character.isWhitespace(tok.charAt(i))) return false;
    }
    return true;
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
    testEditor.getSettings().setTabSize(4);
    testEditor.getSettings().setVirtualSpace(true);
    testEditor.getSettings().setUseTabCharacter(true);
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
    errorAlarm.cancelAllRequests();
    removeGreenHighlighters();
    removeRedHighlighters();

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
      applyHighlightsWithDelay(matches);

    } catch (Exception e) {
      System.err.println("Error in live updating: " + e.getMessage());
      clearHighlighting();
    }
  }

  private List<MatchResult> parseAndMatch(String schema, String testText) {
    List<MatchResult> results = new ArrayList<>();
    try {
      Lexer schemaLexer = new Lexer(Set.of("\\n","\\t","\\s","{:"));
      TokenQueue schemaTokens = schemaLexer.lexText(schema);

      EbnfParserGenerator generator = new EbnfParserGenerator();
      Parser parser = generator.getParser(schemaTokens, true);

      // Gesamter Parse
      int rFull = parser.parse(testText);

      // Test-Tokens + Segmente (mit dem Lexer des Parsers, gleiche Flags)
      TokenQueue testTokens = parser.getLexer().lexText(testText);
      List<int[]> segs = lexTokenSegments(testText, testTokens);

      // Akzeptierte Token-Anzahl bestimmen
      int accepted;
      if (rFull == testTokens.rawSize()) {
        // kompletter Erfolg
        accepted = segs.size();
      } else if (rFull >= 0 && rFull < testTokens.rawSize()) {
        // Parser hat n akzeptiert und dann Schluss gemacht → n ist Präfix
        accepted = Math.min(rFull, segs.size());
      } else if (rFull == -2) {
        // EndOfQueue zu früh: alles bisherige ist akzeptiert
        accepted = segs.size();
      } else {
        // -1, -100 oder sonstiger Fehler: via Präfix-Suche ermitteln
        accepted = parser.getTokenQueue().getLastTokenFound();
      }

      // Grün: genau die akzeptierten Token-Segmente
      for (int i = 0; i < accepted; i++) {
        int[] s = segs.get(i);
        String tok = testTokens.get(i);
        if (testTokens.isUnhandledWhitespace(i)) continue;   // <- neu
        results.add(new MatchResult(s[0], s[1], true));
      }

      // Rot (verzögert): nur, wenn wirklich Fehler/Extra vorliegt (also NICHT -2)
      boolean shouldPaintRed =
          (rFull == -1 || rFull == -100) ||
              (rFull >= 0 && rFull < testTokens.rawSize());

      if (shouldPaintRed) {
        int tailStart = (accepted > 0) ? segs.get(accepted - 1)[1] : 0;
        if (tailStart < testText.length()) {
          results.add(new MatchResult(tailStart, testText.length(), false));
        }
      }

    } catch (Exception e) {
      System.err.println("Error parsing: " + e.getMessage());
      results.clear();
      results.add(new MatchResult(0, testText.length(), false));
    }
    return results;
  }

  private List<int[]> lexTokenSegments(String text, TokenQueue tokens) {
    List<int[]> segs = new ArrayList<>();
    if (text == null || tokens == null) return segs;

    int cursor = 0;
    for (String tok : tokens.getTokens()) {
      if (tok == null) break;

      int end = cursor + tok.length();
      if (end > text.length()) break;

      // Idealfall: 1:1 Slice Match
      if (!text.regionMatches(cursor, tok, 0, tok.length())) {
        // Fallback: nur um nicht komplett auszufallen (sollte im neuen System selten sein)
        int start = text.indexOf(tok, cursor);
        if (start < 0) break;
        cursor = start;
        end = cursor + tok.length();
        if (end > text.length()) break;
      }

      segs.add(new int[]{cursor, end});
      cursor = end;
    }
    return segs;
  }

  private List<MatchResult> computeMatchedTokenSegments(String text, List<String> tokens, int matchedCount) {
    List<MatchResult> segs = new ArrayList<>();
    if (matchedCount <= 0 || text == null || text.isEmpty() || tokens == null || tokens.isEmpty()) {
      return segs;
    }
    int i = 0, consumed = 0;
    while (consumed < matchedCount && i < text.length()) {
      while (i < text.length()) {
        int cp = text.codePointAt(i);
        int cc = Character.charCount(cp);
        if (!Character.isWhitespace(cp)) break;
        i += cc; // Whitespace nicht highlighten
      }
      if (consumed >= matchedCount) break;
      String tok = tokens.get(consumed);
      if (tok == null || tok.isEmpty()) break;
      if (i + tok.length() <= text.length() && text.startsWith(tok, i)) {
        segs.add(new MatchResult(i, i + tok.length(), true)); // nur das Wort
        i += tok.length();
        consumed++;
      } else break;
    }
    return segs;
  }

  private void applyHighlightsWithDelay(List<MatchResult> matches) {
    if (currentTestEditor == null) return;

    // bei jeder Änderung: alles Alte weg, Grün sofort neu setzen
    errorAlarm.cancelAllRequests();
    removeGreenHighlighters();
    removeRedHighlighters();

    var doc = currentTestEditor.getDocument();
    long stamp = doc.getModificationStamp();
    var mm = currentTestEditor.getMarkupModel();

    // 4a) Grün sofort
    for (MatchResult m : matches) {
      if (!m.isMatch) continue;
      RangeHighlighter hl = mm.addRangeHighlighter(
          m.startOffset, m.endOffset,
          HighlighterLayer.SELECTION - 1,
          MATCH_ATTRIBUTES,
          HighlighterTargetArea.EXACT_RANGE
      );
      greenHighlighters.add(hl);
    }

    // 4b) Rot später (nur wenn es überhaupt rote Ranges gibt)
    boolean hasRed = matches.stream().anyMatch(x -> !x.isMatch);
    if (!hasRed) return;

    errorAlarm.addRequest(() -> {
      // nur anwenden, wenn sich der Text seit dem Planen nicht verändert hat
      if (currentTestEditor == null) return;
      if (currentTestEditor.getDocument().getModificationStamp() != stamp) return;

      // falls zwischenzeitlich was Rot gesetzt wurde, bereinigen
      removeRedHighlighters();
      var mmNow = currentTestEditor.getMarkupModel();
      for (MatchResult m : matches) {
        if (m.isMatch) continue;
        RangeHighlighter hl = mmNow.addRangeHighlighter(
            m.startOffset, m.endOffset,
            HighlighterLayer.SELECTION - 1,
            ERROR_ATTRIBUTES,
            HighlighterTargetArea.EXACT_RANGE
        );
        redHighlighters.add(hl);
      }
    }, ERROR_DEBOUNCE_MS);
  }


  private void clearHighlighting() {
    errorAlarm.cancelAllRequests();
    removeGreenHighlighters();
    removeRedHighlighters();
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

  private void removeGreenHighlighters() {
    for (RangeHighlighter hl : greenHighlighters) {
      try { hl.dispose(); } catch (Throwable ignored) {}
    }
    greenHighlighters.clear();
  }

  private void removeRedHighlighters() {
    for (RangeHighlighter hl : redHighlighters) {
      try { hl.dispose(); } catch (Throwable ignored) {}
    }
    redHighlighters.clear();
  }
}
