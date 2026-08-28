package org.redcraft.redcraftchat.commands;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;

/**
 * Builds the Brigadier nodes handed to CommandMeta as hints.
 *
 * Hints are the only completion a Bedrock player ever gets. Velocity copies
 * them into the declared command tree unfiltered for any InvocableCommand,
 * and Geyser turns each literal into an enum value the Bedrock client can
 * offer offline. They never execute: Velocity's own dispatcher skips them and
 * falls through to the command's greedy argument node, so adding a hint
 * cannot change how anything parses.
 *
 * Two rules the shapes here follow, both forced by the Bedrock side:
 *
 * One verb, one arity. A verb that is valid both bare and with a tail is
 * required-with-a-tail on Bedrock and paints the rest of the line red on
 * Java. Where the legacy grammar has that shape it is kept working in
 * execute() and simply never advertised.
 *
 * Every choice must be a literal. An argument node is a free-text box on
 * Bedrock no matter what type it claims, so anything a player picks from a
 * known set is spelled out.
 */
public final class CommandHints {

    private CommandHints() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /** A childless literal, the shape that becomes one Bedrock enum value. */
    public static CommandNode<CommandSource> leaf(String name) {
        return BrigadierCommand.<CommandSource>literalArgumentBuilder(name).build();
    }

    /** A literal whose children are the values it accepts. */
    public static CommandNode<CommandSource> verbWith(String name, List<String> values) {
        LiteralArgumentBuilder<CommandSource> builder = BrigadierCommand.literalArgumentBuilder(name);
        for (String value : values) {
            builder.then(BrigadierCommand.<CommandSource>literalArgumentBuilder(value));
        }
        return builder.build();
    }

    /**
     * A literal taking free text.
     *
     * The greedy tail is what stops a Java client painting the rest of the
     * line red once the verb itself is a known literal. Bedrock sees it as a
     * plain text box, which is the best available there.
     */
    public static CommandNode<CommandSource> verbWithText(String name, String argument) {
        return BrigadierCommand.<CommandSource>literalArgumentBuilder(name)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument(
                        argument, StringArgumentType.greedyString()))
                .build();
    }

    /** A literal taking one word, then free text: the send-to-somebody shape. */
    public static CommandNode<CommandSource> verbWithWordThenText(String name, String word, String text) {
        return BrigadierCommand.<CommandSource>literalArgumentBuilder(name)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument(word, StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument(
                                text, StringArgumentType.greedyString())))
                .build();
    }

    /** A bare argument for a command that is only ever free text. */
    public static CommandNode<CommandSource> text(String argument) {
        return RequiredArgumentBuilder.<CommandSource, String>argument(
                argument, StringArgumentType.greedyString()).build();
    }

    /** A bare single-word argument, for an optional player name. */
    public static CommandNode<CommandSource> word(String argument) {
        return RequiredArgumentBuilder.<CommandSource, String>argument(
                argument, StringArgumentType.word()).build();
    }
}
