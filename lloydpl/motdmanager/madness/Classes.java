package lloydpl.motdmanager.madness;

import org.bukkit.Bukkit;

@SuppressWarnings("unchecked")
public class Classes {

    public static Class<?> serverInfoClass = getNMSClass("PacketStatusOutServerInfo");
    public static Class<?> cs = getNMSClass("ChatSerializer");
    public static Class<Object> serverPingClass = (Class<Object>) getNMSClass("ServerPing");
    public static Class<Object> getMotdClass = (Class<Object>) getNMSClass("IChatBaseComponent");
    public static Class<Object> setMotdClass = (Class<Object>) getNMSClass("ChatComponentText");
    public static Class<Object> getServerClass = (Class<Object>) getNMSClass("ServerData");
    public static Class<Object> getplayerClass = (Class<Object>) getNMSClass("ServerPingPlayerSample");

    private static Class<?> getNMSClass(String name) {
        String nms = Bukkit.getServer().getClass().getPackage().getName();
        nms = "net.minecraft.server." + nms.substring(nms.lastIndexOf('.') + 1);
        try {
            switch(name) {
                case "ServerData":
                    return Class.forName(nms + ".ServerPing$ServerData");
                case "ServerPingPlayerSample":
                    return Class.forName(nms + ".ServerPing$ServerPingPlayerSample");
                case "PacketStatusOutServerInfo":
                    return Class.forName(nms + ".PacketStatusOutServerInfo");
                case "ServerPing":
                    return Class.forName(nms + ".ServerPing");
                case "IChatBaseComponent":
                    return Class.forName(nms + ".IChatBaseComponent");
                case "ChatComponentText":
                    return Class.forName(nms + ".ChatComponentText");
                case "ChatSerializer":
                    return Class.forName(nms + ".IChatBaseComponent$ChatSerializer");
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("Class not found: " + name);
        }
        return null;
    }
}
