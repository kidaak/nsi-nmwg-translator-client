package net.es.nsi.topology.translator.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * Allows for specific elements to be excluded from JSON serialization.
 * 
 * @author hacksaw
 */
public class JsonExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return "self".contentEquals(f.getName());
    }
}