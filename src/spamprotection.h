/**
 * \file spamprotection.h
 * This file adds the spam protection module to the bot.
 * If users send too many links within a certain time frame, they will be kicked.
 */

#pragma once

#include <concord/discord.h>

/**
 * On message sent
 *
 * \param client Discord client
 * \param event Message event
 */
void spamprotection_on_message(struct discord *client, const struct discord_message *event);
