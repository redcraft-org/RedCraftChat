package org.redcraft.redcraftchat.models.caching;

import java.util.Arrays;
import java.util.List;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.SerializableModel;


public class AccountLinkCode extends SerializableModel {
    public String token;
    public String discordId;
    public String minecraftUuid;

    public AccountLinkCode(String token) {
        this.token = token;
    }

    public AccountLinkCode() {
        List<CharacterRule> rules = Arrays.asList(
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1));

        PasswordGenerator generator = new PasswordGenerator();

        do {
            this.token = generator.generatePassword(6, rules);
        } while (this.token == null || CacheManager.get(CacheCategory.ACCOUNT_LINK_CODE, this.token, String.class) != null);
    }
}
