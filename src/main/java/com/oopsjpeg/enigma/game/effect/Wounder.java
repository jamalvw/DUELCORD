package com.oopsjpeg.enigma.game.effect;

import com.oopsjpeg.enigma.game.DamageEvent;
import com.oopsjpeg.enigma.game.buff.Wound;
import com.oopsjpeg.enigma.game.obj.Effect;
import com.oopsjpeg.enigma.util.Util;

public class Wounder extends Effect {
    public static final String NAME = "Wounder";
    private final float power;

    public Wounder(float power) {
        this.power = power;
    }

    @Override
    public DamageEvent onHit(DamageEvent event) {
        event.output.add(event.target.buff(new Wound(event.actor, 1, power)));
        return event;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Reduces the target's healing and shielding by **" + Util.percent(power) + "** on hit.";
    }

    @Override
    public float getPower() {
        return power;
    }
}
