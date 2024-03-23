/**
 * Copyright (C) 2024  Pancake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include <stdlib.h>
#include <signal.h>
#include <string.h>

#include <concord/discord.h>
#include <concord/log.h>

#include "customcommands.h"
#include "submissions.h"
#include "spamprotection.h"

/// Config file for the bot
#define CONFIG_FILE "config.json"

/**
 * Handle SIGINT signal and shut down the bot
 *
 * \param signum Signal number
 */
static void handle_sigint(int signum) {
    log_info("[TAS8999] Received SIGINT, shutting down bot...");
    ccord_shutdown_async();
}

/**
 * Initialize discord client
 *
 * \return Discord client on success, NULL on failure
 */
static struct discord* initialize_discord() {
    // initialize concord
    CCORDcode code = ccord_global_init();
    if (code) {
        log_trace("[TAS8999] ccord_global_init() failed: %d", code);

        return NULL;
    }
    log_trace("[TAS8999] ccord_global_init() success");

    // create discord client
    struct discord* client = discord_config_init(CONFIG_FILE);
    if (!client) {
        log_trace("[TAS8999] discord_create() failed");

        ccord_global_cleanup();
        return NULL;
    }
    log_trace("[TAS8999] discord_create() success");

    return client;
}

/**
 * Handle bot interaction
 *
 * \param client Discord client
 * \param event Interaction event
 */
static void on_interaction(struct discord *client, const struct discord_interaction *event) {
    if (event->type == DISCORD_INTERACTION_APPLICATION_COMMAND) {
        customcommands_on_slash_command(client, event);
        submissions_on_slash_command(client, event);
    } else if (event->type == DISCORD_INTERACTION_MESSAGE_COMPONENT) {
        submissions_on_interaction(client, event);
    }
}

/**
 * Main bot function
 *
 * \param client Discord client
 * \param event Ready event
 */
static void bot_main(struct discord *client, const struct discord_ready *event) {
    // get slash commands from custom commands module
    log_info("[TAS8999] Initializing custom slash commands...");
    command_list* custom_commands = customcommands_initialize(event->application->id);
    if (!custom_commands) {
        log_fatal("[TAS8999] Failed to initialize custom slash commands");

        discord_shutdown(client);
        return;
    }

    // update slash commands
    log_info("[TAS8999] Updating slash commands...");
    struct discord_application_command* commands = calloc(custom_commands->count, sizeof(struct discord_application_command));
    for (int i = 0; i < custom_commands->count; i++) commands[i] = *custom_commands->commands[i].command;

    discord_set_on_interaction_create(client, on_interaction);
    discord_bulk_overwrite_global_application_commands(client, event->application->id, &(struct discord_application_commands) {
        .size = custom_commands->count,
        .array = commands
    }, NULL);

    // cleanup
    free(commands);

    // add slash command from submissions module
    log_info("[TAS8999] Initializing submissions slash command...");
    submissions_initialize(client, event->application->id);
}

/**
 * Main function
 *
 * \return 0 on success, 1 on failure
 */
int main() {
    // initialize discord bot
    log_info("[TAS8999] Initializing tas8999 discord bot...");
    struct discord* client = initialize_discord();
    if (!client) {
        log_fatal("[TAS8999] Failed to initialize discord bot");

        return EXIT_FAILURE;
    }

    // run discord bot
    log_info("[TAS8999] Launching tas8999 discord bot...");
    signal(SIGINT, handle_sigint);
    discord_add_intents(client, DISCORD_GATEWAY_MESSAGE_CONTENT);
    discord_set_on_ready(client, bot_main);
    discord_set_on_message_create(client, spamprotection_on_message);
    CCORDcode code = discord_run(client);

    // cleanup discord bot
    log_info("[TAS8999] Discord bot exited (%d), cleaning up...", code);
    customcommands_deinitialize();
    submissions_deinitialize();
    discord_cleanup(client);
    ccord_global_cleanup();
    return EXIT_SUCCESS;
}
