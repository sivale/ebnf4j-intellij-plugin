package com.sverko.ebnf4j.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import java.util.ArrayList;
import java.util.List;

public class EbnfLogicAnalyzer {
    
    private final List<EbnfSymbolTracker.SymbolInfo> definitions;
    
    public EbnfLogicAnalyzer(List<EbnfSymbolTracker.SymbolInfo> definitions) {
        this.definitions = definitions;
    }
    
    public static class LogicalIssue {
        final int tokenIndex;
        final String message;
        final HighlightSeverity severity;
        
        LogicalIssue(int tokenIndex, String message, HighlightSeverity severity) {
            this.tokenIndex = tokenIndex;
            this.message = message;
            this.severity = severity;
        }
    }
    
    public List<LogicalIssue> analyzeGrammar(String text, List<String> tokens) {
        List<LogicalIssue> issues = new ArrayList<>();
        
        findUnreachableAlternatives(tokens, issues);
        findAlwaysEmptyLoops(tokens, issues);
        findRedundantOptionals(tokens, issues);
        findInvalidRepetitions(tokens, issues);
        
        return issues;
    }
    
    private void findUnreachableAlternatives(List<String> tokens, List<LogicalIssue> issues) {
        for (int i = 0; i < tokens.size() - 4; i++) {
            // Pattern: {X} | Y - Y ist nie erreichbar
            if (tokens.get(i).equals("{") && 
                findClosingBrace(tokens, i) != -1) {
                
                int closingBrace = findClosingBrace(tokens, i);
                if (closingBrace + 1 < tokens.size() && 
                    tokens.get(closingBrace + 1).equals("|")) {
                    
                    int alternativeIndex = closingBrace + 2;
                    if (alternativeIndex < tokens.size()) {
                        issues.add(new LogicalIssue(alternativeIndex,
                            "Unreachable alternative - '{...}' always matches (including empty)",
                            HighlightSeverity.WARNING));
                    }
                }
            }
        }
    }
    
    private void findAlwaysEmptyLoops(List<String> tokens, List<LogicalIssue> issues) {
        for (int i = 0; i < tokens.size() - 2; i++) {
            // Pattern: {""} oder {''} - immer leere Schleife
            if (tokens.get(i).equals("{") && 
                i + 2 < tokens.size() &&
                (tokens.get(i + 1).equals("\"\"") || tokens.get(i + 1).equals("''")) &&
                tokens.get(i + 2).equals("}")) {
                
                issues.add(new LogicalIssue(i,
                    "Infinite empty loop - '{\"\"'}' creates endless empty matches",
                    HighlightSeverity.ERROR));
            }
        }
    }
    
    private void findRedundantOptionals(List<String> tokens, List<LogicalIssue> issues) {
        for (int i = 0; i < tokens.size() - 4; i++) {
            // Pattern: [X] | X - redundant
            if (tokens.get(i).equals("[")) {
                int closingBracket = findClosingBracket(tokens, i);
                if (closingBracket != -1 && closingBracket + 1 < tokens.size() && tokens.get(closingBracket + 1).equals("|")) {
                    int alternativeIndex = closingBracket + 2;
                    if (alternativeIndex < tokens.size() && tokens.get(i + 1).equals(tokens.get(alternativeIndex))) {
                        issues.add(new LogicalIssue(alternativeIndex,
                            "Redundant alternative - '[" + tokens.get(i + 1) + "]' already includes '" + tokens.get(alternativeIndex) + "'",
                            HighlightSeverity.WARNING));
                    }
                }
            }
        }
    }

    private void findInvalidRepetitions(List<String> tokens,  List<LogicalIssue> issues) {
        for (int i = 0; i < tokens.size() - 2; i++) {
            // Pattern: (X)* or (X)+ - invalid repetition
            if (tokens.get(i).equals(")") && (tokens.get(i + 1).equals("*") || tokens.get(i + 1).equals("+"))) {
                int openingParenthesis = findOpeningParenthesis(tokens, i);
                if (openingParenthesis != -1) {
                    issues.add(new LogicalIssue(i,
                        "Invalid repetition - repetitions are not allowed on grouped expressions",
                        HighlightSeverity.ERROR));
                }
            }
        }
    }

    private int findClosingBrace(List<String> tokens, int startIndex) {
        int braceCount = 0;
        for (int i = startIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).equals("{")) {
                braceCount++;
            } else if (tokens.get(i).equals("}")) {
                if (braceCount == 0) {
                    return i;
                } else {
                    braceCount--;
                }
            }
        }
        return -1;
    }
    
    private int findClosingBracket(List<String> tokens, int startIndex) {
        int bracketCount = 0;
        for (int i = startIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).equals("[")) {
                bracketCount++;
            } else if (tokens.get(i).equals("]")) {
                if (bracketCount == 0) {
                    return i;
                } else {
                    bracketCount--;
                }
            }
        }
        return -1;
    }

    private int findOpeningParenthesis(List<String> tokens, int startIndex) {
        int parenthesisCount = 0;
        for (int i = startIndex - 1; i >= 0; i--) {
            if (tokens.get(i).equals(")")) {
                parenthesisCount++;
            } else if (tokens.get(i).equals("(")) {
                if (parenthesisCount == 0) {
                    return i;
                } else {
                    parenthesisCount--;
                }
            }
        }
        return -1;
    }
}