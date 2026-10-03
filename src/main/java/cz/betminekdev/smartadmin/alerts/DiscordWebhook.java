package cz.betminekdev.smartadmin.alerts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Set;

public final class DiscordWebhook {
    private static final Set<String> HOSTS = Set.of("discord.com", "discordapp.com", "canary.discord.com", "ptb.discord.com");

    private DiscordWebhook() {
    }

    public static URI validate(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || !HOSTS.contains(uri.getHost().toLowerCase(java.util.Locale.ROOT))
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.getRawPath().matches("/api(?:/v[0-9]+)?/webhooks/[0-9]+/[A-Za-z0-9_-]+")) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            // Never include a webhook token in an exception or log.
            throw new IllegalArgumentException("Use an HTTPS Discord webhook URL without query parameters.");
        }
    }

    public static String payload(String content) {
        JsonObject payload = new JsonObject();
        payload.addProperty("content", content.length() > 1900 ? content.substring(0, 1900) : content);
        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        payload.add("allowed_mentions", mentions);
        return payload.toString();
    }
}
