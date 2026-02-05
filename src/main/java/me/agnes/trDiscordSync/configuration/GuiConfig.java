package me.agnes.trDiscordSync.configuration;

import com.bentahsin.configuration.annotation.ConfigPath;
import java.util.List;

public class GuiConfig {

    @ConfigPath("profile-menu")
    public ProfileMenuSection profileMenu = new ProfileMenuSection();

    public static class ProfileMenuSection {
        public String title = "ᴘʀᴏꜰɪʟ: &b%player%";
        public int size = 27;

        @ConfigPath("fill-item")
        public MenuItem fillItem = new MenuItem();

        public java.util.Map<String, MenuItem> items = new java.util.HashMap<>();
    }

    public static class MenuItem {
        public String material = "STONE";
        public String name = "";
        public List<String> lore = new java.util.ArrayList<>();
        public int slot = -1;
        public List<Integer> slots = new java.util.ArrayList<>();
    }
}
