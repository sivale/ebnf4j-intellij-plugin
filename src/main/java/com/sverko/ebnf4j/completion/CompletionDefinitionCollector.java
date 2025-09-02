package com.sverko.ebnf4j.completion;

import com.sverko.ebnf.ParseNodeEvent;
import com.sverko.ebnf.ParseNodeEventListener;
import java.util.HashSet;
import java.util.Set;

public class CompletionDefinitionCollector implements ParseNodeEventListener {

  private final Set<String> definitionNames = new HashSet<>();
  private boolean afterDefiningSymbol = false;

  @Override
  public void parseNodeEventOccurred(ParseNodeEvent e) {
    switch (e.parseNode.name) {
      case "meta identifier":
          definitionNames.add(e.resultString);
          System.out.println("DEBUG: Found definition: " + e.resultString);
      break;

      case "defining symbol":
        afterDefiningSymbol = true;
        break;

      case "terminator symbol":
        afterDefiningSymbol = false;
        break;
    }
  }

  public Set<String> getDefinitionNames() {
    return new HashSet<>(definitionNames); // Defensive copy
  }
}