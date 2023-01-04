package net.skyrath.rphoster.file;

import com.profewgames.prohelper.internal.LoaderUtils;
import com.profewgames.prohelper.shaded.storage.Config;
import com.profewgames.prohelper.shaded.storage.LightningBuilder;
import com.profewgames.prohelper.shaded.storage.internal.settings.ReloadSettings;
import lombok.Getter;
import net.skyrath.rphoster.RPHosterPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ConfigurationFile {

    private final RPHosterPlugin plugin;
    private final Config config;

    @Getter
    public String address;

    @Getter
    private int port;

    @Getter
    private boolean forceResourcePack;

    @Getter
    private String declinedResourcePack;

    @Getter
    private File resourcePack;
    @Getter
    private byte[] resourcePackHash;

    public ConfigurationFile(RPHosterPlugin plugin) {
        this.plugin = plugin;
        this.config = LightningBuilder.fromFile(new File(plugin.getDataFolder(), "config.yml"))
                .addInputStream(plugin.getResource("config.yml"))
                .setReloadSettings(ReloadSettings.MANUALLY)
                .createConfig();

        handleReload();
    }

    public void reload() {
        this.config.forceReload();
        handleReload();
    }

    private void handleReload() {
        this.address = this.config.getString("address");
        this.port = this.config.getInt("port");
        this.resourcePack = new File(plugin.getDataFolder(), this.config.getString("resource-pack name"));
        this.forceResourcePack = this.config.getBoolean("force resource-pack");
        this.declinedResourcePack = this.config.getString("declined resource-pack");
        try {
            this.calculateHash();
        } catch (Exception e) {
            LoaderUtils.warn("[RPHost] Failed to calculate hash for resource-pack.");
            e.printStackTrace();
        }
        if (!this.resourcePack.exists())
            LoaderUtils.warn("[RPHoster] Configured resource pack does not exist!");
    }

    private void calculateHash() throws IOException, NoSuchAlgorithmException {
        if (this.resourcePack == null) return;
        try (FileInputStream fis = new FileInputStream(this.resourcePack)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (DigestInputStream dis = new DigestInputStream(fis, digest)) {
                byte[] bytes = new byte[1024];
                // read all file content
                //noinspection StatementWithEmptyBody
                while (dis.read(bytes) > 0) ;

                this.resourcePackHash = digest.digest();
            }
        }
    }
}