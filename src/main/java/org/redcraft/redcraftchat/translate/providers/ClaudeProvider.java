package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClaudeProvider implements TranslationProvider {

    // Chat lines are short, so the reply cannot need many tokens. Keeping this
    // low also caps what a single message can cost if the model rambles.
    private static final int MAX_TOKENS = 1024;

    // The model is told to answer with the translation and nothing else. Chat is
    // relayed verbatim, so a "Sure, here is the translation" preamble would be
    // shown to players as if the player had typed it.
    private static final String SYSTEM_PROMPT = String.join(" ",
            "You are a translation engine for a Minecraft server chat.",
            "Translate the user message from %s to %s.",
            "Reply with the translation only, with no preamble, quotes or explanation.",
            "Keep the tone casual and keep any emotes, punctuation and capitalisation style.",
            "Preserve every placeholder that looks like §x or {0} or %%s exactly as it appears.",
            "If the message cannot be translated, reply with the original message unchanged.");

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public ClaudeProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String translate(String text, String sourceLanguageId, String targetLanguageId)
            throws IllegalStateException, URISyntaxException, IOException, InterruptedException {
        // The token is only required once a translation is actually attempted, so a
        // server with translation switched off still starts without one.
        if (Config.claudeToken == null || Config.claudeToken.isEmpty()) {
            throw new IllegalStateException("Claude translation is selected but claude-token is empty");
        }

        String sourceLangId = sourceLanguageId.toLowerCase().split("-")[0];
        String targetLangId = targetLanguageId.toLowerCase().split("-")[0];

        String cacheKey = String.format("%s;%s;%s", sourceLangId, targetLangId, text);

        String cachedClaudeResponse = (String) CacheManager.get(CacheCategory.CLAUDE_TRANSLATED_MESSAGE, cacheKey,
                String.class);

        if (cachedClaudeResponse != null) {
            return cachedClaudeResponse;
        }

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", text);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", Config.claudeModel);
        payload.addProperty("max_tokens", MAX_TOKENS);
        payload.addProperty("system", String.format(SYSTEM_PROMPT, sourceLangId, targetLangId));
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(new URI(Config.claudeEndpoint))
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .header("x-api-key", Config.claudeToken)
                .header("anthropic-version", Config.claudeApiVersion)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Could not translate text from " + sourceLangId + " to " + targetLangId
                    + " with Claude, the API returned " + response.statusCode() + ": " + response.body());
        }

        String translated = extractText(response.body());

        if (translated == null || translated.isEmpty()) {
            throw new IllegalStateException("Claude returned no translation for " + sourceLangId + " to "
                    + targetLangId);
        }

        // TODO remove debug
        String debugMessage = "Used " + text.length() + " Claude chars to translate to " + targetLangId;
        RedCraftChat.getInstance().getLogger().info(debugMessage);

        CacheManager.put(CacheCategory.CLAUDE_TRANSLATED_MESSAGE, cacheKey, translated);

        return translated;
    }

    // The reply carries a list of content blocks, only the text ones hold the
    // translation. Anything else (a refusal, a tool block) is skipped rather than
    // relayed to players.
    private String extractText(String body) {
        JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();

        if (!parsed.has("content") || !parsed.get("content").isJsonArray()) {
            return null;
        }

        List<String> parts = new ArrayList<String>();

        for (int i = 0; i < parsed.getAsJsonArray("content").size(); i++) {
            JsonObject block = parsed.getAsJsonArray("content").get(i).getAsJsonObject();

            if (block.has("type") && "text".equals(block.get("type").getAsString()) && block.has("text")) {
                parts.add(block.get("text").getAsString());
            }
        }

        return String.join("", parts).trim();
    }
}
