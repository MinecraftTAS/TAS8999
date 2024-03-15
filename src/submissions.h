/**
 * \file submissions.h
 * This file adds the submissions module to the bot.
 * With /submit users can submit their own TAS Videos into three different categories.
 */

#pragma once

#include <concord/discord.h>

/**
 * Initialize submissions
 *
 * \param client Discord client
 * \param application_id Application ID
 */
void submissions_initialize(struct discord *client, u64snowflake application_id);

/**
 * On slash command
 *
 * \param client Discord client
 * \param event Interaction event
 */
void submissions_on_slash_command(struct discord *client, const struct discord_interaction *event);

/**
 * On interaction
 *
 * \param client Discord client
 * \param event Interaction event
 */
void submissions_on_interaction(struct discord *client, const struct discord_interaction *event);

/**
 * Deinitialize submissions
 */
void submissions_deinitialize();
