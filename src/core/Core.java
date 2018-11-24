package core;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import core.MOTDManager.ServerPingEvent;

public class Core extends JavaPlugin implements Listener {

	public void onEnable() {
		this.getLogger().info("Uruchamianie pluginu MOTDManager...");
		new MOTDManager(this);
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		this.inst = this;
		reloadCfg();
		this.getLogger().info("Plugin zostal pomyslnie uruchomiony!");
		if(change) {
			this.getLogger().info("Customowe MOTD zostalo wlaczone!");
		} else {
			this.getLogger().info("Customowe MOTD nie zostalo wlaczone!");
			this.getLogger().info("Aby je wlaczyc zmien wartosc enable w configu na true");
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("motdmanager")) {
			if(args[0].equalsIgnoreCase("reload")) {
				reloadCfg();
				sender.sendMessage(color("&aConfig MOTDManagera zostal zaladowany!"));
				if(change) {
					sender.sendMessage(color("&aCustomowe MOTD zostalo wlaczone!"));
				} else {
					sender.sendMessage(color("&cCustomowe MOTD nie zostalo wlaczone!"));
					sender.sendMessage(color("&cAby je wlaczyc zmien wartosc enable w configu na true"));
				}
			} else if(args[0].equalsIgnoreCase("check")) {
				sender.sendMessage(color("Wartosci w configu MOTDManagera:"));
				sender.sendMessage("Ingerencja: " + String.valueOf(change));
				sender.sendMessage("MOTD:");
				sender.sendMessage(" - "+color(line1));
				sender.sendMessage(" - "+color(line2));
				sender.sendMessage("Wersja:");
				sender.sendMessage(" - "+color(version));
				sender.sendMessage("Gracze:");
				sender.sendMessage(" - "+String.valueOf(online)+"/"+String.valueOf(max));
				sender.sendMessage("Hover:");
				for(String line : hover) 
					sender.sendMessage(" - "+color(line));
			}
		}
		return false;
	}
	
	private String line1, line2, version;
	private List<String> hover;
	private int online, max;
	private boolean change = false;
	private Plugin inst;
	
	@EventHandler
	public void onServerPing(ServerPingEvent ev) {
		if(change) {
			ev.setMotdLine1(line1);
			ev.setMotdLine2(line2);
			ev.setVersion(version);
			ev.setOnline(online);
			ev.setMax(max);
			ev.setHover(hover);
		}
	}
	
	private void reloadCfg() {
		inst.saveDefaultConfig();
		inst.reloadConfig();
		FileConfiguration cfg = inst.getConfig();
		this.line1 = cfg.getString("line1");
		this.line2 = cfg.getString("line2");
		this.version = cfg.getString("version");
		this.hover = cfg.getStringList("hover");
		this.online = cfg.getInt("online");
		this.max = cfg.getInt("max");
		this.change = cfg.getBoolean("enable");
	}
	
	public String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
}