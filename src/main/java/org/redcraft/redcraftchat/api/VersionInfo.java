package org.redcraft.redcraftchat.api;

import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.network.ProtocolVersion;

/**
 * The client versions the proxy accepts, in the shape the website already
 * reads.
 */
public class VersionInfo {

    public String serverSoftware;
    public String mainVersion;
    public ArrayList<String> supportedVersions;

    public VersionInfo(String serverSoftware, String mainVersion, ArrayList<String> supportedVersions) {
        this.serverSoftware = serverSoftware;
        this.mainVersion = mainVersion;
        this.supportedVersions = supportedVersions;
    }

    public static VersionInfo build() {
        String serverSoftware = RedCraftChat.getInstance().getProxy().getVersion().getName();

        ArrayList<String> supportedVersions = new ArrayList<String>();

        // Velocity knows the range it speaks, every entry carries the names of
        // the releases sharing that protocol
        for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
            List<String> names = version.getVersionsSupportedBy();

            for (String name : names) {
                if (!supportedVersions.contains(name)) {
                    supportedVersions.add(name);
                }
            }
        }

        String mainVersion = supportedVersions.isEmpty()
                ? ProtocolVersion.MAXIMUM_VERSION.getVersionIntroducedIn()
                : supportedVersions.get(supportedVersions.size() - 1);

        return new VersionInfo(serverSoftware, mainVersion, supportedVersions);
    }

    public String getVersionsJson() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}
