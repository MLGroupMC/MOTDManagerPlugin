package lloydpl.motdmanager.skript.effects;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import lloydpl.motdmanager.Main;
import lloydpl.motdmanager.ServerPingEvent;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

public class MotdVersionSetter extends Effect {

    private Expression<String> str;

    @SuppressWarnings("unchecked")
    @Override

    public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean paramKleenean, SkriptParser.ParseResult paramParseResult) {
        str = (Expression<String>) expr[0];
        return ScriptLoader.isCurrentEvent(ServerPingEvent.class);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return null;
    }

    @Override
    protected void execute(Event e) {
        if(!Main.getInst().isSkriptMode())
            return;
        try {
            ((ServerPingEvent)e).setVersion(str.getSingle(e));
        } catch(Exception ignored) {}
    }
}