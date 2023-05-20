package org.example.dpnrepair.parser.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Marking implements Cloneable {
    private Map<String, Integer> placeTokenMap = new HashMap<>();

    public Map<String, Integer> getPlaceTokenMap() {
        return placeTokenMap;
    }

    public void setPlaceTokenMap(Map<String, Integer> placeTokenMap) {
        this.placeTokenMap = placeTokenMap;
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

    public boolean greaterThanOrEqual(Marking other) {
        return placeTokenMap.entrySet()
                .stream()
                .allMatch(entry -> entry.getValue() >= other.getPlaceTokenMap().get(entry.getKey()));
    }

    public boolean greaterThan(Marking other) {
        return greaterThanOrEqual(other) && placeTokenMap.entrySet().stream().anyMatch(entry -> entry.getValue() > other.getPlaceTokenMap().get(entry.getKey()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Marking marking = (Marking) o;
        return Objects.equals(placeTokenMap, marking.placeTokenMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeTokenMap);
    }

    @Override
    public Marking clone() {
        try {
            Marking clone = (Marking) super.clone();
            clone.setPlaceTokenMap(new HashMap<>(placeTokenMap));
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
