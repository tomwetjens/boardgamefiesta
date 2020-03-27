package com.wetjens.gwt;

import java.util.Collection;

public class RailroadTrack {

    public RailroadTrack(Collection<Player> players) {
        //TODO
    }

    public Space current(Player player) {
        //TODO
        return null;
    }

    public ImmediateActions moveEngineForward(Player player,Space to, int atLeast, int atMost) {
        Space from = current(player);

        if (!to.isAfter(from)){
            throw new IllegalStateException("Position must be after current");
        }

        // TODO

        return ImmediateActions.none();
    }

    public ImmediateActions moveEngineBackwards(Player player,Space to, int atLeast, int atMost) {
        // TODO
        return ImmediateActions.none();
    }

    public class Space {
        public int spaces(Space to) {
            //TODO
            return 0;
        }

        public boolean isAfter(Space space) {
            //TODO
            return false;
        }
    }
}
