package lloydpl.motdmanager.madness;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lloydpl.motdmanager.MOTDManagerPlugin;
import lloydpl.motdmanager.ServerPingEvent;
import lloydpl.motdmanager.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Level;

public class PacketInterceptor extends ChannelDuplexHandler {

    private final MOTDManagerPlugin plugin;

    public volatile Player player;
    public Reflection.FieldAccessor<Object> serverPing = Reflection.getField(Classes.serverInfoClass, Classes.serverPingClass, 0);
    public Reflection.FieldAccessor<Object> playerRefl = Reflection.getField(Classes.serverPingClass, Classes.getplayerClass, 0);
    public Reflection.FieldAccessor<Object> motdClass = Reflection.getField(Classes.serverPingClass, Classes.getMotdClass, 0);
    public Reflection.FieldAccessor<Object> serverClass = Reflection.getField(Classes.serverPingClass, Classes.getServerClass, 0);

    public Reflection.ConstructorInvoker motdInvoker = Reflection.getConstructor(Classes.setMotdClass, String.class);
    public Reflection.ConstructorInvoker serverInvoker = Reflection.getConstructor(Classes.getServerClass, String.class, int.class);

    private static final Class<?> PACKET_LOGIN_IN_START = Reflection.getMinecraftClass("PacketLoginInStart");
    private static final Reflection.FieldAccessor<GameProfile> getGameProfile = Reflection.getField(PACKET_LOGIN_IN_START, GameProfile.class, 0);

    private Map<String, Channel> channelLookup = new MapMaker().weakValues().makeMap();

    public PacketInterceptor(MOTDManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Channel channel = ctx.channel();
        handleLoginStart(channel, msg);

        try {
            msg = onPacketInAsync(player, channel, msg);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in onPacketInAsync().", e);
        }

        if (msg != null) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            msg = onPacketOutAsync(player, ctx.channel(), msg);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in onPacketOutAsync().", e);
        }

        if (msg != null) {
            super.write(ctx, msg, promise);
        }
    }

    private void handleLoginStart(Channel channel, Object packet) {
        if (PACKET_LOGIN_IN_START.isInstance(packet)) {
            GameProfile profile = getGameProfile.get(packet);
            channelLookup.put(profile.getName(), channel);
        }
    }

    public Object onPacketInAsync(Player receiver, Channel channel, Object packet) {
        return packet;
    }

    public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
        if (Classes.serverInfoClass.isInstance(packet)) {
            Object ping = serverPing.get(packet);
            Object playersample = playerRefl.get(ping);
            Object serverdata = serverClass.get(ping);

            int baseMaxPlayers = (int) Reflection.getMethod(playersample.getClass(), "a").invoke(playersample);
            int baseOnlinePlayers = (int) Reflection.getMethod(playersample.getClass(), "b").invoke(playersample);
            GameProfile[] baseHoverList = (GameProfile[]) Reflection.getMethod(playersample.getClass(), "c").invoke(playersample);
            String baseMOTD = (String) Reflection.getMethod(Classes.cs, "a", Classes.getMotdClass).invoke(Classes.cs, Reflection.getMethod(ping.getClass(), "a").invoke(ping));
            baseMOTD = (String) baseMOTD.subSequence(9, baseMOTD.length() - 2);
            String baseVersion = (String) Reflection.getMethod(serverdata.getClass(), "a").invoke(serverdata);

            ServerPingEvent ev = new ServerPingEvent(channel.remoteAddress().toString(), baseMOTD, baseHoverList, baseOnlinePlayers, baseMaxPlayers, baseVersion);
            Bukkit.getServer().getPluginManager().callEvent(ev);

            if (ev.isCancelled()) return null;
            Reflection.getField(playersample.getClass(), "a", int.class).set(playersample, ev.getMax());
            if (ev.getFakePlayers() != 0)
                Reflection.getField(playersample.getClass(), "b", int.class).set(playersample, baseOnlinePlayers + ev.getFakePlayers());
            else
                Reflection.getField(playersample.getClass(), "b", int.class).set(playersample, ev.getOnline());
            if (ev.getHover() != null)
                Reflection.getField(playersample.getClass(), "c", GameProfile[].class).set(playersample, Util.getHover(ev.getHover()));
            if (ev.getMotdLine1() != null || ev.getMotdLine2() != null) motdClass.set(ping, motdInvoker.invoke(
                    Util.color(((ev.getMotdLine1() == null) ? "" : ev.getMotdLine1()) + "\n" + ((ev.getMotdLine2() == null) ? "" : ev.getMotdLine2()))));
            if (ev.getVersionEdit() != null)
                serverClass.set(ping, serverInvoker.invoke(Util.color(ev.getVersionEdit()), 0));

            return packet;
        }
        return packet;
    }

}