package com.oopsjpeg.enigma.game.item;

import com.oopsjpeg.enigma.game.Stats;
import com.oopsjpeg.enigma.game.obj.Item;

public class Knife extends Item {
    public static final String NAME = "Knife";
    public static final Tree TREE = Tree.DAMAGE;
    public static final int COST = 250;
    public static final Stats STATS = new Stats()
            .put(Stats.DAMAGE, 5);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Tree getTree() {
        return TREE;
    }

    @Override
    public int getCost() {
        return COST;
    }

    @Override
    public Stats getStats() {
        return STATS;
    }
}
