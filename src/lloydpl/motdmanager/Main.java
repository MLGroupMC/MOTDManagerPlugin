package lloydpl.motdmanager;

import java.util.List;

import lloydpl.motdmanager.bstats.Metrics;
import lloydpl.motdmanager.madness.MOTDManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import static lloydpl.motdmanager.Util.color;

public class Main extends JavaPlugin implements Listener {

	public void onEnable() {
		this.getLogger().info("Uruchamianie pluginu MOTDManager...");
		inst = this;
		new MOTDManager(this);
		new Metrics(this);
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
				sender.sendMessage("Ingerencja: "+change);

				/*MOTD*/
				if(line1 != null || line2 != null) {
					sender.sendMessage("MOTD:");
					sender.sendMessage(" - "+((line1 != null) ? color(line1) : ""));
					sender.sendMessage(" - "+((line2 != null) ? color(line2) : ""));
				} else
					sender.sendMessage("MOTD: BRAK INGERENCJI");

				/*Wersja*/
				sender.sendMessage("Wersja: "+((version != null) ? color(version) : "BRAK INGERENCJI"));

				/*Gracze*/
				if(online != null || max != null || fake != null) {
					String o = (online != null) ? String.valueOf(online) : ((fake != 0) ? "zawyzone o "+fake : "real");
					String m = (max != null) ? String.valueOf(max) : "real";
					sender.sendMessage("Gracze: "+o+"/"+m);
				} else
					sender.sendMessage("Gracze: BRAK INGERENCJI");

				/*Lista hover*/
				if(hover != null) {
					sender.sendMessage("Hover:");
					for(String line : hover) 
						sender.sendMessage(" - "+color(line));
				} else
					sender.sendMessage("Hover: BRAK INGERENCJI");
			}
		}
		return false;
	}

	public static Main getInst() {
		return inst;
	}

	private static Main inst;
	private String line1, line2, version;
	private List<String> hover;
	private Integer online, max, fake;
	private boolean change = false;

	private void reloadCfg() {
		this.saveDefaultConfig();
		this.reloadConfig();
		FileConfiguration cfg = this.getConfig();

		this.line1 = (cfg.isSet("line1")) ? cfg.getString("line1") : null;
		this.line2 = (cfg.isSet("line2")) ? cfg.getString("line2") : null;
		this.version = (cfg.isSet("version")) ? cfg.getString("version") : null;
		this.hover = (cfg.isSet("hover")) ? cfg.getStringList("hover") : null;
		this.online = (cfg.isSet("online")) ? cfg.getInt("online") : null;
		this.fake = (cfg.isSet("online")) ? cfg.getInt("fakeplayers") : null;
		this.max = (cfg.isSet("max")) ? cfg.getInt("max") : null;
		this.change = (cfg.isSet("enable")) && cfg.getBoolean("enable");
	}

	@EventHandler
	public void onServerPing(ServerPingEvent ev) {
		if(!change)
			return;
		ev.setMotdLine1(line1);
		ev.setMotdLine2(line2);
		ev.setVersion(version);
		ev.setOnline(online);
		ev.setMax(max);
		ev.setFakePlayers(fake);
		ev.setHover(hover);
	}

}
