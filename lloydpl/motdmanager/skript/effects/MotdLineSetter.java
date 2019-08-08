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
import java.util.Arrays;
import java.util.Collections;

public class MotdLineSetter extends Effect {

    private Expression<String> str;
    private Expression<Integer> line;

    @SuppressWarnings("unchecked")
    @Override

    public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean paramKleenean, SkriptParser.ParseResult paramParseResult) {
        line = (Expression<Integer>) expr[0];
        str = (Expression<String>) expr[1];
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
            int i = line.getSingle(e);
            if(i == 1)
                ((ServerPingEvent)e).setMotdLine1(str.getSingle(e));
            else if(i == 2)
                ((ServerPingEvent)e).setMotdLine2(str.getSingle(e));
        } catch(Exception ignored) {}
    }
}