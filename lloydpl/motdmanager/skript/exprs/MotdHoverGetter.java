package lloydpl.motdmanager.skript.exprs;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import lloydpl.motdmanager.ServerPingEvent;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

public class MotdHoverGetter extends SimpleExpression<String> {

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean init(Expression<?>[] expr, int i, Kleenean kl, SkriptParser.ParseResult pr) {
        return ScriptLoader.isCurrentEvent(ServerPingEvent.class);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if(e == null) return "<none>";
        return ((ServerPingEvent) e).getHover().toString();
    }

    @Override
    protected String[] get(Event e) {
        return ((ServerPingEvent) e).getHover().toArray(new String[0]);
    }
}