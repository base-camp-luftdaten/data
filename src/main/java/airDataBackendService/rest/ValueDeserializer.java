package airDataBackendService.rest;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ValueDeserializer extends StdDeserializer<Double> {
  private static final long serialVersionUID = -6255587348998813262L;

  public ValueDeserializer() {
    super(Double.class);
  }

  @Override
  public Double deserialize(JsonParser jp, DeserializationContext ctxt) {
    JsonNode node;

    try {
      node = jp.getCodec().readTree(jp);
    } catch (IOException e) {
      return new Double(-1);
    }

    // let jackson do the parsing, but use -1 as a default value
    // in case parsing failed (because ".asDouble" doesn't throw on failure)
    double value = node.asDouble(-1);

    return new Double(value);
  }
}