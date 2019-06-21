package lloydpl.motdmanager;

import com.mojang.authlib.GameProfile;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.UUID;

public class Util {

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static GameProfile getGP(String s) {
        return new GameProfile(new UUID(0, 0), color(s));
    }

    public static GameProfile[] getHover(List<String> list) {
        if (list == null || list.size() == 0) return new GameProfile[]{getGP("")};
        GameProfile[] gameProfiles = new GameProfile[list.size()];
        int i = 0;
        for (String line : list) {
            if (line == null) {
                gameProfiles[i] = getGP("");
            } else {
                gameProfiles[i] = getGP(line);
            }
            i++;
        }
        return gameProfiles;
    }

}
