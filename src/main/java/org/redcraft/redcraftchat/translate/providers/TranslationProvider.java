package org.redcraft.redcraftchat.translate.providers;

import java.io.IOException;
import java.net.URISyntaxException;

public interface TranslationProvider {
    public String translate(String text, String sourceLanguageId, String targetLanguageId) throws IllegalStateException, URISyntaxException, IOException, InterruptedException;

    /**
     * Whether this provider is given the message as it stands, colour codes,
     * placeholders, line breaks and all.
     *
     * The tokenizer exists because a machine translation engine will happily
     * translate a colour code or reflow a URL, so those are swapped for opaque
     * hashes first. An instruction following model can simply be told what to
     * leave alone, and reads the message far better for it: the hashes land
     * glued to neighbouring words, and a tokenized line break is invisible as
     * a line break, which is exactly the structure a multi line message needs
     * the model to see.
     */
    public default boolean translatesRawText() {
        return false;
    }
}
