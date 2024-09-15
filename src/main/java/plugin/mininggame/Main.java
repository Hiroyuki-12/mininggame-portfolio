package plugin.mininggame;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.mininggame.command.MiningGameCommand;

import java.util.Objects;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        MiningGameCommand miningGameCommand = new MiningGameCommand(this);
        Bukkit.getPluginManager().registerEvents(miningGameCommand, this);
        Objects.requireNonNull(getCommand("miningGame")).setExecutor(miningGameCommand);
    }
}


