package dk.kb.alma.client.sru;

public class Restriction extends Query {
    
    private final Field field;
    
    private final String restriction;
    
    private final String value;
    
    public Restriction(Field field, String restriction, String value) {
        this.field = field;
        this.restriction = restriction;
        this.value = value;
    }
    
    
    @Override
    public String build() {
        String valueEncoded = value;
        if (value.isBlank() || value.matches("[^<>=/()\\s]")){
            valueEncoded = "\"" + value + "\"";
        }
        return String.join(" ", field.build(), restriction, valueEncoded);
    }
}
