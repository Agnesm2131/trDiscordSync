package me.agnes.trDiscordSync.placeholders;

import com.bentahsin.benthpapimanager.annotations.Placeholder;
import com.bentahsin.benthpapimanager.annotations.PlaceholderIdentifier;
import me.agnes.trDiscordSync.data.EslestirmeManager;

@SuppressWarnings("unused")
@Placeholder(identifier = "trdiscordsync_server", author = "Agnes & bentahsin", version = "1.2.6")
public class ServerPlaceholders {

    @PlaceholderIdentifier(identifier = "toplam_eslesme", onError = "0")
    public String getToplamEslesme() {
        return String.valueOf(EslestirmeManager.getTumEslesmeler().size());
    }
}