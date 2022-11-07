package org.redcraft.redcraftchat.runnables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class DiscordUsersSynchronizerTask implements Runnable {

    private static List<String> discordLpRoles = new ArrayList<String>();
    private static List<String> discordLocaleRoles = new ArrayList<String>();
    private static boolean warnedAboutRoleHierarchy = false;
    private static boolean warnedAboutNickHierarchy = false;

    public static boolean syncDiscordUser(Member member) {
        try {
            return syncDiscordUser(member, LuckPermsProvider.get());
        } catch (IllegalStateException e) {
            // LuckPerms not installed
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().severe("Error while syncing Discord user " + member.getUser().getName());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean syncDiscordUser(User user) {
        if (user == null) {
            return false;
        }
        for (Guild guild : DiscordClient.getClient().getGuilds()) {
            for (Member member : guild.getMembers()) {
                if (member.getUser().getId().equals(user.getId())) {
                    return syncDiscordUser(member);
                }
            }
        }

        return false;
    }

    public static boolean syncDiscordUser(Member member, LuckPerms lp) throws IOException, InterruptedException {
        PlayerPreferences player = PlayerPreferencesManager.getPlayerPreferences(member.getUser());

        List<Role> discordRolesForPlayer = new ArrayList<Role>();

        if (player != null && player.minecraftUuid != null) {
            net.luckperms.api.model.user.User lpUser = lp.getUserManager().getUser(player.minecraftUuid);
            if (lpUser == null) {
                lpUser = lp.getUserManager().loadUser(player.minecraftUuid).join();
            }
            if (lpUser == null) {
                String debugMessage = "Could not find LuckPerms user for " + member.getUser().getName() + " (" + player.minecraftUuid + ")";
                RedCraftChat.getInstance().getLogger().warning(debugMessage);
                return false;
            }

            lpUser.getCachedData().getPermissionData().getPermissionMap().forEach((permission, value) -> {
                Role discordRole = getDiscordRoleFromPermission(permission);
                if (discordRole != null && value) {
                    discordRolesForPlayer.add(discordRole);
                }
            });

            for (String locale : player.languages) {
                Role discordRole = getDiscordRoleFromLocale(locale);
                if (discordRole != null) {
                    discordRolesForPlayer.add(discordRole);
                }
            }
        }

        boolean updated = false;

        // Remove roles that the user should have
        List<Role> rolesToAdd = new ArrayList<Role>();
        for (Role discordRole : discordRolesForPlayer) {
            if (!member.getRoles().contains(discordRole)) {
                rolesToAdd.add(discordRole);
            }
        }

        // Add roles that the user should not have
        List<Role> rolesToRemove = new ArrayList<Role>();
        for (Role role : member.getRoles()) {
            if ((getDiscordLpRoles().contains(role.getId()) && !discordRolesForPlayer.contains(role)) ||
                (player != null && getDiscordLocaleRoles().contains(role.getId()) && !player.languages.contains(role.getName()))) {
                rolesToRemove.add(role);
            }
        }

        if (!rolesToAdd.isEmpty() || !rolesToRemove.isEmpty()) {
            try {
                member.getGuild().modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();
                String debugMessage = "Updating Discord user " + member.getUser().getName() + " roles (adding " + rolesToAdd + " and removing " + rolesToRemove + ")";
                RedCraftChat.getInstance().getLogger().info(debugMessage);
                updated = true;
            } catch (HierarchyException e) {
                // We can't edit, probably server owner
                if (!warnedAboutRoleHierarchy) {
                    RedCraftChat.getInstance().getLogger().warning("Could not update Discord user " + member.getUser().getName() + " roles due to hierarchy (probably server owner) - will not warn again");
                    warnedAboutRoleHierarchy = true;
                }
            }
        }

        if (player != null && player.lastKnownMinecraftName != null && !member.getEffectiveName().equals(player.lastKnownMinecraftName)) {
            try {
                member.modifyNickname(player.lastKnownMinecraftName).queue();
                updated = true;
            } catch (HierarchyException e) {
                // We can't edit, probably server owner
                if (!warnedAboutNickHierarchy) {
                    RedCraftChat.getInstance().getLogger().warning(
                        "Could not update Discord user " + member.getUser().getName() + " nickname to " + player.lastKnownMinecraftName + " due to hierarchy (probably server owner) - will not warn again");
                    warnedAboutNickHierarchy = true;
                }
            }
        }

        return updated;
    }

    public static Role getDiscordRoleFromPermission(String permission) {
        String permissionPrefix = "redcraftchat.discord-role.";
        if (permission.startsWith(permissionPrefix)) {
            String roleName = permission.substring(permissionPrefix.length());
            List<Role> matchingRoles = DiscordClient.getClient().getRolesByName(roleName, true);
            if (!matchingRoles.isEmpty()) {
                return matchingRoles.get(0);
            }
        }

        return null;
    }

    public static Role getDiscordRoleFromLocale(String locale) {
        List<Role> matchingRoles = DiscordClient.getClient().getRolesByName(locale, true);
        if (!matchingRoles.isEmpty()) {
            return matchingRoles.get(0);
        }

        return null;
    }

    public static List<String> getDiscordLpRoles() {
        if (!discordLpRoles.isEmpty()) {
            buildDiscordLpRoles();
        }
        return discordLpRoles;
    }

    public static void buildDiscordLpRoles() {
        try {
            List<String> updatedDiscordLpRoles = new ArrayList<String>();
            LuckPerms lp = LuckPermsProvider.get();
            lp.getGroupManager().getLoadedGroups().forEach(group -> {
                group.getCachedData().getPermissionData().getPermissionMap().forEach((permission, value) -> {
                    Role discordRole = getDiscordRoleFromPermission(permission);
                    if (discordRole != null && value && !updatedDiscordLpRoles.contains(discordRole.getId())) {
                        updatedDiscordLpRoles.add(discordRole.getId());
                    }
                });
            });
            if (discordLpRoles.size() != updatedDiscordLpRoles.size()) {
                RedCraftChat.getInstance().getLogger().fine("Discord LP groups updated. New detected linked roles: ");
                updatedDiscordLpRoles.forEach(role -> {
                    RedCraftChat.getInstance().getLogger().fine(" - " + role);
                });
                discordLpRoles = updatedDiscordLpRoles;
            }
        } catch (IllegalStateException e) {
            // LuckPerms not installed
        }
    }

    public static List<String> getDiscordLocaleRoles() {
        if (discordLocaleRoles.isEmpty()) {
            buildDiscordLocaleRoles();
        }
        return discordLocaleRoles;
    }

    public static void buildDiscordLocaleRoles() {
        try {
            List<String> updatedDiscordLocaleRoles = new ArrayList<String>();

            for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
                Role discordRole = getDiscordRoleFromLocale(locale.code);
                if (discordRole != null && !updatedDiscordLocaleRoles.contains(discordRole.getId())) {
                    updatedDiscordLocaleRoles.add(discordRole.getId());
                }
            }

            if (discordLocaleRoles.size() != updatedDiscordLocaleRoles.size()) {
                RedCraftChat.getInstance().getLogger().fine("Discord Locale groups updated. New detected linked roles: ");
                updatedDiscordLocaleRoles.forEach(role -> {
                    RedCraftChat.getInstance().getLogger().fine(" - " + role);
                });
                discordLocaleRoles = updatedDiscordLocaleRoles;
            }
        } catch (IllegalStateException e) {
            // LuckPerms not installed
        }
    }

    public void run() {
        buildDiscordLpRoles();
        DiscordClient.getClient().getGuilds().forEach(guild -> {
            guild.getMembers().forEach(member -> {
                if (member.getUser().isBot()) {
                    return;
                }
                try {
                    if (syncDiscordUser(member)) {
                        RedCraftChat.getInstance().getLogger().info("Updated Discord user " + member.getUser().getName());
                    }
                } catch (Exception e) {
                    RedCraftChat.getInstance().getLogger().severe("Error while syncing Discord user " + member.getUser().getName());
                    e.printStackTrace();
                }
            });
        });
    }

}
