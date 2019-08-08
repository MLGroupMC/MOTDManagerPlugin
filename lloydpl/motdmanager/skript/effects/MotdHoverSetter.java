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

public class MotdHoverSetter extends Effect {

    private Expression<String> hover;

    @SuppressWarnings("unchecked")
    @Override

    public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean paramKleenean, SkriptParser.ParseResult paramParseResult) {
        hover = (Expression<String>) expr[0];
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
            ((ServerPingEvent)e).setHover(
                hover.isSingle()
                    ? Collections.singletonList(hover.getSingle(e))
                    : Arrays.asList(hover.getAll(e))
            );
        } catch(Exception ignored) {}
    }
}