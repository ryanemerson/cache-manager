package io.gingersnapproject.search;

import java.util.List;

public record QueryResult(long hitCount, boolean hitCountExact, List<String> hits, boolean hitsExacts) {
}
