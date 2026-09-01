# Translating RedCraftChat's own text

RedCraftChat translates two very different things. Player prose, which arrives at runtime and has to go through a translator. And its own interface text, which doesn't.
This documents where the plugin's own English lives today, how to finish translating it, and how to add a language.

## How it works

There are three tiers, and a string is in exactly one of them.

Hand-written translations live in `UiTranslations`. `localizeUiForPlayer` checks them first and returns immediately on a hit. No network, no cache, no cost, and the wording is whatever you decided it should be.

Machine translation is the fallback, and it's meant to stay. A string with no hand-written entry goes to the configured provider (Claude by default), gets cached, and `TranslationWarmer` pre-translates every constant in `UiStrings.ALL` at boot so the first player doesn't pay for it.

:warning: The fallback is a permanent part of the design, not a gap waiting to be closed. A language must work with zero hand-written strings, and it must work with some of them. Adding a language to `rcc_supported_locales` and nothing else has to leave you with a fully usable, fully translated interface, just a machine-translated one. Never make a hand-written entry a requirement for a language to function.

Untranslated is the third tier, and it's not a decision anyone made. Ten `sendInternalMessage` calls pass a raw English literal and never touch a localizer at all, so they're English for everyone forever.

Player prose is separate and stays that way. `localizeMessageForPlayer` returns a message untouched when the reader already understands the language it's in, which is right for a chat line and wrong for a menu. Keep mail bodies and private messages on that path.

## Where the plugin's English is

- 40 constants in `UiStrings`, which is the registry
- 13 of them have hand-written translations in 5 languages
- 27 still go to the machine
- 15 call sites re-type the English inline (14 distinct strings) instead of referencing the constant
- 10 sends never get localized at all
- `DISCORD_ALREADY_LINKED` is a constant but isn't in `ALL`, so it's never warmed

The re-typed literals are the sneaky one. They happen to match their constant character for character today, so the warmer's cache entry is hit by luck. Change a constant and the inline copy silently keeps the old text, still translated, still wrong.

## Why machines are bad at this

Not a general claim about translation quality. It's that a UI string is the worst possible input for one.

A menu string is translated alone, with nothing around it. `"Close"` is a verb on a button and an adjective everywhere else, and French can just as easily come back with "proche". We shipped `"Everything on the server gets translated into it"` and got `"Tout sur le serveur est traduit dedans"`, because the pronoun pointed at a question printed underneath that the translator never saw.

Some other things to keep in mind:

- Placeholders have to survive. `Keep %language%` is the only one today, and a translator that helpfully translates the placeholder name breaks the substitution.
- Minecraft formatting codes have to survive too. The Claude system prompt already covers `§` codes, so don't drop that instruction when you touch it.
- Length varies a lot. German and Russian run well past English, and the in-world panel has fixed pixel budgets, so a long translation gets ellipsized rather than wrapped forever.
- Endonyms are never translated. `LocaleManager.getEndonym` asks the JDK for a language's name in its own language, so "Français" stays "Français" in the German menu, which is the point.
- Terminology has to be consistent between strings. "Main language" and "primary language" are the same thing in the source and should be the same word in every target.

:warning: Machine translation is charged per call and cached forever. A cached bad translation is served for good, so if you change how translations are produced, clear the cache or nobody will ever see the improvement.

## The plan

The goal isn't a hand-written translation for every string. The machine does a decent job on whole sentences, which is why join and leave messages, restart notices and the rest read fine today. Where it falls down is short interface chrome, where there's no sentence to get context from.

So: hand-write the chrome, leave the prose to the machine, and stop paying to pre-translate what's already hand-written.

Five steps, in this order, because each one makes the next smaller.

#### Make the registry the only source of truth

Replace the 15 inline literals with their `UiStrings` constants, and add `DISCORD_ALREADY_LINKED` to `ALL`. Nothing changes for players. What changes is that every piece of interface text is now reachable from one place, which is what makes the rest of this checkable.

#### Localize the ten that never were

The mail command has seven, `/linkdiscord` two, `/settings` one. Each needs a constant in `UiStrings`, an entry in `ALL`, and a `localizeUiForPlayer` call. Watch `Mail sent to ` and `Current settings: `, which are prefixes concatenated with a value, so keep the trailing space in the constant or move the value into a placeholder.

#### Stop warming what's already hand-written

This is the token fix. `TranslationWarmer` walks `UiStrings.ALL` for every language and translates all of it, including the 13 strings that now have hand-written entries. Those cached results are never read, because `localizeUiForPlayer` checks the hand-written table first and returns before it ever looks at the cache.

Skip a string when `UiTranslations.lookup` already answers for that language. It's one condition in the inner loop, and it gets cheaper with every string you translate by hand.

#### Hand-write the chrome, not everything

Buttons, labels and fragments first. Those are the ones a machine gets wrong, because "Close" and "Legend:" and "enabled" carry no sentence to take context from, and they're also what players look at most.

Whole sentences can stay on the machine path as long as they read well. `You have no mails.` and `Please confirm the language you want to play in` are complete thoughts and translate fine. There's no prize for hand-writing those, and every one you add is a line somebody has to maintain.

#### Guard it with tests

`UiTranslationsTest` already checks that every language covers the buttons. Extend it so the build fails on:

- a button or label string missing from a language that has a table, since those are the ones the machine gets wrong
- a translation that still equals the English
- a translation missing a placeholder its source has
- a translation more than roughly twice the length of its source, which catches the ones that will be ellipsized in the panel

Notice what isn't on that list. Don't fail the build because a language lacks an entry for every constant, and don't fail it because a language has no table at all. Both are supported states, and a test that forbids them turns the fallback into a lie.

The length rule wants a generous threshold. It's there to catch a runaway, not to enforce brevity.

## How to add a language

Say you're adding Portuguese.

Add it to the database, since the supported locale list comes from `rcc_supported_locales` and not from the code:
`insert into rcc_supported_locales (code, name) values ('pt-BR', 'Portuguese');`

That's enough. The interface works in Portuguese from here, machine translated, and you can stop if that's good enough for you.

If you want the buttons hand-written, add a table in `UiTranslations.build()`, copying an existing block. The key is the language part only, so `pt`, not `pt-BR`. Regions don't get their own tables, because these words don't differ between Portugal and Brazil in any way that matters on a button.

You don't have to fill the whole table. Translate what you're sure of, leave the rest out, and every missing string keeps going to the machine as before.

Check the endonym looks right. `getEndonym` asks the JDK for the language's name in its own language and falls back to the `name` column when the JDK doesn't know it, so a bad `name` value only shows up for obscure locales. For `pt-BR` you'll get "Português".

Run the tests. If you missed a string, `UiTranslationsTest` tells you which one.

Restart the proxy. `TranslationWarmer` will pre-translate anything still on the machine path for the new language, which costs one round of provider calls, once.

:warning: Don't add a language to `UiTranslations` without adding it to the database. The table would just sit there unused, and you'd wonder why nothing changed (been there).

## How to add a string

- Add the constant to `UiStrings`
- Add it to `ALL` so the warmer picks it up
- Use `localizeUiForPlayer` for it, never `localizeMessageForPlayer`
- Add hand-written translations only if it's a button or a short label. A full sentence is fine on the machine path.

Write the English so it survives being read alone. No pronouns pointing at another string, no single words whose meaning comes from the screen around them. `Close menu` instead of `Close`. It costs a word and saves a mistranslation in every language you don't speak.

## Contributing

You are free to suggest changes by opening an issue ticket.

You can also open PRs, remember to bump the version in `pom.xml` before opening a pull request.

## Action bars

Two wire forms exist and they are different packets: the dedicated
`SET_ACTION_BAR_TEXT` most plugins emit through Adventure, and system chat
with the overlay flag, which is how CMI writes its bar. `ActionBarTranslator`
handles both, rewriting in place from a cache: a hit costs nothing on the
netty thread, a miss passes the original through and warms the cache so the
next repeat is translated. Counters collapse onto one template through
`NumericTemplate`, so a HUD repainting every tick costs one provider call
ever. A per-player churn guard stops new provider spending (never cache hits)
when more than eight uncached templates appear inside ten seconds.

`translate-action-bars` turns the whole path off, which also returns overlay
system chat to the grouping buffer it used before.

## Messages with clickable parts

Behind `upstream-translate-interactive`, off for one release while it soaks.
Every subtree carrying a click, hover or insertion is lifted out before
serialization and its visible text left inline between paired markers, so the
provider translates the sentence and the button labels together, with full
context:

    §6Click %click_a%here%end_a% to teleport

The events never travel: they wait on the proxy, keyed by marker name, and
are reattached to the translated label afterwards, so a provider cannot
corrupt a click. Restore validates the pair discipline string-level and
returns the untouched original on any violation, which is exactly what these
messages got before the feature existed. A WARN line logs every fallback;
watch it before flipping the default on.

## Clients before 1.19.1

Unsupported for system chat and action bar translation, deliberately. Those
clients receive the legacy chat packet, which nothing here listens for, and
thirty days of logs show no real player below 1.21.5 (the 1.7 and 1.8
traffic is scanners pinging and never connecting). Player-to-player chat is
unaffected: it is re-emitted through the Velocity API, which speaks every
client's own protocol.
