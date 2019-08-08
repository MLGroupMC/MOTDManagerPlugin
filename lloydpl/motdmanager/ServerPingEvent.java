package lloydpl.motdmanager;

import com.mojang.authlib.GameProfile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

public class ServerPingEvent extends Event implements Cancellable {

    private String address;
    private String motdline1;
    private String motdline2 = "";
    private List<String> hover;
    private int online;
    private int max;
    private int fake;
    private final String version;
    private String versionedit;
    private boolean cancel = false;

    public ServerPingEvent(String address, String motd, GameProfile[] hover, int online, int max, String ver) {
        this.address = address;
        final String[] lines = motd.split("\\n");
        this.motdline1 = lines[0];
        if (lines.length > 1) this.motdline2 = lines[1];
        this.hover = new ArrayList<>();
        for (GameProfile profile : hover) {
            if (profile == null || profile.getName() == null || profile.getName().equals("")) {
                this.hover.add("");
            } else {
                this.hover.add(profile.getName());
            }
        }
        this.online = online;
        this.max = max;
        this.fake = 0;
        this.version = ver;
    }

    public String getAddress() {
        return this.address;
    }

    public String getMotdLine1() {
        return this.motdline1;
    }

    public void setMotdLine1(String s) {
        this.motdline1 = s;
    }

    public String getMotdLine2() {
        return this.motdline2;
    }

    public void setMotdLine2(String s) {
        this.motdline2 = s;
    }

    public List<String> getHover() {
        return this.hover;
    }

    public void setHover(List<String> h) {
        this.hover = h;
    }

    public int getOnline() {
        return this.online;
    }

    public void setOnline(int o) {
        this.online = o;
    }

    public int getMax() {
        return this.max;
    }

    public void setMax(int m) {
        this.max = m;
    }

    public int getFakePlayers() {
        return this.fake;
    }

    public void setFakePlayers(int i) {
        this.fake = i;
    }

    public String getVersion() {
        return this.version;
    }

    public String getVersionEdit() {
        return this.versionedit;
    }

    public void setVersion(String v) {
        this.versionedit = v;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancel;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancel = b;
    }
}