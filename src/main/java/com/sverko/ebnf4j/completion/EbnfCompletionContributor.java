package com.sverko.ebnf4j.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.sverko.ebnf.ParseNode;
import com.sverko.ebnf4j.EbnfLanguage;
import com.sverko.ebnf4j.lexer.EbnfTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EbnfCompletionContributor extends CompletionContributor implements EbnfTokenTypes {

  public EbnfCompletionContributor() {
        // Simplified pattern - trigger on any IDENTIFIER token
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(IDENTIFIER)
                        .withLanguage(EbnfLanguage.INSTANCE),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        
                        System.out.println("DEBUG: EbnfCompletionContributor triggered!");
                        
                        PsiElement position = parameters.getPosition();
                        System.out.println("DEBUG: Position element: " + position.getText());
                        System.out.println("DEBUG: Position type: " + position.getNode().getElementType());
                        
                        if (shouldProvideCompletion(position)) {
                            System.out.println("DEBUG: Should provide completion = true");
                            addDefinitionCompletions(position, result);
                        } else {
                            System.out.println("DEBUG: Should provide completion = false");
                        }
                    }
                });
    }

    private boolean shouldProvideCompletion(PsiElement position) {
        // Debug: Always provide completion for now to test
        System.out.println("DEBUG: Checking if should provide completion...");
        
        // Get text before cursor to see context
        String textBeforeCursor = getTextBeforeCursor(position);
        System.out.println("DEBUG: Text before cursor: '" + textBeforeCursor + "'");
        
        // For now, always provide completion if we're typing an identifier
        // Later we can refine this to check for semicolon context
        return true; // Temporarily always true for debugging
    }
    
    private String getTextBeforeCursor(PsiElement position) {
        String fileText = position.getContainingFile().getText();
        int cursorOffset = position.getTextOffset();
        
        // Get last 50 characters before cursor for context
        int start = Math.max(0, cursorOffset - 50);
        return fileText.substring(start, cursorOffset);
    }

    private void addDefinitionCompletions(PsiElement position, CompletionResultSet result) {
        System.out.println("DEBUG: Adding definition completions...");
        
        // Extract the actual prefix (remove IntelliJ's dummy text)
        String elementText = position.getText();
        String actualPrefix = elementText.replace("IntellijIdeaRulezzz", "");
        System.out.println("DEBUG: Actual prefix: '" + actualPrefix + "'");
        
        // Create a result set with the correct prefix matcher
        CompletionResultSet prefixedResult = result.withPrefixMatcher(actualPrefix);
        
        // Get all existing definitions from the file
        Set<String> existingDefinitions = collectExistingDefinitions(position);
        System.out.println("DEBUG: Found " + existingDefinitions.size() + " existing definitions: " + existingDefinitions);
        
        // Add all existing definitions as completion suggestions
        for (String definition : existingDefinitions) {
            // Skip the definition that contains the dummy text (incomplete current typing)
            if (!definition.contains("IntellijIdeaRulezzz")) {
                prefixedResult.addElement(LookupElementBuilder.create(definition)
                        .withTypeText("EBNF Definition")
                        .withIcon(null)); // You can add an icon later
            }
        }
    }

    private Set<String> collectExistingDefinitions(PsiElement position) {
        try {
            String fileText = position.getContainingFile().getText();
            
            // Create our simple definition collector
            CompletionDefinitionCollector definitionCollector = new CompletionDefinitionCollector();
            
            // Use the hardcoded EBNF schema parser
            ParseNode ebnfSchemaStartNode = com.sverko.ebnf.EbnfParseTree.getStartNode();
            Map<String, ParseNode> ebnfNodeMap = com.sverko.ebnf.EbnfParseTree.getNodeMap();
            
            com.sverko.ebnf.Parser schemaParser = new com.sverko.ebnf.Parser(
                ebnfSchemaStartNode, 
                ebnfNodeMap, 
                Set.of(), 
                true
            );
            
            // Add our simple collector as event listener - only need these 3 events
            schemaParser.assignNodeEventListeners(definitionCollector, 
                "meta identifier",
                "defining symbol", 
                "terminator symbol"
            );
            
            schemaParser.parse(fileText);
            
            Set<String> definitions = definitionCollector.getDefinitionNames();
            System.out.println("DEBUG: Collected definitions: " + definitions);
            return definitions;
            
        } catch (Exception e) {
            System.err.println("DEBUG: Error collecting definitions: " + e.getMessage());
            return new HashSet<>();
        }
    }
    
    private boolean isValidIdentifier(String token) {
        return token != null && !token.isEmpty() && 
               Character.isLetter(token.charAt(0)) && 
               token.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_');
    }
}