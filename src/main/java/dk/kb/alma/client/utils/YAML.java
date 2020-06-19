package dk.kb.alma.client.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.ws.rs.ext.ContextResolver;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.Date;


public class YAML implements ContextResolver<ObjectMapper> {
  private final ObjectMapper mapper;

  public YAML() {
    
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);

    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

    mapper.setDateFormat(new RFC3339DateFormat());
    mapper.registerModule(new JavaTimeModule());
  }
  
  public static String toYaml(Object object) {
      return toYaml(object, true);
  }
  
  public static <T> String toYaml(T object, boolean indent) {
      
      if (object == null) {
          return "";
      }
      YAML yaml = new YAML();
      ObjectMapper mapper = yaml.getContext(object.getClass());
      
      
      if (indent) {
          mapper.enable(SerializationFeature.INDENT_OUTPUT);
      } else {
          mapper.disable(SerializationFeature.INDENT_OUTPUT);
      }
      
      
      try {
          return mapper.writeValueAsString(object);
      } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
      }
  }
  
  public static <T> T fromYaml(String object, Class<T> type) {
      YAML json = new YAML();
      ObjectMapper mapper = json.getContext(type);
      
      try {
          return mapper.readValue(object, type);
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
    
    public static <T> T fromYaml(File object, Class<T> type) {
        YAML json = new YAML();
        ObjectMapper mapper = json.getContext(type);
        
        try {
            return mapper.readValue(object, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
  
  /**
   * Set the date format for JSON (de)serialization with Date properties.
   * @param dateFormat Date format
   */
  public void setDateFormat(DateFormat dateFormat) {
    mapper.setDateFormat(dateFormat);
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }


  static class RFC3339DateFormat extends com.fasterxml.jackson.databind.util.StdDateFormat {

    private static final long serialVersionUID = -3215326930097719238L;

    // Same as ISO8601DateFormat but serializing milliseconds.
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
      String value = ISO8601Utils.format(date, true);
      toAppendTo.append(value);
      return toAppendTo;
    }

  }
}
