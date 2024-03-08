#include "customcommands.h"

#include <concord/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <dirent.h>
#include <errno.h>

#define CUSTOMCOMMANDS_DIR "customcommands"

static command_list *commands = NULL;

command_list* customcommands_initialize(u64snowflake application_id) {
    // check if the custom commands directory exists
    DIR *dir = opendir(CUSTOMCOMMANDS_DIR);
    if (!dir) {
        log_trace("[CUSTOMCOMMANDS] opendir() failed: %s", strerror(errno));
        return NULL;
    }
    log_trace("[CUSTOMCOMMANDS] opendir() success");

    // allocate memory for the command list
    commands = calloc(1, sizeof(command_list));
    if (!commands) {
        log_trace("[CUSTOMCOMMANDS] malloc() failed");

        closedir(dir);
        return NULL;
    }
    log_trace("[CUSTOMCOMMANDS] malloc() success");

    // load each json file in the custom commands directory
    log_debug("[CUSTOMCOMMANDS] Loading custom commands...");
    char filename[512];
    char description[512];
    struct dirent *entry;
    while ((entry = readdir(dir))) {
        // '.json' is 5 characters long
        int name_length = strlen(entry->d_name);
        if (name_length <= 5)
            continue;

        // check if the file ends with '.json'
        if (strcmp(entry->d_name + name_length - 5, ".json") != 0)
            continue;

        // open the file
        sprintf(filename, "%s/%s", CUSTOMCOMMANDS_DIR, entry->d_name);
        FILE *file = fopen(filename, "r");
        if (!file) {
            log_trace("[CUSTOMCOMMANDS] fopen() failed: %s", strerror(errno));

            closedir(dir);
            continue;
        }
        log_trace("[CUSTOMCOMMANDS] fopen() success");

        // get the file size
        struct stat st;
        stat(filename, &st);
        size_t size = st.st_size;

        // read the file
        char *data = malloc(size);
        if (!data) {
            log_trace("[CUSTOMCOMMANDS] malloc() failed");

            fclose(file);
            closedir(dir);
            return NULL;
        }
        log_trace("[CUSTOMCOMMANDS] malloc() success");
        fread(data, 1, size, file);

        // close the file
        fclose(file);

        // parse the json
        struct discord_embed *embed = calloc(1, sizeof(struct discord_embed));
        discord_embed_from_json(data, size, embed);

        // allocate memory for the new command
        commands->commands = realloc(commands->commands, (commands->count + 1) * sizeof(struct discord_application_command));
        if (!commands->commands) {
            log_trace("[CUSTOMCOMMANDS] malloc() failed");

            free(data);
            closedir(dir);
            return NULL;
        }
        log_trace("[CUSTOMCOMMANDS] malloc() success");

        // create the command
        struct discord_application_command *command = calloc(1, sizeof(struct discord_application_command));
        command->type = DISCORD_APPLICATION_CHAT_INPUT;
        entry->d_name[name_length - 5] = '\0'; // remove '.json' from the name
        command->name = strdup(entry->d_name);
        sprintf(description, "Learn more about %s", entry->d_name);
        command->description = strdup(description);
        command->default_permission = true;
        command->application_id = application_id;

        // cleanup
        free(data);

        // fill structure
        commands->commands[commands->count].name = command->name;
        commands->commands[commands->count].description = command->description;
        commands->commands[commands->count].embed = embed;
        commands->commands[commands->count].command = command;
        commands->count++;
    }

    // cleanup
    closedir(dir);
    return commands;
}

void customcommands_on_slash_command(struct discord *client, const struct discord_interaction *event) {
    if (!commands)
        return;

    // find the command
    custom_command *command = NULL;
    for (int i = 0; i < commands->count; i++) {
        if (strcmp(event->data->name, commands->commands[i].name) == 0) {
            command = &commands->commands[i];
            break;
        }
    }

    if (!command)
        return;

    // send the embed
    discord_create_interaction_response(client, event->id, event->token, &(struct discord_interaction_response) {
        .type = DISCORD_INTERACTION_CHANNEL_MESSAGE_WITH_SOURCE,
        .data = &(struct discord_interaction_callback_data) {
            .content = "",
            .embeds = &(struct discord_embeds) {
                .array = command->embed,
                .size = 1
            }
        }
    }, NULL);
}

void customcommands_deinitialize() {
    if (!commands)
        return;

    for (int i = 0; i < commands->count; i++) {
        free(commands->commands[i].name);
        free(commands->commands[i].description);
        discord_embed_cleanup(commands->commands[i].embed);
        free(commands->commands[i].embed);
        free(commands->commands[i].command);
    }

    free(commands->commands);
    free(commands);
    commands = NULL;
}