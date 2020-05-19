package lloydpl.motdmanager.madness;

import lloydpl.motdmanager.MOTDManagerPlugin;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class MOTDManager implements Listener {

    public MOTDManager(MOTDManagerPlugin p) {
        try {
            new TinyProtocol(p);
        } catch (Exception ex) {
            System.out.print("MOTDManager encountered unhandled exception! " + ex);
        }
    }

} 
 
