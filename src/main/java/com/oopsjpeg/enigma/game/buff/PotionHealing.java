package com.oopsjpeg.enigma.game.buff;

import com.oopsjpeg.enigma.game.Game;
import com.oopsjpeg.enigma.game.item.Potion;
import com.oopsjpeg.enigma.game.obj.Buff;

public class PotionHealing extends Buff {
    public PotionHealing(Game.Member source, int turns) {
        super(source, turns);
    }

    @Override
    public String onTurnStart(Game.Member member) {
        return member.heal(Potion.HEAL / Potion.TURNS, "Potion");
    }

    @Override
    public String getName() {
        return "Potion Healing";
    }
}
