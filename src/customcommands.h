/**
 * \file customcommands.h
 * This file adds the custom commands module to the bot.
 * With the help of this module, simple json files can be turned into slash commands for the bot.
 */

#pragma once

#include <concord/discord.h>

/// Custom command structure
typedef struct {
    char *name; //!< Command name
    char *description; //!< Command description
    struct discord_embed* embed; //!< Command embed
} custom_command;

/// Command list
typedef struct {
    custom_command *commands; //!< Array of commands
    int count; //!< Number of commands
} command_list;

/**
 * Initialize custom commands
 *
 * \param commands Commands array
 * \param application_id Application ID
 *
 * \return Amount of commands initialized, -1 on failure
 */
int customcommands_initialize(struct discord_application_command* commands, u64snowflake application_id);

/**
 * On slash command
 *
 * \param client Discord client
 * \param event Interaction event
 */
void customcommands_on_slash_command(struct discord *client, const struct discord_interaction *event);

/**
 * Deinitialize custom commands
 */
void customcommands_deinitialize();
