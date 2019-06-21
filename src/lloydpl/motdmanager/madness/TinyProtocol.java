package lloydpl.motdmanager.madness;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import io.netty.channel.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class TinyProtocol {

    private static final AtomicInteger ID = new AtomicInteger(0);

    private static final Class<Object> minecraftServerClass = Reflection.getUntypedClass("{nms}.MinecraftServer");
    private static final Class<Object> serverConnectionClass = Reflection.getUntypedClass("{nms}.ServerConnection");
    private static final Reflection.FieldAccessor<Object> getMinecraftServer = Reflection.getField("{obc}.CraftServer", minecraftServerClass, 0);
    private static final Reflection.FieldAccessor<Object> getServerConnection = Reflection.getField(minecraftServerClass, serverConnectionClass, 0);
    private static final Reflection.MethodInvoker getNetworkMarkers = Reflection.getTypedMethod(serverConnectionClass, null, List.class, serverConnectionClass);

    private static final Class<?> PACKET_LOGIN_IN_START = Reflection.getMinecraftClass("PacketLoginInStart");

    private Listener listener;

    private Set<Channel> uninjectedChannels = Collections.newSetFromMap(new MapMaker().weakKeys().<Channel, Boolean>makeMap());

    private List<Object> networkManagers;

    private List<Channel> serverChannels = Lists.newArrayList();
    private ChannelInboundHandlerAdapter serverChannelHandler;
    private ChannelInitializer<Channel> beginInitProtocol;
    private ChannelInitializer<Channel> endInitProtocol;

    private String handlerName;

    protected volatile boolean closed;
    protected Plugin plugin;

    public TinyProtocol(final Plugin plugin) {
        this.plugin = plugin;

        this.handlerName = getHandlerName();

        registerBukkitEvents();

        try {
            registerChannelHandler();
        } catch (IllegalArgumentException ex) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    registerChannelHandler();
                }
            }.runTask(plugin);
        }
    }

    private void createServerChannelHandler() {
        try {
            endInitProtocol = new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel channel) {
                    try {
                        synchronized (networkManagers) {
                            if (!closed) {
                                channel.eventLoop().submit(() -> injectChannelInternal(channel));
                            }
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.SEVERE, "Cannot inject incomming channel " + channel, ex);
                    }
                }

            };

            beginInitProtocol = new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel channel) {
                    channel.pipeline().addLast(endInitProtocol);
                }

            };

            serverChannelHandler = new ChannelInboundHandlerAdapter() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    Channel channel = (Channel) msg;

                    channel.pipeline().addFirst(beginInitProtocol);
                    ctx.fireChannelRead(msg);
                }

            };
        } catch (Exception ex) {
            System.out.println("TinyProtocol encountered unhandled exception: " + ex);
        }
    }

    private void registerBukkitEvents() {
        listener = new Listener() {
            @EventHandler
            public final void onPluginDisable(PluginDisableEvent e) {
                if (e.getPlugin().equals(plugin)) {
                    for (Channel serverChannel : serverChannels) {
                        final ChannelPipeline pipeline = serverChannel.pipeline();
                        serverChannel.eventLoop().execute(() -> pipeline.remove(serverChannelHandler));
                    }
                }
            }
        };

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }


    private void registerChannelHandler() {
        Object mcServer = getMinecraftServer.get(Bukkit.getServer());
        Object serverConnection = getServerConnection.get(mcServer);
        boolean looking = true;

        networkManagers = (List<Object>) getNetworkMarkers.invoke(null, serverConnection);
        createServerChannelHandler();

        for (int i = 0; looking; i++) {
            List<Object> list = Reflection.getField(serverConnection.getClass(), List.class, i).get(serverConnection);

            for (Object item : list) {
                if (!ChannelFuture.class.isInstance(item))
                    break;

                Channel serverChannel = ((ChannelFuture) item).channel();

                serverChannels.add(serverChannel);
                serverChannel.pipeline().addFirst(serverChannelHandler);
                looking = false;
            }
        }
    }

    protected String getHandlerName() {
        return "tiny-" + plugin.getName() + "-" + ID.incrementAndGet();
    }

    private PacketInterceptor injectChannelInternal(Channel channel) {
        try {
            PacketInterceptor interceptor = (PacketInterceptor) channel.pipeline().get(handlerName);
            if (interceptor == null) {
                interceptor = new PacketInterceptor();
                channel.pipeline().addBefore("packet_handler", handlerName, interceptor);
                uninjectedChannels.remove(channel);
            }
            return interceptor;
        } catch (IllegalArgumentException e) {
            return (PacketInterceptor) channel.pipeline().get(handlerName);
        }
    }

}