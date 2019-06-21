package lloydpl.motdmanager.madness;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class MOTDManager {

    public MOTDManager(Plugin p) {
        try {
            new TinyProtocol(p);
        } catch (Exception ex) {
            Main.getInst().getLogger().log(Level.SEVERE, "MOTDManager encountered unhandled exception! ", ex);
        }
    }

} 
 
