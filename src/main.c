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

/// Config file for the bot
#define CONFIG_FILE "config.json"

/// Discord client instance
static struct discord *discord_client = NULL;

/**
 * Handle SIGINT signal and shut down the bot
 *
 * \param signum Signal number
 */
static void handle_sigint(int signum) {
    log_info("[TAS8999] Received SIGINT, shutting down bot...");
    if (discord_client) discord_shutdown(discord_client);
}

/**
 * Initialize discord client
 *
 * \return 0 on success, 1 on failure
 */
static int initialize_discord() {
    // initialize concord
    CCORDcode code = ccord_global_init();
    if (code) {
        log_trace("[TAS8999] ccord_global_init() failed: %d", code);

        return 1;
    }
    log_trace("[TAS8999] ccord_global_init() success");

    // create discord client
    discord_client = discord_config_init(CONFIG_FILE);
    if (!discord_client) {
        log_trace("[TAS8999] discord_create() failed");

        ccord_global_cleanup();
        return 1;
    }
    log_trace("[TAS8999] discord_create() success");

    return 0;
}

/**
 * Handle slash command interaction
 *
 * \param client Discord client
 * \param event Interaction event
 */
static void on_slash_command(struct discord *client, const struct discord_interaction *event) {

}

/**
 * Main bot function
 *
 * \param client Discord client
 * \param event Ready event
 */
void bot_main(struct discord *client, const struct discord_ready *event) {
    // initialize global slash commands
    log_info("[TAS8999] Initializing global slash commands...");
    discord_set_on_interaction_create(client, on_slash_command);
}

/**
 * Main function
 *
 * \return 0 on success, 1 on failure
 */
int main() {
    // initialize discord bot
    log_info("[TAS8999] Initializing tas8999 discord bot...");
    if (initialize_discord()) {
        log_fatal("[TAS8999] Failed to initialize discord bot");

        return EXIT_FAILURE;
    }

    // run discord bot
    log_info("[TAS8999] Launching tas8999 discord bot...");
    signal(SIGINT, handle_sigint);
    discord_set_on_ready(discord_client, bot_main);
    CCORDcode code = discord_run(discord_client);

    // cleanup discord bot
    log_info("[TAS8999] Discord bot exited (%d), cleaning up...", code);
    discord_cleanup(discord_client);
    ccord_global_cleanup();
    return EXIT_SUCCESS;
}
