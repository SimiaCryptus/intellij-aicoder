package com.github.simiacryptus.aicoder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.simiacryptus.aicoder.openai.CompletionRequest;
import com.github.simiacryptus.aicoder.openai.TextCompletion;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OpenAIAPI {

  public static final OpenAIAPI INSTANCE = new OpenAIAPI();

  protected AppSettingsState getSettingsState() {
    return AppSettingsState.getInstance();
  }

  public ObjectNode getEngines() throws IOException {
    return getMapper().readValue(get(getSettingsState().apiBase + "/engines"), ObjectNode.class);
  }

  protected String postRequest(String url, Map<String, Object> map) throws IOException, InterruptedException {
    return post(url, getMapper().writeValueAsString(map));
  }

  protected String post(String url, String body) throws IOException, InterruptedException {
    return post(url, body, 3);
  }

  public TextCompletion complete(CompletionRequest completionRequest, String model) throws IOException, InterruptedException {
    String result = post(getSettingsState().apiBase + "/engines/" + model + "/completions", getMapper().writeValueAsString(completionRequest));
    return getMapper().readValue(result, TextCompletion.class);
  }

  protected String post(String url, String json, int retries) throws IOException, InterruptedException {
    try {
      HttpClientBuilder client = HttpClientBuilder.create();
      HttpPost request = new HttpPost(url);
      request.addHeader("Content-Type", "application/json");
      request.addHeader("Accept", "application/json");
      authorize(request);
      request.setEntity(new StringEntity(json));
      HttpResponse response = client.build().execute(request);
      HttpEntity entity = response.getEntity();
      return EntityUtils.toString(entity);
    } catch (IOException e) {
      if (retries > 0) {
        e.printStackTrace();
        Thread.sleep(15000);
        return post(url, json, retries - 1);
      }
      throw e;
    }
  }

  protected ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(MapperFeature.USE_STD_BEAN_NAMING)
        //.registerModule(DefaultScalaModule)
        .activateDefaultTyping(mapper.getPolymorphicTypeValidator());
    return mapper;
  }

  protected void authorize(HttpRequestBase request) throws IOException {
    request.addHeader("Authorization", "Bearer " + getSettingsState().apiKey);
  }

  public String get(String url) throws IOException {
    HttpClientBuilder client = HttpClientBuilder.create();
    HttpGet request = new HttpGet(url);
    request.addHeader("Content-Type", "application/json");
    request.addHeader("Accept", "application/json");
    authorize(request);
    HttpResponse response = client.build().execute(request);
    HttpEntity entity = response.getEntity();
    return EntityUtils.toString(entity);
  }


  public Function<String, String> xmlFN(String inputTag, String outputTag, String instruction, Map<String, String> inputAttr, Map<String, String> outputAttr) {
    return (originalText) -> {
      String inputAttributes = inputAttr.isEmpty() ? "" : (" " + inputAttr.entrySet().stream().map(t -> String.format("%s=\"%s\"", t.getKey(), t.getValue())).collect(Collectors.joining()));
      String outputAttributes = outputAttr.isEmpty() ? "" : (" " + outputAttr.entrySet().stream().map(t -> String.format("%s=\"%s\"", t.getKey(), t.getValue())).collect(Collectors.joining()));
      CompletionRequest request = new CompletionRequest(
          String.format("<!-- %s -->\n<%s%s>%s</%s>\n<%s%s>", instruction, inputTag, inputAttributes, originalText, inputTag, outputTag, outputAttributes).trim(),
          getSettingsState().temperature,
          getSettingsState().maxTokens,
          String.format("</%s>", outputTag),
          true
      );
      try {
        TextCompletion completion = complete(request, getSettingsState().model);
        Optional<String> completionOption = Optional.ofNullable(completion.choices).flatMap(choices -> Arrays.stream(choices).findFirst()).map(choice -> choice.text.trim());
        if (completionOption.isEmpty()) {
          return originalText;
        } else {
          return stripPrefix(completionOption.get(), request.prompt);
        }
      } catch (Throwable e) {
        e.printStackTrace();
        return originalText;
      }
    };
  }

  public static String stripPrefix(String text, String prefix) {
    boolean startsWith = text.startsWith(prefix);
    if (startsWith) {
      return text.substring(prefix.length());
    } else {
      return text;
    }
  }

}