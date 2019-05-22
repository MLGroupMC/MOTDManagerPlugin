package core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;

import core.MOTDManager.Reflection.ConstructorInvoker;
import core.MOTDManager.Reflection.FieldAccessor;
import core.MOTDManager.Reflection.MethodInvoker;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

@SuppressWarnings("unchecked")
public class MOTDManager implements Listener {

	public MOTDManager(Plugin p) {
		try {
			new TinyProtocol(p);
		} catch(Exception ex) {
			System.out.print("MOTDManager encountered unhandled exception! " + ex);
		}
	}
	
    public static class TinyProtocol {
    	
    	public Class<?> serverInfoClass = getNMSClass("PacketStatusOutServerInfo");
        public Class<?> cs = getNMSClass("ChatSerializer");
        public Class<Object> serverPingClass = (Class<Object>) getNMSClass("ServerPing");
        public Class<Object> getMotdClass = (Class<Object>) getNMSClass("IChatBaseComponent");
        public Class<Object> setMotdClass = (Class<Object>) getNMSClass("ChatComponentText");
        public Class<Object> getServerClass = (Class<Object>) getNMSClass("ServerData");
        public Class<Object> getplayerClass = (Class<Object>) getNMSClass("ServerPingPlayerSample");
        
        private String color(String str) {
            return ChatColor.translateAlternateColorCodes('&', str);
        }
        
        private GameProfile getGP(String s) {
            return new GameProfile(new UUID(0, 0), color(s));
        }
        
        private GameProfile[] getHover(List<String> list) {
            if(list == null || list.size() == 0) return new GameProfile[] {getGP("")};
            GameProfile[] gameProfiles = new GameProfile[list.size()];
            int i = 0;
            for(String line : list) {
                if(line == null) {
                    gameProfiles[i] = getGP("");
                } else {
                    gameProfiles[i] = getGP(line);
                }
                i++;
            }
            return gameProfiles;
        }

        private Class<?> getNMSClass(String name) {
            String nms = Bukkit.getServer().getClass().getPackage().getName();
            nms = "net.minecraft.server." + nms.substring(nms.lastIndexOf('.') + 1);
            try {
                if(name.equals("ServerData")) {
                    return Class.forName(nms + ".ServerPing$ServerData");
                } else if(name.equals("ServerPingPlayerSample")) {
                    return Class.forName(nms + ".ServerPing$ServerPingPlayerSample");
                } else if(name.equals("PacketStatusOutServerInfo")) {
                    return Class.forName(nms + ".PacketStatusOutServerInfo");
                } else if(name.equals("ServerPing")) {
                    return Class.forName(nms + ".ServerPing");
                } else if(name.equals("IChatBaseComponent")) {
                    return Class.forName(nms + ".IChatBaseComponent");
                } else if(name.equals("ChatComponentText")) {
                    return Class.forName(nms + ".ChatComponentText");
                } else if(name.equals("ChatSerializer")) {
                    return Class.forName(nms + ".IChatBaseComponent$ChatSerializer");
                }
            } catch(ClassNotFoundException ex) {
                System.out.println("Class not found: " + name);
            }
            return null;
        }
        
        public FieldAccessor<Object> serverPing = Reflection.getField(serverInfoClass, serverPingClass, 0);
        public FieldAccessor<Object> player = Reflection.getField(serverPingClass, getplayerClass, 0);
        public FieldAccessor<Object> motdClass = Reflection.getField(serverPingClass, getMotdClass, 0);
        public FieldAccessor<Object> serverClass = Reflection.getField(serverPingClass, getServerClass, 0);
        
        public ConstructorInvoker motdInvoker = Reflection.getConstructor(setMotdClass, String.class);
        public ConstructorInvoker serverInvoker = Reflection.getConstructor(getServerClass, String.class, int.class);
    	
        private static final AtomicInteger ID = new AtomicInteger(0);

        private static final Class<Object> minecraftServerClass = Reflection.getUntypedClass("{nms}.MinecraftServer");
        private static final Class<Object> serverConnectionClass = Reflection.getUntypedClass("{nms}.ServerConnection");
        private static final FieldAccessor<Object> getMinecraftServer = Reflection.getField("{obc}.CraftServer", minecraftServerClass, 0);
        private static final FieldAccessor<Object> getServerConnection = Reflection.getField(minecraftServerClass, serverConnectionClass, 0);
        private static final MethodInvoker getNetworkMarkers = Reflection.getTypedMethod(serverConnectionClass, null, List.class, serverConnectionClass);

        private static final Class<?> PACKET_LOGIN_IN_START = Reflection.getMinecraftClass("PacketLoginInStart");
        private static final FieldAccessor<GameProfile> getGameProfile = Reflection.getField(PACKET_LOGIN_IN_START, GameProfile.class, 0);

        private Map<String, Channel> channelLookup = new MapMaker().weakValues().makeMap();
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
	                protected void initChannel(Channel channel) throws Exception {
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
	                protected void initChannel(Channel channel) throws Exception {
	                    channel.pipeline().addLast(endInitProtocol);
	                }
	
	            };
	
	            serverChannelHandler = new ChannelInboundHandlerAdapter() {
	        
	                @Override
	                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
	                    Channel channel = (Channel) msg;
	
	                    channel.pipeline().addFirst(beginInitProtocol);
	                    ctx.fireChannelRead(msg);
	                }
	
	            };
        	} catch(Exception ex) {
        		System.out.println("TinyProtocol encountered unhandled exception: " + ex);
        	}
        }

        private void registerBukkitEvents() {
            listener = new Listener() {
                @EventHandler
                public final void onPluginDisable(PluginDisableEvent e) {
                    if (e.getPlugin().equals(plugin)) {
                    	try {
	                		for (Channel serverChannel : serverChannels) {
	                			final ChannelPipeline pipeline = serverChannel.pipeline();
	                			try {
	                				serverChannel.eventLoop().execute(new Runnable() {
			                			@Override
			                			public void run() {
			                				try {
			                					pipeline.remove(serverChannelHandler);
			                				} catch (NoSuchElementException e) {}
			                			}
	                				});
	                			} catch(Exception ex) {}
	                		}
                    	} catch(Exception ex) {
                    		System.out.print("MOTDManager encountered unhandled exception! " + e);
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

        public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
        	if (serverInfoClass.isInstance(packet)) {
                Object ping = serverPing.get(packet);
                Object playersample = player.get(ping);
                Object serverdata = serverClass.get(ping);

                int baseMaxPlayers = (int) Reflection.getMethod(playersample.getClass(), "a").invoke(playersample);
                int baseOnlinePlayers = (int) Reflection.getMethod(playersample.getClass(), "b").invoke(playersample);
                GameProfile[] baseHoverList = (GameProfile[]) Reflection.getMethod(playersample.getClass(), "c").invoke(playersample);
                String baseMOTD = (String) Reflection.getMethod(cs, "a", getMotdClass).invoke(cs, Reflection.getMethod(ping.getClass(), "a").invoke(ping));
                baseMOTD = (String) baseMOTD.subSequence(9, baseMOTD.length() - 2);
                String baseVersion = (String) Reflection.getMethod(serverdata.getClass(), "a").invoke(serverdata);
                
                ServerPingEvent ev = new ServerPingEvent(channel.remoteAddress().toString().substring(1), baseMOTD, baseHoverList, baseOnlinePlayers, baseMaxPlayers, baseVersion);
                Bukkit.getServer().getPluginManager().callEvent(ev);
                
                if(ev.isCancelled()) return null;
                Reflection.getField(playersample.getClass(), "a", int.class).set(playersample, ev.getMax());
                Reflection.getField(playersample.getClass(), "b", int.class).set(playersample, ev.getOnline());
                if(ev.getHover() != null) Reflection.getField(playersample.getClass(), "c", GameProfile[].class).set(playersample, getHover(ev.getHover()));
                if(ev.getMotdLine1() != null || ev.getMotdLine2() != null) motdClass.set(ping, motdInvoker.invoke(
                	color(((ev.getMotdLine1() == null) ? "" : ev.getMotdLine1()) + "\n" + ((ev.getMotdLine2() == null) ? "" : ev.getMotdLine2()))));
                if(ev.getVersionEdit() != null) serverClass.set(ping, serverInvoker.invoke(color(ev.getVersionEdit()), 0));
                
                return packet;
            }
            return packet;
        }

        public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
            return packet;
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

        private final class PacketInterceptor extends ChannelDuplexHandler {
            public volatile Player player;

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
        }
    }

    public static class Reflection {

        public interface ConstructorInvoker {
            public Object invoke(Object... arguments);
        }

        public interface MethodInvoker {
            public Object invoke(Object target, Object... arguments);
        }

        public interface FieldAccessor<T> {

            public T get(Object target);
            
            public void set(Object target, Object value);

            public boolean hasField(Object target);
        }
        
        private static String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
        private static String NMS_PREFIX = OBC_PREFIX.replace("org.bukkit.craftbukkit", "net.minecraft.server");
        private static String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit", "").replace(".", "");

        private static Pattern MATCH_VARIABLE = Pattern.compile("\\{([^\\}]+)\\}");

        private Reflection() {
        }

        public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType) {
            return getField(target, name, fieldType, 0);
        }

        public static <T> FieldAccessor<T> getField(String className, String name, Class<T> fieldType) {
            return getField(getClass(className), name, fieldType, 0);
        }


        public static <T> FieldAccessor<T> getField(Class<?> target, Class<T> fieldType, int index) {
            return getField(target, null, fieldType, index);
        }

        public static <T> FieldAccessor<T> getField(String className, Class<T> fieldType, int index) {
            return getField(getClass(className), fieldType, index);
        }

        private static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType, int index) {
            for (final Field field : target.getDeclaredFields()) {
                if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType()) && index-- <= 0) {
                    field.setAccessible(true);

                    return new FieldAccessor<T>() {

                        @Override
                        public T get(Object target) {
                            try {
                                return (T) field.get(target);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Cannot access reflection.", e);
                            }
                        }

                        @Override
                        public void set(Object target, Object value) {
                            try {
                                field.set(target, value);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Cannot access reflection.", e);
                            }
                        }

                        @Override
                        public boolean hasField(Object target) {
                            return field.getDeclaringClass().isAssignableFrom(target.getClass());
                        }
                    };
                }
            }
            
            if (target.getSuperclass() != null)
                return getField(target.getSuperclass(), name, fieldType, index);

            throw new IllegalArgumentException("Cannot find field with type " + fieldType);
        }

        public static MethodInvoker getMethod(String className, String methodName, Class<?>... params) {
            return getTypedMethod(getClass(className), methodName, null, params);
        }

        public static MethodInvoker getMethod(Class<?> clazz, String methodName, Class<?>... params) {
            return getTypedMethod(clazz, methodName, null, params);
        }

        public static MethodInvoker getTypedMethod(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... params) {
            for (final Method method : clazz.getDeclaredMethods()) {
                if ((methodName == null || method.getName().equals(methodName))
                        && (returnType == null || method.getReturnType().equals(returnType))
                        && Arrays.equals(method.getParameterTypes(), params)) {
                    method.setAccessible(true);

                    return new MethodInvoker() {

                        @Override
                        public Object invoke(Object target, Object... arguments) {
                            try {
                                return method.invoke(target, arguments);
                            } catch (Exception e) {
                                throw new RuntimeException("Cannot invoke method " + method, e);
                            }
                        }

                    };
                }
            }

            if (clazz.getSuperclass() != null)
                return getMethod(clazz.getSuperclass(), methodName, params);

            throw new IllegalStateException(String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)));
        }

        public static ConstructorInvoker getConstructor(String className, Class<?>... params) {
            return getConstructor(getClass(className), params);
        }

        public static ConstructorInvoker getConstructor(Class<?> clazz, Class<?>... params) {
            for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (Arrays.equals(constructor.getParameterTypes(), params)) {
                    constructor.setAccessible(true);

                    return new ConstructorInvoker() {

                        @Override
                        public Object invoke(Object... arguments) {
                            try {
                                return constructor.newInstance(arguments);
                            } catch (Exception e) {
                                throw new RuntimeException("Cannot invoke constructor " + constructor, e);
                            }
                        }

                    };
                }
            }

            throw new IllegalStateException(String.format("Unable to find constructor for %s (%s).", clazz, Arrays.asList(params)));
        }

        public static Class<Object> getUntypedClass(String lookupName) {
            Class<Object> clazz = (Class<Object>) getClass(lookupName);
            return clazz;
        }

        public static Class<?> getClass(String lookupName) {
            return getCanonicalClass(expandVariables(lookupName));
        }

        public static Class<?> getMinecraftClass(String name) {
            return getCanonicalClass(NMS_PREFIX + "." + name);
        }

        public static Class<?> getCraftBukkitClass(String name) {
            return getCanonicalClass(OBC_PREFIX + "." + name);
        }

        private static Class<?> getCanonicalClass(String canonicalName) {
            try {
                return Class.forName(canonicalName);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot find " + canonicalName, e);
            }
        }

        private static String expandVariables(String name) {
            StringBuffer output = new StringBuffer();
            Matcher matcher = MATCH_VARIABLE.matcher(name);

            while (matcher.find()) {
                String variable = matcher.group(1);
                String replacement = "";

                if ("nms".equalsIgnoreCase(variable))
                    replacement = NMS_PREFIX;
                else if ("obc".equalsIgnoreCase(variable))
                    replacement = OBC_PREFIX;
                else if ("version".equalsIgnoreCase(variable))
                    replacement = VERSION;
                else
                    throw new IllegalArgumentException("Unknown variable: " + variable);

                if (replacement.length() > 0 && matcher.end() < name.length() && name.charAt(matcher.end()) != '.')
                    replacement += ".";
                matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(output);
            return output.toString();
        }
    }
    
    public static class ServerPingEvent extends Event implements Cancellable{

        private String address;
        private String motdline1;
        private String motdline2 = "";
        private List<String> hover;
        private int online;
        private int max;
        private String version;
        private String versionedit;
        private boolean cancel = false;
        
        public ServerPingEvent(String address, String motd, GameProfile[] hover, int online, int max, String ver) {
            this.address = address;
            final String[] lines = motd.split("\\n");
            this.motdline1 = lines[0];
            if(lines.length > 1) this.motdline2 = lines[1];
            this.hover = new ArrayList<>();
            for(GameProfile profile : hover) {
                if(profile == null|| profile.getName() == null || profile.getName().equals("")) {
                    this.hover.add("");
                } else {
                    this.hover.add(profile.getName());
                }
            }
            this.online = online;
            this.max = max;
            this.version = ver;
        }
        
        public String getAddress() {return this.address;}
        
        public String getMotdLine1() {return this.motdline1;}
        public void setMotdLine1(String s) {this.motdline1 = s;}
        
        public String getMotdLine2() {return this.motdline2;}
        public void setMotdLine2(String s) {this.motdline2 = s;}
        
        public List<String> getHover() {return this.hover;}
        public void setHover(List<String>  h) {this.hover = h;}
        
        public int getOnline() {return this.online;}
        public void setOnline(int o) {this.online = o;}
        
        public int getMax() {return this.max;}
        public void setMax(int m) {this.max = m;}
        
        public String getVersion() {return this.version;}
        public String getVersionEdit() {return this.versionedit;}
        public void setVersion(String v) {this.version = v; this.versionedit = v;}
        
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
} 
 
