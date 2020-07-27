package dk.kb.alma.client.sru;

public class SortBy extends Query {

    private final SortableField sortField;
    
    private final boolean descending;
    
    private final  Query query;
    
    public SortBy(SortableField sortField, boolean descending, Query query) {
        this.sortField = sortField;
        this.descending = descending;
        this.query = query;
    }
    
    @Override
    public String build() {
        String direction = "ascending";
        if (descending){
            direction = "descending";
        }
        return query.build() + " sortBy "+sortField.build()+"/sort."+direction;
    }
}
