package org.example.dpnrepair.parser.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Marking implements Cloneable {
    private final Map<String, Integer> placeTokenMap = new HashMap<>();

    public Map<String, Integer> getPlaceTokenMap() {
        return placeTokenMap;
    }

    public void addPlaceWithToken(String placeId, int tokenNumber) {
        if (placeId != null && tokenNumber >= 0) {
            this.placeTokenMap.put(placeId, tokenNumber);
        }
    }

    public List<String> getPlacesWithToken() {
        return placeTokenMap.entrySet()
                .stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void removeTokens(Map<String, Integer> placeTokenMap) {
        for(Map.Entry<String, Integer> entry : placeTokenMap.entrySet()) {
            int tokenDiff = this.placeTokenMap.get(entry.getKey()) - entry.getValue();
            this.placeTokenMap.replace(entry.getKey(), tokenDiff);
        }
    }

    public void addTokens(Map<String, Integer> placeTokenMap) {
        for(Map.Entry<String, Integer> entry : placeTokenMap.entrySet()) {
            this.placeTokenMap.replace(entry.getKey(), this.placeTokenMap.get(entry.getKey()) + entry.getValue());
        }
    }


    @Override
    public Marking clone() {
        Marking clone = new Marking();
        for (String s : placeTokenMap.keySet()) {
            clone.getPlaceTokenMap().put(s, placeTokenMap.get(s));
        }
        return clone;
    }
}
