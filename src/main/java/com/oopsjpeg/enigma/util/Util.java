package com.oopsjpeg.enigma.util;

import com.oopsjpeg.enigma.game.DamageEvent;
import com.oopsjpeg.enigma.game.Stats;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Util {
    public static final Random RANDOM = new Random();

    public static void sendError(MessageChannel channel, String error) {
        channel.sendMessage(Emote.NO + error).complete();
    }

    public static int nextInt(int min, int max) {
        return min + RANDOM.nextInt((max - min) + 1);
    }

    public static String formatStats(Stats stats) {
        List<String> output = new ArrayList<>();
        if (stats.get(Stats.MAX_HP) > 0)
            output.add("Max Health: +**" + stats.getInt(Stats.MAX_HP) + "**");
        if (stats.get(Stats.DAMAGE) > 0)
            output.add("Damage: +**" + stats.getInt(Stats.DAMAGE) + "**");
        if (stats.get(Stats.ABILITY_POWER) > 0)
            output.add("Ability Power: +**" + stats.getInt(Stats.ABILITY_POWER) + "**");
        if (stats.get(Stats.CRIT_CHANCE) > 0)
            output.add("Critical Chance: +**" + Util.percent(stats.get(Stats.CRIT_CHANCE)) + "**");
        if (stats.get(Stats.CRIT_DAMAGE) > 0)
            output.add("Critical Damage: +**" + Util.percent(stats.get(Stats.CRIT_DAMAGE)) + "**");
        if (stats.get(Stats.LIFE_STEAL) > 0)
            output.add("Life Steal: **" + Util.percent(stats.get(Stats.LIFE_STEAL)) + "**");
        return String.join("\n", output);
    }

    public static String formatPerTurn(Stats perTurn) {
        List<String> output = new ArrayList<>();
        if (perTurn.get(Stats.HP) > 0)
            output.add("Health/turn: +**" + perTurn.getInt(Stats.HP) + "**");
        if (perTurn.get(Stats.ENERGY) > 0)
            output.add("Energy/turn: +**" + perTurn.getInt(Stats.ENERGY) + "**");
        if (perTurn.get(Stats.GOLD) > 0)
            output.add("Gold/turn: +**" + perTurn.getInt(Stats.GOLD) + "**");
        return String.join("\n", output);
    }

    public static String percent(float x) {
        return Math.round(x * 100) + "%";
    }

    public static float limit(float x, float min, float max) {
        return Math.max(min, Math.min(max, x));
    }

    public static int limit(int x, int min, int max) {
        return (int) Math.ceil(limit((float) x, (float) min, (float) max));
    }

    public static String damageText(DamageEvent event, String attacker, String victim, String emote, String action) {
        return emote + "**" + attacker + "** " + action + " **" + victim + "** by **" + Math.round(event.damage)
                + "**" + (event.bonus > 0 ? " (+" + Math.round(event.bonus) + ")" : "") + "!" + (event.crit ? " **CRIT**!" : "")
                + (event.miss ? " **MISS**!" : "") + " [**" + event.target.getStats().getInt(Stats.HP)
                + " / " + event.target.getStats().getInt(Stats.MAX_HP) + "**]";
    }
}
