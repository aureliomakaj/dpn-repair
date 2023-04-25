package org.example.dpnrepair.parser.ast;

import java.util.ArrayList;
import java.util.List;

public class Marking {
    private final List<String> placeIds = new ArrayList<>();

    public List<String> getPlaceIds() {
        return placeIds;
    }

    public void addPlaceIds(String placeId) {
        if(placeId != null){
            this.placeIds.add(placeId);
        }
    }
}
