package core;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import core.MOTDManager.ServerPingEvent;

public class Core extends JavaPlugin implements Listener {

	public void onEnable() {
		this.getLogger().info("Uruchamianie pluginu MOTDManager...");
		new MOTDManager(this);
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
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
				if(line1 != null || line2 != null) {
					sender.sendMessage("MOTD:");
					if(line1 != null)
						sender.sendMessage(" - "+color(line1));
					else
						sender.sendMessage(" -");
					if(line2 != null)
						sender.sendMessage(" - "+color(line2));
					else
						sender.sendMessage(" -");
				} else
					sender.sendMessage("MOTD:BRAK_INGERENCJI");
				if(version != null)
					sender.sendMessage("Wersja: "+color(version)); 
				else
					sender.sendMessage("Wersja:BRAK_INGERENCJI");
				if(online != null || max != null) {
					String o, m;
					if(online != null)
						o = String.valueOf(online);
					else
						o = "real";
					if(max != null)
						m = String.valueOf(max);
					else
						m = "real";
					sender.sendMessage("Gracze: "+o+"/"+m);
				} else
					sender.sendMessage("Gracze:BRAK_INGERENCJI");
				if(hover != null) {
					sender.sendMessage("Hover:");
					for(String line : hover) 
						sender.sendMessage(" - "+color(line));
				} else
					sender.sendMessage("Hover:BRAK_INGERENCJI");
			}
		}
		return false;
	}
	
	private String line1, line2, version;
	private List<String> hover;
	private Integer online, max;
	private boolean change = false;
	
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
		this.saveDefaultConfig();
		this.reloadConfig();
		FileConfiguration cfg = this.getConfig();
		if(cfg.isSet("line1"))
			this.line1 = cfg.getString("line1");
		else
			this.line1 = null;
		if(cfg.isSet("line2"))
			this.line2 = cfg.getString("line2");
		else
			this.line2 = null;
		if(cfg.isSet("version"))
			this.version = cfg.getString("version");
		else
			this.version = null;
		if(cfg.isSet("hover"))
			this.hover = cfg.getStringList("hover");
		else
			this.hover = null;
		if(cfg.isSet("online"))
			this.online = cfg.getInt("online");
		else
			this.online = null;
		if(cfg.isSet("max"))
			this.max = cfg.getInt("max");
		else
			this.max = null;
		if(cfg.isSet("enable"))
			this.change = cfg.getBoolean("enable");
		else
			this.change = false;
	}
	
	public String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
}
