package com.github.nickxgrom.traceBrush;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;

public class CoreProtectHook {
    private final CoreProtectAPI coreProtectAPI;

    public CoreProtectHook() {
        this.coreProtectAPI = loadCoreProtectAPI();
    }

    private CoreProtectAPI loadCoreProtectAPI() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI api = ((CoreProtect) plugin).getAPI();
        if (!api.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (api.APIVersion() < 10) {
            return null;
        }

        return api;
    }

    public CoreProtectAPI getCoreProtectAPI() {
        return coreProtectAPI;
    }
}