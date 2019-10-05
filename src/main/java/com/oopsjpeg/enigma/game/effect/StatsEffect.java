package com.oopsjpeg.enigma.game.effect;

import com.oopsjpeg.enigma.game.Game;
import com.oopsjpeg.enigma.game.Stats;
import com.oopsjpeg.enigma.game.obj.Effect;
import com.oopsjpeg.enigma.util.Util;

public class StatsEffect extends Effect {
    private final Stats stats;
    private final Stats perTurn;

    public StatsEffect(Stats stats, Stats perTurn) {
        this.stats = stats;
        this.perTurn = perTurn;
    }

    public static StatsEffect stats(Stats stats) {
        return new StatsEffect(stats, new Stats());
    }

    public static StatsEffect perTurn(Stats perTurn) {
        return new StatsEffect(new Stats(), perTurn);
    }

    @Override
    public String getName() {
        return "Stats";
    }

    @Override
    public String getDesc() {
        return Util.formatStats(stats) + Util.formatPerTurn(perTurn);
    }

    @Override
    public Stats getStats(Game.Member member) {
        return stats;
    }

    @Override
    public Stats getPerTurn(Game.Member member) {
        return perTurn;
    }
}
