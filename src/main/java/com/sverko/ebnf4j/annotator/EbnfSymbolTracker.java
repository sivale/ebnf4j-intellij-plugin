package com.sverko.ebnf4j.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EbnfSymbolTracker implements com.sverko.ebnf.ParseNodeEventListener {
    
    public static class SymbolInfo {
        public final String name;
        public final int startTokenIndex;
        public final int endTokenIndex;
        public final SymbolType type;
        
        public enum SymbolType {
            DEFINITION, REFERENCE, TERMINAL_STRING, SPECIAL_SEQUENCE
        }
        
        public SymbolInfo(String name, int startTokenIndex, int endTokenIndex, SymbolType type) {
            this.name = name;
            this.startTokenIndex = startTokenIndex;
            this.endTokenIndex = endTokenIndex;
            this.type = type;
        }
    }
    
    private final List<SymbolInfo> definitions = new ArrayList<>();
    private final List<SymbolInfo> references = new ArrayList<>();
    private boolean afterDefiningSymbol = false;
    
    @Override
    public void parseNodeEventOccurred(com.sverko.ebnf.ParseNodeEvent e) {
        switch (e.parseNode.name) {
            case "meta identifier":
                if (!afterDefiningSymbol) {
                    definitions.add(new SymbolInfo(e.resultString, e.from, e.to, SymbolInfo.SymbolType.DEFINITION));
                } else {
                    references.add(new SymbolInfo(e.resultString, e.from, e.to, SymbolInfo.SymbolType.REFERENCE));
                }
                break;
                
            case "defining symbol":
                afterDefiningSymbol = true;
                break;
                
            case "terminator symbol":
                afterDefiningSymbol = false;
                break;
        }
    }
    
    public List<SymbolInfo> getDefinitions() { return definitions; }
    public List<SymbolInfo> getReferences() { return references; }
    
    public boolean isDefinedSymbol(String symbolName) {
        return definitions.stream().anyMatch(def -> def.name.equals(symbolName));
    }
    
    public List<SymbolInfo> getUndefinedReferences() {
        return references.stream()
            .filter(ref -> !isDefinedSymbol(ref.name))
            .collect(Collectors.toList());
    }
}