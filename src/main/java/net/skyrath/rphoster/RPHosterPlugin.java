package net.skyrath.rphoster;

import com.profewgames.prohelper.ProJavaPlugin;
import com.profewgames.prohelper.internal.LoaderUtils;
import lombok.Getter;
import net.skyrath.rphoster.file.ConfigurationFile;
import net.skyrath.rphoster.httpd.MiniHttpd;
import net.skyrath.rphoster.listener.EventListener;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;

public class RPHosterPlugin extends ProJavaPlugin {

    @Getter
    private ConfigurationFile configurationFile;

    private MiniHttpd httpd;

    @Override
    protected void enable() {
        this.configurationFile = new ConfigurationFile(this);
        startHttpd();

        Bukkit.getPluginManager().registerEvents(new EventListener(this), this);
    }

    @Override
    protected void disable() {
        if (this.httpd != null)
            this.httpd.terminate();
    }

    private void startHttpd() {
        try {
            this.httpd = new MiniHttpd(this.configurationFile.getPort()) {

                @Override
                public File requestFileCallback(MiniConnection connection, String request) {
                    return RPHosterPlugin.this.configurationFile.getResourcePack();
                }

                @Override
                public void onSuccessfulRequest(MiniConnection connection, String request) {
                    LoaderUtils.debug("[RPHost] Successfully served resource pack to '%s'".formatted(connection.getClient().getInetAddress()));
                }

                @Override
                public void onRequestError(MiniConnection connection, int code) {
                    LoaderUtils.warn("[RPHost] Error '%s' when attempting to serve '%s'".formatted(code, connection.getClient().getInetAddress()));
                }
            };
            this.httpd.start();
            LoaderUtils.info("[RPHost] Successfully started the mini http daemon!");
        } catch (IOException e) {
            LoaderUtils.warn("[RPHost] Unable to start the mini http daemon!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
}