package lloydpl.motdmanager.skript.exprs;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import lloydpl.motdmanager.ServerPingEvent;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

public class MotdLineGetter extends SimpleExpression<String> {

    private Expression<Integer> line;

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public boolean init(Expression<?>[] expr, int i, Kleenean kl, SkriptParser.ParseResult pr) {
        line = (Expression<Integer>) expr[0];
        return ScriptLoader.isCurrentEvent(ServerPingEvent.class);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if(e == null) return "<none>";
        int line = this.line.getSingle(e);
        if(line == 1)
            return ((ServerPingEvent) e).getMotdLine1();
        else if(line == 2)
            return ((ServerPingEvent) e).getMotdLine2();
        return "";
    }

    @Override
    protected String[] get(Event e) {
        int line = this.line.getSingle(e);
        if(line == 1)
            return new String[] {((ServerPingEvent) e).getMotdLine1()};
        else if(line == 2)
            return new String[] {((ServerPingEvent) e).getMotdLine2()};
        return new String[] {null};
    }
}