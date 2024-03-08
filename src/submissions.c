#include "submissions.h"

#include <concord/log.h>
#include <stdlib.h>
#include <string.h>

#define REQUIRES_CONFIRMATION_RESPONSE "Your submission has been posted to the moderators for confirmation. You will get notified via dm if your tas was rejected."
#define CONFIRMATION_RESPONSE "Your submission has been saved."
#define DEFAULT_COMMENT "No comment."
#define REJECTED_SUBMISSION "Your recent submission has been rejected.\nFeel free to resubmit it to #new-misc-things.\nYour submission:\n\n%s\n%s"
#define SUBMISSION "%s\n%s\nSubmitted by <@%ld>"

#define MISC_DESCRIPTION "Submit to #new-misc-things (does not require confirmation through moderator)"
#define TAS_DESCRIPTION "Submit to #new-tas-things"
#define TASBATTLE_DESCRIPTION "Submit to #tb-videos"

#define MISC_CHANNEL 816982394912374805ULL
#define TAS_CHANNEL 816274632025833475ULL
#define TASBATTLE_CHANNEL 816982092747505676ULL
#define CONFIRMATION_CHANNEL 888503048824037486ULL

void submissions_initialize(struct discord *client, u64snowflake application_id) {
    struct discord_application_command_options* options = &(struct discord_application_command_options) {
        .size = 2,
        .array = (struct discord_application_command_option[]) {
            {
                .type = DISCORD_APPLICATION_OPTION_STRING,
                .name = "url",
                .description = "Video URL",
                .required = true
            },
            {
                .type = DISCORD_APPLICATION_OPTION_STRING,
                .name = "comment",
                .description = "Optional comment",
                .required = false
            }
        }
    };

    struct discord_application_command_options* subcommands = &(struct discord_application_command_options) {
        .size = 3,
        .array = (struct discord_application_command_option[]) {
            {
                .type = DISCORD_APPLICATION_OPTION_SUB_COMMAND,
                .name = "misc",
                .description = MISC_DESCRIPTION,
                .options = options
            },
            {
                .type = DISCORD_APPLICATION_OPTION_SUB_COMMAND,
                .name = "tas",
                .description = TAS_DESCRIPTION,
                .options = options
            },
            {
                .type = DISCORD_APPLICATION_OPTION_SUB_COMMAND,
                .name = "tasbattle",
                .description = TASBATTLE_DESCRIPTION,
                .options = options
            }
        }
    };

    discord_create_global_application_command(client, application_id, &(struct discord_create_global_application_command) {
        .type = DISCORD_APPLICATION_CHAT_INPUT,
        .name = "submit",
        .description = "Submit a TAS",
        .default_permission = true,
        .options = subcommands
    }, NULL);
}

/**
 * Send a dm to the user
 *
 * \param client The discord client
 * \param response The response from the message creation
 * \param channel The channel to send the dm to
 */
static void send_dm(struct discord *client, struct discord_response *response, const struct discord_channel *channel) {
    discord_create_message(client, channel->id, &(struct discord_create_message) {
        .content = response->data
    }, NULL);
    free(response->data);
}

/**
 * Add a thread to the message to allow the user to discuss the submission
 *
 * \param client The discord client
 * \param response The response from the message creation
 * \param message The message that was created
 */
static void add_thread(struct discord *client, struct discord_response *response, const struct discord_message *message) {
    discord_start_thread_with_message(client, message->channel_id, message->id, &(struct discord_start_thread_with_message) {
        .name = "Discussion",
        .auto_archive_duration = 1440
    }, NULL);
}

/**
 * Add reactions to the message to allow the user to confirm or deny the submission
 *
 * \param client The discord client
 * \param response The response from the message creation
 * \param message The message that was created
 */
static void add_reactions(struct discord *client, struct discord_response *response, const struct discord_message *message) {
    discord_create_reaction(client, message->channel_id, message->id, 0, "\xE2\x9C\x85", NULL);
    discord_create_reaction(client, message->channel_id, message->id, 0, "\xE2\x9D\x8C", NULL);
}

/**
 * Approve a submission
 *
 * \param client The discord client
 * \param response The response from the message creation
 * \param message The message to approve
 */
static void approve_submission(struct discord *client, struct discord_response *response, const struct discord_message *message) {
    // check if message is from a bot
    if (!message->author->bot)
        return;

    // parse message
    char* content = message->content;
    char* comment = strtok(content, "\n");
    char* url = strtok(NULL, "\n");
    char* user_text = strtok(NULL, "\n");
    char* channel_text = strtok(NULL, "\n");
    if (!comment || !url || !user_text || !channel_text)
        return;

    strtok(user_text, "@");
    char* user = strtok(NULL, ">");
    strtok(channel_text, "#");
    char* channel = strtok(NULL, ">");
    if (!user || !channel)
        return;

    // send message in appropriate channel
    uint64_t user_id = atoll(channel);
    char new_message[4001];
    sprintf(new_message, SUBMISSION, comment, url, user_id);
    struct discord_ret_message ret = { .done = add_thread };
    discord_create_message(client, atoll(channel), &(struct discord_create_message) {
        .content = new_message
    }, &ret);

    // delete message
    discord_delete_message(client, message->channel_id, message->id, &(struct discord_delete_message) {
        .reason = "Approved submission"
    }, NULL);

    log_info("[SUBMISSIONS] Approved submission (%s) from %s", url, user);
}

/**
 * Reject a submission
 *
 * \param client The discord client
 * \param response The response from the message creation
 * \param message The message to reject
 */
static void reject_submission(struct discord *client, struct discord_response *response, const struct discord_message *message) {
    // check if message is from a bot
    if (!message->author->bot)
        return;

    // parse message
    char* content = message->content;
    char* comment = strtok(content, "\n");
    char* url = strtok(NULL, "\n");
    char* user_text = strtok(NULL, "\n");
    char* channel_text = strtok(NULL, "\n");
    if (!comment || !url || !user_text || !channel_text)
        return;

    strtok(user_text, "@");
    char* user = strtok(NULL, ">");
    strtok(channel_text, "#");
    char* channel = strtok(NULL, ">");
    if (!user || !channel)
        return;

    // send dm to user
    char* rejection_message = malloc(4001);
    snprintf(rejection_message, 4000, REJECTED_SUBMISSION, comment, url);
    struct discord_ret_channel ret = {
        .done = send_dm,
        .data = rejection_message
    };

    discord_create_dm(client, &(struct discord_create_dm) {
        .recipient_id = atoll(user)
    }, &ret);

    // delete message
    discord_delete_message(client, message->channel_id, message->id, &(struct discord_delete_message) {
        .reason = "Rejected submission"
    }, NULL);

    log_info("[SUBMISSIONS] Rejected submission (%s) from %s", url, user);
}

void submissions_on_reaction_add(struct discord *client, const struct discord_message_reaction_add *event) {
    // check if bot reacted
    if (event->member->user->bot)
        return;

    // check if in submission channel
    if (event->channel_id != CONFIRMATION_CHANNEL)
        return;

    // check if the reaction is an approval
    if (strcmp(event->emoji->name, "\xE2\x9C\x85") == 0) {
        struct discord_ret_message ret = { .done = approve_submission };
        discord_get_channel_message(client, event->channel_id, event->message_id, &ret);
    } else if (strcmp(event->emoji->name, "\xE2\x9D\x8C") == 0) {
        struct discord_ret_message ret = { .done = reject_submission };
        discord_get_channel_message(client, event->channel_id, event->message_id, &ret);
    }
}

void submissions_on_slash_command(struct discord *client, const struct discord_interaction *event) {
    if (strcmp(event->data->name, "submit") != 0)
        return;

    // get command parameters
    char* subcommand = event->data->options[0].array[0].name;
    char* url = event->data->options[0].array[0].options[0].array[0].value;
    char* comment = event->data->options[0].array[0].options[0].size == 2 ? event->data->options[0].array[0].options[0].array[1].value : DEFAULT_COMMENT;

    // sanitize comment
    if (strlen(comment) > 1000)
        comment[1000] = '\0';
    if (strlen(url) > 1000)
        url[1000] = '\0';

    // create message
    char message[4001];
    char* message_ptr = message + sprintf(message, SUBMISSION, comment, url, event->member->user->id);

    // adjust message based on subcommand and create misc message
    bool requires_confirmation = true;
    if (strcmp(subcommand, "misc") == 0) {
        requires_confirmation = false;
        struct discord_ret_message ret = { .done = add_thread };
        discord_create_message(client, MISC_CHANNEL, &(struct discord_create_message) {
            .content = message
        }, &ret);
        log_info("[SUBMISSIONS] Submitted misc tas (%s) from %s", url, event->member->user->username);
    } else if (strcmp(subcommand, "tas") == 0)
        sprintf(message_ptr, "\nSubmitted to: <#%lld>", TAS_CHANNEL);
    else
        sprintf(message_ptr, "\nSubmitted to: <#%lld>", TASBATTLE_CHANNEL);

    // send response
    discord_create_interaction_response(client, event->id, event->token, &(struct discord_interaction_response) {
        .type = DISCORD_INTERACTION_CHANNEL_MESSAGE_WITH_SOURCE,
        .data = &(struct discord_interaction_callback_data) {
            .content = requires_confirmation ? REQUIRES_CONFIRMATION_RESPONSE : CONFIRMATION_RESPONSE,
            .flags = DISCORD_MESSAGE_EPHEMERAL,
        }
    }, NULL);

    // send confirmation message
    if (requires_confirmation) {
        struct discord_ret_message ret = { .done = add_reactions };
        discord_create_message(client, CONFIRMATION_CHANNEL, &(struct discord_create_message) {
            .content = message,
        }, &ret);

        log_info("[SUBMISSIONS] Submitted tas (%s) from %s to confirmation", url, event->member->user->username);
    }
}

void submissions_deinitialize() {

}