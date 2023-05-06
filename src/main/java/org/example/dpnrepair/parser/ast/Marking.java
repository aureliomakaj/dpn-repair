package org.example.dpnrepair.parser.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Marking {
    private final Map<String, Integer> placeTokenMap = new HashMap<>();

    public Map<String, Integer> getPlaceTokenMap() {
        return placeTokenMap;
    }

    public void addPlaceWithToken(String placeId, int tokenNumber) {
        if (placeId != null && tokenNumber >= 0) {
            this.placeTokenMap.put(placeId, tokenNumber);
        }
    }
}
