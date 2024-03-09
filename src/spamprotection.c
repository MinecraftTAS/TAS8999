#include "spamprotection.h"

#include <concord/log.h>
#include <string.h>

u64snowflake monitored_user = 0; //!< User to monitor
u64snowflake last_channel_id = 0; //!< Last channel the user sent a message in
u64snowflake message_channelids[5]; //!< Last 5 channels the user sent a message in
u64snowflake message_ids[5]; //!< Last 5 messages the user sent
uint64_t last_url_time = 0; //!< Last time a URL was sent by the user
uint64_t amount_of_urls = 0; //!< Amount of URLs sent by the user

void spamprotection_on_message(struct discord *client, const struct discord_message *event) {
    // check if message contains url
    if (!strstr(event->content, "http://") && !strstr(event->content, "https://") && !strstr(event->content, "discord.gg")) {
        if (event->author->id == monitored_user) {
            monitored_user = 0;
            last_channel_id = 0;
            last_url_time = 0;
            amount_of_urls = 0;
            log_info("[SPAMPROTECTION] Cleared monitored user due to sending non-URL message");
        }

        return;
    }

    // clear monitored user if 10s passed
    if ((event->timestamp - last_url_time) > 10000) {
        monitored_user = 0;
        last_channel_id = 0;
        last_url_time = 0;
        amount_of_urls = 0;
        log_info("[SPAMPROTECTION] Cleared monitored user due to inactivity");
    }

    // check if message is in the same channel
    if (event->channel_id == last_channel_id) {
        monitored_user = 0;
        last_channel_id = 0;
        last_url_time = 0;
        amount_of_urls = 0;
        log_info("[SPAMPROTECTION] Cleared monitored user due to sending URL in the same channel");
    }

    // update monitored user
    u64snowflake user = event->author->id;
    if (user != monitored_user) {
        monitored_user = user;
        last_channel_id = event->channel_id;
        last_url_time = event->timestamp;
        message_ids[0] = event->id;
        message_channelids[0] = event->channel_id;
        amount_of_urls = 1;
        log_info("[SPAMPROTECTION] Updated monitored user to %lu", monitored_user);
        return;
    }

    // check if user sent 5 or more urls in 10s
    message_ids[amount_of_urls] = event->id;
    message_channelids[amount_of_urls] = event->channel_id;
    if (++amount_of_urls >= 5) {
        discord_remove_guild_member(client, event->guild_id, monitored_user, &(struct discord_remove_guild_member) {
            .reason = "Spam Protection flagged user for sending too many URLs"
        }, NULL);

        for (int i = 0; i < 5; i++) {
            discord_delete_message(client, message_channelids[i], message_ids[i], &(struct discord_delete_message) {
                .reason = "Spam Protection flagged user for sending too many URLs"
            }, NULL);
        }

        log_info("[SPAMPROTECTION] Kicked user %lu for sending too many URLs", monitored_user);

        monitored_user = 0;
        last_channel_id = 0;
        last_url_time = 0;
        amount_of_urls = 0;
        return;
    }

    // update last url time
    last_channel_id = event->channel_id;
    last_url_time = event->timestamp;
    log_info("[SPAMPROTECTION] User %lu sent %lu URLs within 10s", monitored_user, amount_of_urls);
}
