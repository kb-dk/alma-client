package dk.kb.alma.client.sru;

import java.util.List;
import java.util.stream.Collectors;

public class Or extends Query {
    
    List<Query> queries;
    
    public Or(List<Query> queries) {
        this.queries = queries;
    }
    
    @Override
    public String build() {
        if (queries.size() > 1) {
            return "("+queries.stream().map(query -> query.build()).collect(Collectors.joining(" OR "))+")";
        } else {
            return queries.stream().map(query -> query.build()).collect(Collectors.joining(" OR "));
        }
    }
    
}
