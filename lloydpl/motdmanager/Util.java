package lloydpl.motdmanager;

import com.mojang.authlib.GameProfile;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.UUID;

public class Util {

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static GameProfile getGameProfile(String s) {
        return new GameProfile(new UUID(0, 0), color(s));
    }

    public static GameProfile[] getHover(List<String> list) {
        if (list == null || list.size() == 0) return new GameProfile[]{getGameProfile("")};
        GameProfile[] gameProfiles = new GameProfile[list.size()];
        int i = 0;
        for(String line : list) {
            gameProfiles[i] = (line == null) ? getGameProfile("") : getGameProfile(line);
            i++;
        }
        return gameProfiles;
    }

}
