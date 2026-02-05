package me.agnes.trDiscordSync.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.annotation.*;
import me.agnes.trDiscordSync.trDiscordSync;
import me.agnes.trDiscordSync.data.EslestirmeManager;
import me.agnes.trDiscordSync.discord.DiscordBot;
import me.agnes.trDiscordSync.util.LuckPermsUtil;
import me.agnes.trDiscordSync.util.MessageUtil;
import me.agnes.trDiscordSync.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CommandAlias("%main_cmd")
@Description("Main trDiscordSync integration command.")
public class EsleCommandACF extends BaseCommand {
    private final trDiscordSync plugin;

    public EsleCommandACF(trDiscordSync plugin) {
        this.plugin = plugin;
    }

    private void playSuccess(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void playError(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_esle")
    @Description("Generates a code to link with your Discord account.")
    public void onEsle(Player player) {
        if (EslestirmeManager.beklemeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-zaten-bekliyor");
            return;
        }
        if (EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "hesap-zaten-eslesmis");
            return;
        }

        String kod = EslestirmeManager.uretKod(player.getUniqueId());
        playSuccess(player);
        Map<String, String> vars = new HashMap<>();
        vars.put("kod", kod);
        MessageUtil.sendTitle(player, "kod-verildi", vars);

        player.sendMessage(MessageUtil.getMessage("kod-verildi.message", vars));
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_iptal")
    @Description("Cancels your pending linking code.")
    public void onIptal(Player player) {
        if (!EslestirmeManager.beklemeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-onayi-beklemiyor");
            return;
        }
        if (!EslestirmeManager.iptalEt(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-iptal-edilemedi");
            return;
        }
        playSuccess(player);
        MessageUtil.sendTitle(player, "hesap-esle-kodiptal");
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_onayla")
    @Description("Confirms the linking request from Discord.")
    public void onOnayla(BukkitCommandIssuer issuer) {
        Player player = issuer.getPlayer();
        if (player == null)
            return;

        if (!EslestirmeManager.beklemeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-onayi-beklemiyor");
            return;
        }

        boolean ilkEslesme = !EslestirmeManager.eslesmeVar(player.getUniqueId());
        String ipAdresi = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        boolean onaylandi = EslestirmeManager.onaylaEslesme(player.getUniqueId(), ipAdresi);

        if (!onaylandi) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-onaylanamadi");
            return;
        }

        playSuccess(player);
        MessageUtil.sendTitle(player, "eslesme-basariyla-tamamlandi");

        if (ilkEslesme) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5,
                    0.5);

            SchedulerUtil.runAsync(() -> {
                String discordId = EslestirmeManager.getDiscordId(player.getUniqueId());
                if (discordId != null) {
                    DiscordBot bot = trDiscordSync.getInstance().getDiscordBot();
                    bot.changeNickname(discordId, player.getName());

                    LuckPermsUtil lpUtil = plugin.getLuckPermsUtil();
                    if (lpUtil != null) {
                        String group = lpUtil.getPrimaryGroup(player.getUniqueId());
                        if (group != null) {
                            Map<String, String> rolesMap = trDiscordSync.getInstance().getMainConfig().roles;

                            if (rolesMap != null) {
                                for (Map.Entry<String, String> entry : rolesMap.entrySet()) {
                                    String roleName = entry.getKey();
                                    String roleId = entry.getValue();

                                    if (roleId == null || roleId.isEmpty())
                                        continue;

                                    if (group.equalsIgnoreCase(roleName)) {
                                        bot.addRoleToMember(discordId, roleId);
                                        plugin.getLogger().info(player.getName() + " oyuncusuna " + roleName
                                                + " Discord rolü verildi.");
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @SuppressWarnings("unused")
    @Subcommand("profile")
    @Description("Displays your own profile or another player's profile.")
    @Syntax("[oyuncu]")
    @CommandCompletion("@players")
    public void onProfil(Player sender, @Optional OfflinePlayer target) {
        if (target == null) {
            plugin.getProfileMenu().open(sender, sender);
            playSuccess(sender);
            return;
        }

        if (!sender.hasPermission("trDiscordSync.admin")) {
            playError(sender);
            sender.sendMessage(MessageUtil.getMessage("yetki-yok"));
            return;
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            playError(sender);
            MessageUtil.sendTitle(sender, "oyuncu-bulunamadi");
            return;
        }

        plugin.getProfileMenu().open(sender, target);
        playSuccess(sender);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_kaldir")
    @Description("Removes your existing account link.")
    public void onKaldir(Player player) {
        if (!EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            playError(player);
            MessageUtil.sendTitle(player, "eslesme-yok");
            return;
        }
        EslestirmeManager.kaldirEslesme(player.getUniqueId());
        playSuccess(player);
        MessageUtil.sendTitle(player, "kaldirildi");
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_2fa")
    @CommandCompletion("on|off")
    @Description("Manages two-factor authentication.")
    public void on2fa(Player player, String durum) {
        if (!EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            playError(player);
            player.sendMessage(MessageUtil.getMessage("2fa-not-linked"));
            return;
        }

        durum = durum.toLowerCase();
        if (durum.equals("on") || durum.equals("aç") || durum.equals("ac")) {
            if (EslestirmeManager.isIkiFAOpen(player.getUniqueId())) {
                player.sendMessage(MessageUtil.getMessage("2fa-already-enabled"));
            } else {
                EslestirmeManager.setIkiFA(player.getUniqueId(), true);
                player.sendMessage(MessageUtil.getMessage("2fa-successfully-enabled"));
                playSuccess(player);
            }
        } else if (durum.equals("off") || durum.equals("kapat") || durum.equals("kapa")) {
            if (!EslestirmeManager.isIkiFAOpen(player.getUniqueId())) {
                player.sendMessage(MessageUtil.getMessage("2fa-already-disabled"));
            } else {
                EslestirmeManager.setIkiFA(player.getUniqueId(), false);
                player.sendMessage(MessageUtil.getMessage("2fa-successfully-disabled"));
                playSuccess(player);
            }
        } else {
            playError(player);
            player.sendMessage(MessageUtil.getMessage("2fa-invalid-subcommand"));
        }
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_liste")
    @CommandPermission("trDiscordSync.admin")
    @Description("Lists all linked players.")
    @Syntax("[sayfa]")
    @CommandCompletion("@nothing")
    public void onListe(Player sender, @Default("1") int page) {
        final int PAGE_SIZE = 10;
        Map<UUID, String> eslesmeler = EslestirmeManager.getTumEslesmeler();
        if (eslesmeler.isEmpty()) {
            playError(sender);
            sender.sendMessage(MessageUtil.getMessage("no-matches-yet"));
            return;
        }

        int toplam = eslesmeler.size();
        int maxPage = (toplam + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(1, Math.min(page, maxPage));

        Map<String, String> vars = new HashMap<>();
        vars.put("page", String.valueOf(page));
        vars.put("maxPage", String.valueOf(maxPage));
        sender.sendMessage(MessageUtil.getMessage("list-header", vars));

        UUID[] uuids = eslesmeler.keySet().toArray(new UUID[0]);
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, toplam);
        for (int i = start; i < end; i++) {
            UUID uuid = uuids[i];
            String id = eslesmeler.get(uuid);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String ad = op.getName() != null ? op.getName() : MessageUtil.getMessage("unknown-player");

            vars.clear();
            vars.put("player", ad);
            vars.put("discordId", id);
            sender.sendMessage(MessageUtil.getMessage("list-entry", vars));
        }
        if (maxPage > 1)
            sender.sendMessage(MessageUtil.getMessage("list-footer"));
        playSuccess(sender);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_sifirla")
    @CommandPermission("trDiscordSync.admin")
    @Description("Resets a player's account link.")
    @CommandCompletion("@players")
    public void onSifirla(Player sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore()) {
            playError(sender);
            MessageUtil.sendTitle(sender, "oyuncu-bulunamadi");
            return;
        }
        if (!EslestirmeManager.eslesmeVar(target.getUniqueId())) {
            playError(sender);
            MessageUtil.sendTitle(sender, "eslesme-yok");
            return;
        }

        EslestirmeManager.kaldirEslesme(target.getUniqueId());
        playSuccess(sender);
        Map<String, String> vars = new HashMap<>();
        vars.put("player", target.getName() != null ? target.getName() : MessageUtil.getMessage("unknown-player"));
        sender.sendMessage(MessageUtil.getMessage("player-match-reset", vars));
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_odul")
    @CommandPermission("trDiscordSync.admin")
    @Description("Manually gives the linking reward to a player.")
    @CommandCompletion("@players")
    public void onOdul(Player sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore()) {
            playError(sender);
            MessageUtil.sendTitle(sender, "oyuncu-bulunamadi");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (!EslestirmeManager.eslesmeVar(targetUUID)) {
            sender.sendMessage(MessageUtil.getMessage("odul-not-linked"));
            return;
        }

        if (EslestirmeManager.odulVerildiMi(targetUUID)) {
            sender.sendMessage(MessageUtil.getMessage("odul-already-given"));
            return;
        }

        trDiscordSync.getInstance().odulVer(targetUUID);
        EslestirmeManager.odulVerildi(targetUUID);

        Map<String, String> vars = new HashMap<>();
        vars.put("player", target.getName());
        sender.sendMessage(MessageUtil.getMessage("odul-given-success", vars));
        playSuccess(sender);
    }

    @SuppressWarnings("unused")
    @Subcommand("%sub_yenile")
    @CommandPermission("trDiscordSync.admin")
    @Description("Reloads configurations and language files.")
    public void onYenile(Player sender) {
        MessageUtil.yenile();
        playSuccess(sender);
        MessageUtil.sendTitle(sender, "yenilendi");
        plugin.reloadConfig();
    }

    @SuppressWarnings("unused")
    @HelpCommand
    @Syntax("[help]")
    public void onHelp(Player sender) {
        sender.sendMessage(MessageUtil.getMessage("help-header"));

        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
            {
                put("command", "link");
                put("syntax", "");
                put("description", "Generate a linking code.");
            }
        }));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
            {
                put("command", "confirm");
                put("syntax", "");
                put("description", "Confirm the linking request.");
            }
        }));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
            {
                put("command", "cancel");
                put("syntax", "");
                put("description", "Cancel your pending code.");
            }
        }));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
            {
                put("command", "unlink");
                put("syntax", "");
                put("description", "Remove your account link.");
            }
        }));
        sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
            {
                put("command", "2fa");
                put("syntax", "<on|off>");
                put("description", "Manage 2FA security.");
            }
        }));

        if (sender.hasPermission("trDiscordSync.admin")) {
            sender.sendMessage(" ");
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
                {
                    put("command", "list");
                    put("syntax", "[page]");
                    put("description", "List all account links.");
                }
            }));
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
                {
                    put("command", "reset");
                    put("syntax", "<player>");
                    put("description", "Reset a player's link.");
                }
            }));
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
                {
                    put("command", "reward");
                    put("syntax", "<player>");
                    put("description", "Give manual reward.");
                }
            }));
            sender.sendMessage(MessageUtil.getMessage("help-format", new HashMap<String, String>() {
                {
                    put("command", "reload");
                    put("syntax", "");
                    put("description", "Reload the plugin.");
                }
            }));
        }

        sender.sendMessage(MessageUtil.getMessage("help-footer"));
    }
}