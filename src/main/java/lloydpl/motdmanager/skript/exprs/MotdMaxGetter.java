package lloydpl.motdmanager.skript.exprs;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import lloydpl.motdmanager.ServerPingEvent;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

public class MotdMaxGetter extends SimpleExpression<Integer> {

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public boolean init(Expression<?>[] expr, int i, Kleenean kl, SkriptParser.ParseResult pr) {
        return ScriptLoader.isCurrentEvent(ServerPingEvent.class);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (e == null) return "<none>";
        return ((ServerPingEvent) e).getMax() + "";
    }

    @Override
    protected Integer[] get(Event e) {
        return new Integer[]{((ServerPingEvent) e).getMax()};
    }
}