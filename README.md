# Rundeck Discord Notification Plugin

Sends rundeck notification messages to a discord channel. This plugin is based on [rundeck-slack-plugin](https://github.com/bitplaces/rundeck-slack-plugin)

## Build / Deploy

- To build the project from source, issue: `./gradlew clean build`
- The resulting jar files can be found under `build/libs`. 
- Copy the `discord-notification-<version>.jar` file to your `$RDECK_BASE/libext` folder
- Restart Rundeck
- You should now have an additional "Discord Notification" option when configuring notifications

## Configuration
This plugin uses Discord incoming-webhooks. Create a new webhook and copy the provided configuration attrinutes.

![configuration](config.png)

Required configuration settings:

- [token](https://discord.com/developers/docs/resources/webhook): the secure token of the webhook
- [webhookId](https://discord.com/developers/docs/resources/webhook): the id of the webhook


## Contributors

*  Original [bitplaces/rundeck-slack-plugin](https://github.com/bitplaces/rundeck-slack-plugin) authors
    *  @totallyunknown
    *  @notandy
    *  @lusis
*  @chobbs