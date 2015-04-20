package net.es.nsi.topology.translator.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.glassfish.jersey.client.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy class fronting GSON providing a common set of methods and serializers.
 * 
 * @author hacksaw
 */
public class JsonProxy {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Gson serializer;

    /**
     * Constructor creating proxy to Gson with common configuration.
     */
    public JsonProxy() {
        serializer = new GsonBuilder()
        .registerTypeAdapter(XMLGregorianCalendar.class, new XMLGregorianCalendarSerializer())
        .registerTypeAdapter(XMLGregorianCalendar.class, new XMLGregorianCalendarDeserializer())
        .setExclusionStrategies(new JsonExclusionStrategy())
        .setPrettyPrinting()
        .create();
    }

    /**
     * Convert the specified Java object to s JSON string.
     * 
     * @param obj Object to serialize.
     * @return Serialize object.
     */
    public String serialize(Object obj) {
        return serializer.toJson(obj);
    }

    /**
     * Convert a serialized JSON string into a java object of the the specified
     * class.
     * 
     * @param <T> The target type of Java object.
     * @param json The JSON string to deserialize.
     * @param classOfT The target java class of the JSON string.
     * @return Java object representing the JSON string.
     */
    public <T extends Object> T deserialize(String json, Class<T> classOfT) {
        return serializer.fromJson(json, classOfT);
    }

    /**
     * Read a serialized JSON string from the Jersey response object and convert
     * to target Java class.
     * 
     * @param <T> The target type of Java object.
     * @param response The Jersey response object containing the JSON string.
     * @param classOfT The target java class of the JSON string.
     * @return Java object representing the JSON string.
     */
    public <T extends Object> T deserialize(Response response, Class<T> classOfT) {
        final ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {});
        String chunk;
        String finalChunk = null;
        while ((chunk = chunkedInput.read()) != null) {
            finalChunk = chunk;
        }
        response.close();
        return serializer.fromJson(finalChunk, classOfT);
    }

    /**
     * Serialize Java list into JSON string.
     * 
     * @param <T> The source type of the Java object.
     * @param obj The source Java object.
     * @param classOfT The class of the Java object.
     * @return The serialized JSON string.
     */
    public <T extends Object> String serializeList(Object obj, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.toJson(obj, type);
    }

    /**
     * Deserialize a JSON string into a Java list.
     * 
     * @param <T> The target type of the Java object.
     * @param json The serialized JSON string.
     * @param classOfT The class of the target Java object.
     * @return The Java list.
     */
    public <T extends Object> List<T> deserializeList(String json, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.fromJson(json, type);
    }

    /**
     * Read a JSON string from a Jersey response object and deserialize into a Java list.
     * @param <T> The target type of the Java object.
     * @param response The Jersey response object containing the JSON string.
     * @param classOfT The class of the target Java object.
     * @return The Java list.
     */
    public <T extends Object> List<T> deserializeList(Response response, Class<T> classOfT) {
        final ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {});
        String chunk;
        String finalChunk = null;
        while ((chunk = chunkedInput.read()) != null) {
            finalChunk = chunk;
        }
        response.close();
        return deserializeList(finalChunk, classOfT);
    }

    /**
     * Private class for serializing/deserializing JSON lists.
     */
    private static class ListParameterizedType implements ParameterizedType {
        private final Type type;

        private ListParameterizedType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] {type};
        }

        @Override
        public Type getRawType() {
            return ArrayList.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 83 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (this.getClass() != other.getClass()) return false;
            ListParameterizedType otherType = (ListParameterizedType) other;
            return (this.type == otherType.type);
        }
    }

    /**
     * A custom serializer for an XML Gregorian calendar.
     */
    private class XMLGregorianCalendarSerializer implements JsonSerializer<XMLGregorianCalendar> {
        @Override
        public JsonElement serialize(XMLGregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    /**
     * A custom deserializer for an XML Gregorian calendar.
     */
    private class XMLGregorianCalendarDeserializer implements JsonDeserializer<XMLGregorianCalendar> {
      @Override
      public XMLGregorianCalendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
          try {
              return DatatypeFactory.newInstance().newXMLGregorianCalendar(json.getAsJsonPrimitive().getAsString());
          } catch (DatatypeConfigurationException ex) {
              log.error("XMLGregorianCalendarDeserializer: Could not convert supplied date " +  json.getAsJsonPrimitive().getAsString());
              return(null);
          }
      }
    }
    
    /**
     * Serialize the specified java object to a JSON string and write to file.
     * 
     * @param file Target file for the JSON output.
     * @param obj Java object to convert.
     * @throws IOException If file cannot be open for writing.
     */
    public void write(String file, Object obj) throws IOException {       
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            serializer.toJson(obj, bw);
        }
   }
}
