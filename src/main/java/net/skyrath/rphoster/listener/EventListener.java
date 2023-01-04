package net.skyrath.rphoster.listener;

import com.profewgames.prohelper.format.C;
import com.profewgames.prohelper.format.F;
import com.profewgames.prohelper.internal.LoaderUtils;
import com.profewgames.prohelper.scheduler.ScheduleHelper;
import net.skyrath.rphoster.RPHosterPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class EventListener implements Listener {

    private final RPHosterPlugin plugin;

    public EventListener(RPHosterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String url = "http://" + this.plugin.getConfigurationFile().getAddress() + ":" + this.plugin.getConfigurationFile().getPort() + "/" + this.plugin.getConfigurationFile().getResourcePack().getName();
        if (this.plugin.getConfigurationFile().getResourcePackHash() == null)
            player.setResourcePack(url);
        else
            player.setResourcePack(url, this.plugin.getConfigurationFile().getResourcePackHash());
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            ScheduleHelper.sync().runLater(() -> {
                player.sendTitle(C.headInverse("Resource Pack"), C.error("Download Failed"), 10, 20 * 5, 10);
                player.sendMessage(F.main("Resource Pack", "You've accepted to download the server textures but the download has failed. Some visuals may not work until you relog."));
            }, 2L);
            LoaderUtils.debug("[RPHost] '%s' has accepted to download the resource pack but the download failed. Allowing player online.".formatted(player.getName()));
            return;
        }
        if (!this.plugin.getConfigurationFile().isForceResourcePack()) return;
        if (event.getStatus() != PlayerResourcePackStatusEvent.Status.DECLINED) return;
        player.kickPlayer(C.color(this.plugin.getConfigurationFile().getDeclinedResourcePack()));
    }
}