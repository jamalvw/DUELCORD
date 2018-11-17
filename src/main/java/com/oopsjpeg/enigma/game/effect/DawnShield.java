package com.oopsjpeg.enigma.game.effect;

import com.oopsjpeg.enigma.game.Game;
import com.oopsjpeg.enigma.game.Stats;
import com.oopsjpeg.enigma.game.obj.Effect;

public class DawnShield extends Effect {
	@Override
	public String getName() {
		return "Dawn Shield";
	}

	@Override
	public String getDesc() {
		return "Left over energy in a turn shields for the same amount.";
	}

	@Override
	public String onTurnEnd(Game.Member user) {
		return user.shield(user.getStats().get(Stats.ENERGY));
	}
}
