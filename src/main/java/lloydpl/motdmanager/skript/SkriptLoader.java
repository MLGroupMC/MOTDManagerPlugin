package lloydpl.motdmanager.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.util.SimpleEvent;
import lloydpl.motdmanager.ServerPingEvent;
import lloydpl.motdmanager.skript.effects.*;
import lloydpl.motdmanager.skript.exprs.*;

public class SkriptLoader {

    public static void load() {
        Skript.registerEvent("server pinging", SimpleEvent.class, ServerPingEvent.class, "server pinging");

        Skript.registerEffect(MotdFakeSetter.class, "set motd fake player amount to %integer%");
        Skript.registerEffect(MotdHoverSetter.class, "set motd hover to %string%");
        Skript.registerEffect(MotdLineSetter.class, "set motd line %integer% to %integer%");
        Skript.registerEffect(MotdMaxSetter.class, "set motd max player amount to %integer%");
        Skript.registerEffect(MotdVersionSetter.class, "set motd version to %string%");

        Skript.registerExpression(MotdAddressGetter.class, String.class, ExpressionType.PROPERTY, "address of pinging person");
        Skript.registerExpression(MotdHoverGetter.class, String.class, ExpressionType.PROPERTY, "hover");
        Skript.registerExpression(MotdLineGetter.class, String.class, ExpressionType.PROPERTY, "motd line %integer%");
        Skript.registerExpression(MotdMaxGetter.class, Integer.class, ExpressionType.PROPERTY, "max player(|s) amount");
        Skript.registerExpression(MotdOnlineGetter.class, Integer.class, ExpressionType.PROPERTY, "online player(|s) amount");
        Skript.registerExpression(MotdVersionEditedGetter.class, String.class, ExpressionType.PROPERTY, "motd edited version");
        Skript.registerExpression(MotdVersionGetter.class, String.class, ExpressionType.PROPERTY, "motd original version");
    }

}
