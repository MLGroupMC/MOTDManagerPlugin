package lloydpl.motdmanager.madness;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class MOTDManager implements Listener {

    public MOTDManager(Plugin p) {
        try {
            new TinyProtocol(p);
        } catch (Exception ex) {
            System.out.print("MOTDManager encountered unhandled exception! " + ex);
        }
    }

} 
 
