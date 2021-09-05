package com.plugin.discord;

/**
 * @author Craig Hobbs
 */
public class DiscordNotificationPluginException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message error message
     */
    public DiscordNotificationPluginException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message error message
     * @param cause exception cause
     */
    public DiscordNotificationPluginException(String message, Throwable cause) {
        super(message, cause);
    }

}