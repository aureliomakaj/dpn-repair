package org.example.dpnrepair.parser.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Marking {
    private final Map<String, Integer> placeTokenMap = new HashMap<>();

    public Map<String, Integer> getPlaceTokenMap() {
        return placeTokenMap;
    }

    public void addPlaceWithToken(String placeId, int tokenNumber) {
        if(placeId != null && tokenNumber >= 0){
            this.placeTokenMap.put(placeId, tokenNumber);
        }
    }

    public List<String> getPlacesWithToken() {
        return placeTokenMap.entrySet()
                .stream()
                .filter( e -> e.getValue() == 1)
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }
}
