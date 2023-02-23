package io.gingersnapproject.search;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.gingersnapproject.Caches;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class QueryHandler {

   @Inject
   Caches caches;

   @Inject
   Instance<SearchBackend> searchBackend;

   public Uni<QueryResult> query(String query) {
      Uni<SearchResult> result = searchBackend.get().query(query);
      return result.onItem().transformToUni(sr ->
            Multi.createFrom().iterable(sr.hits())
                  .onItem().transformToUni(sh -> caches.get(sh.indexName(), sh.documentId()))
                  .concatenate()
                  .collect().asList()
                  .onItem().transform(list -> new QueryResult(sr.hitCount(), sr.hitCountExact(), list, sr.hitsExact()))
      );
   }
}
