package dk.kb.alma.client.sru;

public class Field {
    
    private String name;
    private String description;
    
    public Field(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String build(){
        return name;
    }
    
}
