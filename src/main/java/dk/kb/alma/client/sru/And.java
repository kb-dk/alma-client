package dk.kb.alma.client.sru;

import java.util.List;
import java.util.stream.Collectors;

public class And extends Query {
    
    List<Query> queries;
    
    public And(List<Query> queries) {
        this.queries = queries;
    }
    
    @Override
    public String build() {
        return "(" + queries.stream().map(query -> query.build()).collect(Collectors.joining(" AND ")) + ")";
    }
}
