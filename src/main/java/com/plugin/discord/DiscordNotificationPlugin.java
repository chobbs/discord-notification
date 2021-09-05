package com.plugin.discord;

/**
 * Sends Rundeck job notification messages to a Discord.
 *
 * @author Craig Hobbs
 */

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;

import com.dtolabs.rundeck.plugins.descriptions.Password;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;


@Plugin(service= "Notification", name="DiscordNotification")
@PluginDescription(title="Discord Pligin", description="Post Rundeck Notifications to Discord")
public class DiscordNotificationPlugin implements NotificationPlugin {

    private static final String DISCORD_MESSAGE_COLOR_GREEN = "65280";
    private static final String DISCORD_MESSAGE_COLOR_YELLOW = "16776960";
    private static final String DISCORD_MESSAGE_COLOR_RED = "16711680";

    private static final String DISCORD_MESSAGE_TEMPLATE = "discord-incoming-message.ftl";

    private static final String TRIGGER_START = "start";
    private static final String TRIGGER_SUCCESS = "success";
    private static final String TRIGGER_FAILURE = "failure";
    private static final String TRIGGER_AVERAGE = "avgduration";
    private static final String TRIGGER_ONRETRY = "retryablefailure";

    private static final Map<String, DiscordNotificationData> TRIGGER_NOTIFICATION_DATA = new HashMap<String, DiscordNotificationData>();

    private static final Configuration FREEMARKER_CFG = new Configuration();

    @PluginProperty(title = "Discord URL",
                    description = "Discord Base URL",
                    defaultValue = "https://discordapp.com/api/webhooks",
                    scope=PropertyScope.Instance)
    private String webhook_base_url;

    @Password
    @PluginProperty(title = "Token",
                    description = "Webhook Token, something like C00000000_B00000000_XXXXXXXXXXXXXXXXXXXXXXXX",
                    scope=PropertyScope.Instance)
    private String webhook_token;

    @PluginProperty(title = "Channel_ID",
                    description = "Channel_id of the webhook",
                    scope=PropertyScope.Instance)
    private String webhook_id;

    /**
     * Sends a message to a Discord channel when a job notification event is raised by Rundeck.
     *
     * @param trigger name of job notification event causing notification
     * @param executionData job execution data
     * @param config plugin configuration
     * @throws DiscordNotificationPluginException when any error occurs sending the Discord message
     * @return true, if the Discord API response indicates a message was successfully delivered to a chat room
     */
    public boolean postNotification(String trigger, Map executionData, Map config) {

        String ACTUAL_DISCORD_TEMPLATE;

        ClassTemplateLoader builtInTemplate = new ClassTemplateLoader(DiscordNotificationPlugin.class, "/templates");
        TemplateLoader[] loaders = new TemplateLoader[]{builtInTemplate};
        MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
        FREEMARKER_CFG.setTemplateLoader(mtl);
        ACTUAL_DISCORD_TEMPLATE = DISCORD_MESSAGE_TEMPLATE;

        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_START,   new DiscordNotificationData(ACTUAL_DISCORD_TEMPLATE, DISCORD_MESSAGE_COLOR_YELLOW));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_SUCCESS, new DiscordNotificationData(ACTUAL_DISCORD_TEMPLATE, DISCORD_MESSAGE_COLOR_GREEN));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_FAILURE, new DiscordNotificationData(ACTUAL_DISCORD_TEMPLATE, DISCORD_MESSAGE_COLOR_RED));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_AVERAGE, new DiscordNotificationData(ACTUAL_DISCORD_TEMPLATE, DISCORD_MESSAGE_COLOR_YELLOW));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_ONRETRY, new DiscordNotificationData(ACTUAL_DISCORD_TEMPLATE, DISCORD_MESSAGE_COLOR_YELLOW));


        try {
            FREEMARKER_CFG.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
        }catch(Exception e){
            System.err.printf("Got and exception from Freemarker: %s", e.getMessage());
        }

        if (!TRIGGER_NOTIFICATION_DATA.containsKey(trigger)) {
            throw new IllegalArgumentException("Unknown trigger type: [" + trigger + "].");
        }

        if(this.webhook_base_url.isEmpty() || this.webhook_token.isEmpty()){
            throw new IllegalArgumentException("URL or Token not set");
        }

        String webhook_url=this.webhook_base_url+"/"+this.webhook_id+"/"+this.webhook_token;

        //String message = generateMessage(trigger, executionData, config, this.webhook_id);
        String message = generateMessage(trigger, executionData, config);
        String discordResponse = invokeDiscordAPIMethod(webhook_url, message);
        String ms = "payload_json=" + message;

        if ("ok".equals(discordResponse)) {
            return true;
        } else {
            // Throwing an exception as Discord RC is not 204, message will being logged.
            throw new DiscordNotificationPluginException("Unknown status returned from Discord API: [" + discordResponse + "]." + "\n" + ms);
        }
    }

    private String generateMessage(String trigger, Map executionData, Map config) {
        String templateName = TRIGGER_NOTIFICATION_DATA.get(trigger).template;
        String color = TRIGGER_NOTIFICATION_DATA.get(trigger).color;

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("trigger", trigger);
        model.put("color", color);
        model.put("executionData", executionData);
        model.put("config", config);

        StringWriter sw = new StringWriter();
        try {
            Template template = FREEMARKER_CFG.getTemplate(templateName);
            template.process(model,sw);

        } catch (IOException ioEx) {
            throw new DiscordNotificationPluginException("Error loading Discord notification message template: [" + ioEx.getMessage() + "].", ioEx);
        } catch (TemplateException templateEx) {
            throw new DiscordNotificationPluginException("Error merging Discord notification message template: [" + templateEx.getMessage() + "].", templateEx);
        }

        return sw.toString();

    }

    private String invokeDiscordAPIMethod(String webhook_url, String body) {
        URL requestUrl = toURL(webhook_url);

        HttpURLConnection connection = null;
        InputStream responseStream = null;
        int responseCode = 0;

        try {
            connection = openConnection(requestUrl);
            putRequestStream(connection, body);
            responseCode = getResponseCode(connection);
            responseStream = getResponseStream(connection);
            return getDiscordResponse(responseStream, responseCode);

        } finally {
            closeQuietly(responseStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException malformedURLEx) {
            throw new DiscordNotificationPluginException("Discord API URL is malformed: [" + malformedURLEx.getMessage() + "].", malformedURLEx);
        }
    }

    private HttpURLConnection openConnection(URL requestUrl) {
        try {
            return (HttpURLConnection) requestUrl.openConnection();
        } catch (IOException ioEx) {
            throw new DiscordNotificationPluginException("Error opening connection to Discord URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private void putRequestStream(HttpURLConnection connection, String message) {
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(message);
            wr.flush();
            wr.close();
        } catch (IOException ioEx) {
            throw new DiscordNotificationPluginException("Error putting data to Discord URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private InputStream getResponseStream(HttpURLConnection connection) {
        InputStream input = null;
        try {
            input = connection.getInputStream();
        } catch (IOException ioEx) {
            input = connection.getErrorStream();
        }
        return input;
    }

    private int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException ioEx) {
            throw new DiscordNotificationPluginException("Failed to obtain HTTP response code: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private String getDiscordResponse(InputStream responseStream, int responseCode) {
        if (responseCode == 204) {
            return "ok";
        }
        else {
            try {
                return new Scanner(responseStream,"UTF-8").useDelimiter("\\A").next();
            } catch (Exception ioEx) {
                throw new DiscordNotificationPluginException("Error reading Discord API JSON response: [" + ioEx.getMessage() + "].", ioEx);
            }
        }
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ioEx) {
                // ignore
            }
        }
    }

    private static class DiscordNotificationData {
        private String template;
        private String color;
        public DiscordNotificationData(String template, String color) {
            this.color = color;
            this.template = template;
        }
    }

}