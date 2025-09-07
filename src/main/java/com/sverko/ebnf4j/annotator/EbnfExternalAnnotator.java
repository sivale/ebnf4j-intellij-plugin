package com.sverko.ebnf4j.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.sverko.ebnf.ParseNode;
import com.sverko.ebnf4j.EbnfFileType;
import com.sverko.ebnf4j.EbnfLanguage;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs lightweight syntax/structure checks off the EDT and annotates issues.
 * Uses EBNF4J ParseNodeEvents (with token indices) and maps them to document offsets.
 */
public class EbnfExternalAnnotator extends ExternalAnnotator<EbnfExternalAnnotator.Input, EbnfExternalAnnotator.Result> {

  public static class Input {
    final String text;
    final Document document;
    public Input(String text, Document document) {
      this.text = text;
      this.document = document;
    }
  }

  public static class Problem {
    final int startOffset;
    final int endOffset;
    final String message;
    final HighlightSeverity severity;
    public Problem(int startOffset, int endOffset, String message, HighlightSeverity severity) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.message = message;
      this.severity = severity;
    }
  }

  public static class Result {
    final List<Problem> problems = new ArrayList<>();
  }

  @Override
  public @Nullable Input collectInformation(@NotNull PsiFile file) {
    if (file.getFileType() != EbnfFileType.INSTANCE || file.getLanguage() != EbnfLanguage.INSTANCE) return null;
    Document doc = file.getViewProvider().getDocument();
    if (doc == null) return null;
    return new Input(doc.getText(), doc);
  }

  @Override
  public @Nullable Result doAnnotate(Input input) {
    if (input == null) return null;

    if (input.text.length() > 100_000) {
      return new Result();
    }

    Result result = new Result();

    try {
      long startTime = System.currentTimeMillis();

      EbnfSymbolTracker symbolTracker = new EbnfSymbolTracker();

      // Use the EBNF Lexer to get tokens (for offset calculation)
      com.sverko.ebnf.Lexer ebnfLexer = new com.sverko.ebnf.Lexer();
      java.util.List<String> tokens = ebnfLexer.lexText(input.text);

      // Build token→document mapping (start + length per token)
      TokenMap tm = computeTokenMapExactSearch(input.text, tokens);
      int[] tokenStartOffsets = tm.starts;
      int[] tokenLengths = tm.lengths;

      // NEW: Use the hardcoded EBNF schema parser instead of generated parser
      ParseNode ebnfSchemaStartNode = com.sverko.ebnf.EbnfParseTree.getStartNode();
      Map<String, ParseNode> ebnfNodeMap = com.sverko.ebnf.EbnfParseTree.getNodeMap();

      // Create parser with the hardcoded EBNF schema
      com.sverko.ebnf.Parser schemaParser = new com.sverko.ebnf.Parser(
          ebnfSchemaStartNode,
          ebnfNodeMap,
          Set.of(), // No special lexer tokens needed for EBNF schema
          true      // Ignore whitespace
      );

      // Add symbol tracker as event listener to all relevant EBNF nodes
      schemaParser.assignNodeEventListeners(symbolTracker,
          "meta identifier",
          "defining symbol",
          "concatenate symbol",
          "definition separator symbol",
          "terminal string",
          "start option symbol",
          "end option symbol",
          "start group symbol",
          "end group symbol",
          "start repeat symbol",
          "end repeat symbol",
          "terminator symbol",
          "special sequence",
          "except symbol",
          "integer",
          "repetition symbol"
      );

      int parseResult = schemaParser.parse(input.text);

      // NEW: Check for syntax errors based on parse result
      if (parseResult >= 0 && parseResult < tokens.size()) {
        int startOffset = tokenStartOffsets[parseResult];
        int endOffset   = startOffset + tokenLengths[parseResult];
        String errorToken = tokens.get(parseResult);
        result.problems.add(new Problem(
            startOffset, endOffset,
            "Syntax error: unexpected token '" + errorToken + "'",
            HighlightSeverity.ERROR));
      }
      // Note: parseResult == -1 (END_OF_QUEUE) is ignored during input - that's normal

      // After existing symbol tracking, add logical analysis
      EbnfLogicAnalyzer logicAnalyzer = new EbnfLogicAnalyzer(symbolTracker.getDefinitions());
      List<EbnfLogicAnalyzer.LogicalIssue> logicalIssues = logicAnalyzer.analyzeGrammar(input.text, tokens);

      // Add logical issues to results
      for (EbnfLogicAnalyzer.LogicalIssue issue : logicalIssues) {
        if (issue.tokenIndex >= 0 && issue.tokenIndex < tokens.size()) {
          int startOffset = tokenStartOffsets[issue.tokenIndex];
          int endOffset   = startOffset + tokenLengths[issue.tokenIndex];
          result.problems.add(new Problem(
              startOffset, endOffset, issue.message, issue.severity));
        }
      }

      if (System.currentTimeMillis() - startTime > 1000) {
        return result;
      }

      // Create annotations from symbolTracker results
      for (EbnfSymbolTracker.SymbolInfo undef : symbolTracker.getUndefinedReferences()) {
        int s = Math.max(0, undef.startTokenIndex);
        int e = Math.min(tokens.size(), Math.max(s, undef.endTokenIndex)); // exklusiv
        for (int ti = s; ti < e; ti++) {
          int startOffset = tokenStartOffsets[ti];
          int endOffset   = startOffset + tokenLengths[ti];
          result.problems.add(new Problem(
              startOffset, endOffset,
              "Undefined symbol: '" + undef.name + "'",
              HighlightSeverity.ERROR));
        }
      }

      for (EbnfSymbolTracker.SymbolInfo def : symbolTracker.getDefinitions()) {
        int s = Math.max(0, def.startTokenIndex);
        int e = Math.min(tokens.size(), Math.max(s, def.endTokenIndex));
        for (int ti = s; ti < e; ti++) {
          int startOffset = tokenStartOffsets[ti];
          int endOffset   = startOffset + tokenLengths[ti];
          result.problems.add(new Problem(
              startOffset, endOffset,
              "Definition: '" + def.name + "'",
              HighlightSeverity.INFORMATION));
        }
      }

    } catch (Exception e) {
      System.err.println("Error in EBNF annotation: " + e.getMessage());
      e.printStackTrace();
    }

    return result;
  }

  @Override
  public void apply(@NotNull PsiFile file, Result result, @NotNull AnnotationHolder holder) {
    if (result == null) return;
    for (Problem p : result.problems) {
      // Use TextRange instead of (int, int) overload
      holder.newAnnotation(p.severity, p.message)
          .range(new TextRange(p.startOffset, p.endOffset))
          .create();
    }
  }

  // ---- Mapping-Utilities ----------------------------------------------------

  /**
   * Enthält Start-Offsets und Längen aller Tokens, gemappt auf Dokument-Offsets.
   * Wichtig: Whitespaces im Dokument werden beim Finden des Starts übersprungen
   * (Parser ignoriert sie). Das Ende eines Single-Token-Hits ist dann start + length.
   */
  private static class TokenMap {
    final int[] starts;
    final int[] lengths;
    TokenMap(int[] starts, int[] lengths) {
      this.starts = starts;
      this.lengths = lengths;
    }
  }

  /**
   * Maps a list of tokens (no whitespace tokens) to document offsets by scanning forward in the original text.
   * Whitespaces zwischen Tokens werden im Dokument übersprungen (Parser-Logik).
   */
  private static TokenMap computeTokenMapExactSearch(String text, java.util.List<String> tokens) {
    int[] starts = new int[tokens.size()];
    int[] lengths = new int[tokens.size()];
    int cursor = 0;

    for (int ti = 0; ti < tokens.size(); ti++) {
      String tok = tokens.get(ti);
      int start = text.indexOf(tok, cursor);
      if (start < 0) {
        // Fallback: try global search to avoid -1; in der Praxis sehr selten
        start = text.indexOf(tok);
        if (start < 0) {
          // Ultimativer Fallback: setze auf cursor (verhindert NPEs)
          start = Math.min(cursor, text.length());
        }
      }
      starts[ti] = start;
      lengths[ti] = tok.length();
      // Weiter direkt hinter diesem Token suchen
      cursor = start + tok.length();
    }

    return new TokenMap(starts, lengths);
  }

  private static int indexOfTokenForward(String text, int from, String tok) {
    return text.indexOf(tok, from);
  }
}
