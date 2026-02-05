package me.agnes.trDiscordSync.data;

import me.agnes.trDiscordSync.trDiscordSync;
import me.agnes.trDiscordSync.discord.DiscordBot;
import me.agnes.trDiscordSync.util.SchedulerUtil;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EslestirmeManager {

    private static final Logger logger = trDiscordSync.getInstance().getLogger();

    private static final Map<String, UUID> kodlar = new ConcurrentHashMap<>();
    private static final Map<UUID, String> eslesmeler = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> boosterZamanlari = new ConcurrentHashMap<>();
    private static final Map<UUID, String> bekleyenEslesmeler = new ConcurrentHashMap<>();
    private static final Map<String, UUID> bekleyenKodlar = new ConcurrentHashMap<>();
    private static final Map<String, Long> kodZamanlari = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> eslesmeZamanlari = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ikiFADurumu = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> odulVerildiMap = new ConcurrentHashMap<>();
    private static final Map<UUID, String> kayitliIPler = new ConcurrentHashMap<>();

    public static void init() {
        loadEslesmeler();

        SchedulerUtil.runTimerAsync(() -> {
            long now = System.currentTimeMillis();
            long expirationTime = 10 * 60 * 1000;

            Iterator<Map.Entry<String, Long>> it = kodZamanlari.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if ((now - entry.getValue()) > expirationTime) {
                    String kod = entry.getKey();
                    kodlar.remove(kod);
                    bekleyenKodlar.remove(kod);
                    it.remove();
                }
            }
        }, 1200L, 1200L);
    }

    public static long getEslesmeTarihi(UUID uuid) {
        Long time = eslesmeZamanlari.get(uuid);
        if (time != null)
            return time;
        return -1L;
    }

    public static String uretKod(UUID uuid) {
        String kod = null;
        int attempts = 0;
        do {
            kod = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            attempts++;
        } while ((kodlar.containsKey(kod) || bekleyenKodlar.containsKey(kod)) && attempts < 5);

        kodlar.put(kod, uuid);
        bekleyenKodlar.put(kod, uuid);
        kodZamanlari.put(kod, System.currentTimeMillis());
        return kod;
    }

    public static UUID koduKontrolEt(String kod) {
        if (kod == null)
            return null;
        return kodlar.get(kod.toUpperCase());
    }

    public static String getDiscordId(UUID uuid) {
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT discord_id FROM eslestirmeler WHERE uuid=?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            String discordId = null;
            if (rs.next()) {
                discordId = rs.getString("discord_id");
            }
            rs.close();
            ps.close();
            return discordId;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean eslestir(UUID uuid, String discordId) {
        if (uuid == null || discordId == null)
            return false;
        if (eslesmeler.containsValue(discordId) || bekleyenEslesmeler.containsValue(discordId))
            return false;
        bekleyenEslesmeler.put(uuid, discordId);
        return true;
    }

    public static boolean odulVerildiMi(UUID uuid) {
        if (odulVerildiMap.containsKey(uuid))
            return odulVerildiMap.get(uuid);

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT odul FROM eslestirmeler WHERE uuid=?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            boolean verildi = false;
            if (rs.next()) {
                verildi = rs.getInt("odul") == 1;
            }
            rs.close();
            ps.close();

            odulVerildiMap.put(uuid, verildi);
            return verildi;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void odulVerildi(UUID uuid) {
        odulVerildiMap.put(uuid, true);

        SchedulerUtil.runAsync(() -> {
            try {
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE eslestirmeler SET odul=? WHERE uuid=?");
                ps.setInt(1, 1);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean onaylaEslesme(UUID uuid, String ip) {
        final String discordId = bekleyenEslesmeler.remove(uuid);
        if (discordId == null)
            return false;

        eslesmeler.put(uuid, discordId);
        eslesmeZamanlari.put(uuid, System.currentTimeMillis());
        kayitliIPler.put(uuid, ip);
        ikiFADurumu.put(uuid, false);

        SchedulerUtil.runAsync(() -> {
            try {
                Connection conn = DatabaseManager.getConnection();

                PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO eslestirmeler (uuid, discord_id, iki_fa, ip, odul) VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, discordId);
                ps.setInt(3, 0);
                ps.setString(4, ip);
                ps.setInt(5, 0);
                ps.executeUpdate();
                ps.close();

                if (!odulVerildiMi(uuid)) {
                    SchedulerUtil.runSync(() -> trDiscordSync.getInstance().odulVer(uuid));
                    odulVerildi(uuid);
                }

                DiscordBot bot = trDiscordSync.getInstance().getDiscordBot();
                if (bot != null) {
                    bot.sendEslestirmeEmbed(uuid, discordId);
                }

            } catch (SQLException e) {
                logger.warning(e.getMessage());
            }
        });

        return true;
    }

    public static boolean beklemeVar(UUID uuid) {
        return bekleyenEslesmeler.containsKey(uuid);
    }

    public static void kaldirEslesme(UUID uuid) {
        eslesmeler.remove(uuid);
        ikiFADurumu.remove(uuid);
        kayitliIPler.remove(uuid);

        final UUID finalUuid = uuid;
        SchedulerUtil.runAsync(() -> {
            try {
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM eslestirmeler WHERE uuid=?");
                ps.setString(1, finalUuid.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                logger.warning(e.getMessage());
            }
        });
    }

    public static Map<UUID, String> getTumEslesmeler() {
        return Collections.unmodifiableMap(eslesmeler);
    }

    public static boolean eslesmeVar(UUID uuid) {
        return eslesmeler.containsKey(uuid);
    }

    public static boolean discordZatenEslesmis(String discordId) {
        return eslesmeler.containsValue(discordId) || bekleyenEslesmeler.containsValue(discordId);
    }

    public static UUID getUUIDByDiscordId(String discordId) {
        for (Map.Entry<UUID, String> entry : eslesmeler.entrySet()) {
            if (entry.getValue().equals(discordId))
                return entry.getKey();
        }
        return null;
    }

    public static boolean iptalEt(UUID uuid) {
        String kod = null;
        for (Map.Entry<String, UUID> entry : bekleyenKodlar.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                kod = entry.getKey();
                break;
            }
        }
        if (kod != null) {
            kodlar.remove(kod);
            bekleyenKodlar.remove(kod);
            kodZamanlari.remove(kod);
            return true;
        }
        return false;
    }

    public static boolean isIkiFAOpen(UUID uuid) {
        return ikiFADurumu.getOrDefault(uuid, false);
    }

    public static long getBoosterSonAlim(UUID uuid) {
        return boosterZamanlari.getOrDefault(uuid, 0L);
    }

    public static void setBoosterSonAlim(UUID uuid, long time) {
        boosterZamanlari.put(uuid, time);
        SchedulerUtil.runAsync(() -> {
            try {
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE eslestirmeler SET is_booster=? WHERE uuid=?");
                ps.setLong(1, time);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                logger.warning("Booster zamanı güncellenirken hata: " + e.getMessage());
            }
        });
    }

    public static void setIkiFA(UUID uuid, boolean durum) {
        ikiFADurumu.put(uuid, durum);

        final UUID finalUuid = uuid;
        final int val = durum ? 1 : 0;
        SchedulerUtil.runAsync(() -> {
            try {
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE eslestirmeler SET iki_fa=? WHERE uuid=?");
                ps.setInt(1, val);
                ps.setString(2, finalUuid.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                logger.warning(e.getMessage());
            }
        });
    }

    public static String getKayitliIP(UUID uuid) {
        return kayitliIPler.get(uuid);
    }

    public static void setKayitliIP(UUID uuid, String ip) {
        kayitliIPler.put(uuid, ip);

        final UUID finalUuid = uuid;
        final String finalIp = ip;
        SchedulerUtil.runAsync(() -> {
            try {
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE eslestirmeler SET ip=? WHERE uuid=?");
                ps.setString(1, finalIp);
                ps.setString(2, finalUuid.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                logger.warning(e.getMessage());
            }
        });
    }

    public static boolean ipDegisti(UUID uuid, String yeniIP) {
        String eskiIP = kayitliIPler.get(uuid);
        if (eskiIP == null)
            return false;
        return !eskiIP.equals(yeniIP);
    }

    private static void loadEslesmeler() {
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM eslestirmeler");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                eslesmeler.put(uuid, rs.getString("discord_id"));
                ikiFADurumu.put(uuid, rs.getInt("iki_fa") == 1);
                kayitliIPler.put(uuid, rs.getString("ip"));
                eslesmeZamanlari.put(uuid, System.currentTimeMillis());
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }
    }
}
