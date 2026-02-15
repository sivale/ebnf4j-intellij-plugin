package com.sverko.ebnf4j.annotator;

import com.sverko.ebnf.ParseNodeEventListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EbnfSymbolTracker implements ParseNodeEventListener {

  private final List<String> predefinedNodeNames;
  private final Set<String> definedNames = new HashSet<>();

  public EbnfSymbolTracker(List<String> predefinedNodeNames) {
    this.predefinedNodeNames = predefinedNodeNames != null ? predefinedNodeNames : List.of();

    // Seed predefined names as "defined"
    definedNames.addAll(this.predefinedNodeNames);
  }

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
    System.out.println(e.getNode().getName() + ": " + e.getTrimmed());
    switch (e.getNode().getName()) {
      case "meta identifier":
        if (!afterDefiningSymbol) {
          String name = e.getTrimmed();
          definitions.add(new SymbolInfo(name, e.getTrimmedFromPtr(), e.getTrimmedToPtr(), SymbolInfo.SymbolType.DEFINITION));
          definedNames.add(name);
        } else {
          references.add(new SymbolInfo(e.getTrimmed(), e.getTrimmedFromPtr(), e.getTrimmedToPtr(), SymbolInfo.SymbolType.REFERENCE));
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
    return symbolName != null && definedNames.contains(symbolName);
  }

  public List<SymbolInfo> getUndefinedReferences() {
    return references.stream()
        .filter(ref -> !isDefinedSymbol(ref.name))
        .collect(Collectors.toList());
  }
}