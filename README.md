
# RedCraftChat

A chat plugin for Velocity that does live translation and much more.

:warning: This plugin is in a very early alpha state, and should be treated as such. A lot of the documentation is currently missing, don't expect any support. :warning:

## Requirements

- Velocity 4, the plugin is built against the 4.x API and won't load on a 3.x proxy
- Java 25
- A MySQL or MariaDB database, it's where player preferences, mails and supported locales live
- Redis, only if you set `cache-provider` to redis

## Build

`mvn package -B`

## How it works

The proxy cancels the message a player sends and re-emits its own copy to everyone, translated per recipient.
That's why every chat line you see comes from the proxy and not from the backend server.

Because a signed message can't be cancelled, the signature is stripped from incoming chat before Velocity reads it.
This is what `strip-chat-signatures` does, and turning it off breaks chat on 1.19 and newer clients.

Messages coming from a backend server are intercepted on their way out and translated the same way, so a plugin message on the survival server reaches a French player in French.

Keep in mind that a message carrying a click event is forwarded untouched.
Translating it would flatten it and the menu would stop being clickable.

## Config

Everything lives in `config.yml`, in the `plugins/redcraftchat` folder of your proxy (Velocity names that folder after the plugin id, not after the jar).

### Translation

- `translation-enabled` turns the whole thing on and off. When it's off, messages are still relayed, just not translated.
- `chat-translation-provider` is what translates player chat, `upstream-translation-provider` does the same for messages coming from backend servers. Both accept `claude`, `deepl`, `modernmt` and `modernmt-free`.
- `pretranslate-ui-enabled` translates every menu and button into each supported language on startup. The cache never expires, so it costs one pass the first time and nothing after.

By default the provider is `claude`, because the free ModernMT endpoint stopped returning translations.
Set `claude-token` and you're done, `claude-model` defaults to a Haiku model which is fast enough for chat.

:warning: A provider that fails doesn't lose the message, the original text is sent instead. Look for stack traces in your proxy log if everything suddenly arrives untranslated.

### Server names

- `server-display-names` maps a server name to what players should see: `hub: '&6Hub'`. It's used in join and leave messages, in the chat prefix and in the Discord bridge.

If a server isn't listed, the plugin pings it and uses the first line of its motd, and falls back to the name the proxy registers it under.
Please notice that a motd can't carry colours, so list a server here if you want it coloured.

### Discord

- `discord-enabled` and `discord-token` are all you need to bridge chat. When the token is empty the bridge is skipped and the rest of the plugin still works.
- `discord-channel-minecraft` is the channel that mirrors in game chat.

The bot invite link is `https://discord.com/oauth2/authorize?client_id=<client_id>&scope=bot&permissions=8`

### Storage

- `database-uri`, `database-username` and `database-password` point at your database. `player-provider`, `mail-provider`, `supported-locales-provider` and `scheduled-announcements-provider` each accept `database` or `api`.
- `cache-provider` is `memory` or `redis`. Use redis if you run more than one proxy, otherwise memory is fine.

:warning: :warning: :warning: The supported locales table has to contain at least one locale, an empty table means nobody can be given a language and chat will look broken.

## Commands

- `/lang` opens the language selector, click a language to enable it and the checkbox to make it your main one
- `/msg`, `/r` and `/me` are the usual ones, they go through the proxy so they work across servers
- `/mail` is a small inbox, `/mail send <player> <message>` sends one
- `/commandspy` shows the commands other players run, `/broadcast` sends a message to the whole network
- `/redcraftchat` is the admin command

## Contributing

You are free to suggest changes by opening an issue ticket.

You can also open PRs, remember to bump the version in `pom.xml` before opening a pull request.
