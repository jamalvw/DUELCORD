package com.oopsjpeg.enigma.game;

import com.oopsjpeg.enigma.game.object.Buff;
import com.oopsjpeg.enigma.game.object.Effect;
import com.oopsjpeg.enigma.game.object.Item;
import com.oopsjpeg.enigma.game.object.Unit;
import com.oopsjpeg.enigma.storage.Player;
import com.oopsjpeg.enigma.util.ChanceBag;
import com.oopsjpeg.enigma.util.Emote;
import com.oopsjpeg.enigma.util.Util;
import discord4j.core.object.entity.User;

import java.util.*;
import java.util.stream.Collectors;

import static com.oopsjpeg.enigma.game.Stats.*;

public class GameMember {
    private final Game game;
    private final Player player;
    private Unit unit;
    private boolean alive = true;
    private boolean defensive = false;

    private final GameMemberVars vars = new GameMemberVars();

    private final List<Item> items = new ArrayList<>();
    private final Map<Class<? extends Effect>, Effect> effects = new HashMap<>();
    private final List<Buff> buffs = new ArrayList<>();

    private ChanceBag critBag = new ChanceBag(0, 0.5f);

    private int health = 0;
    private int gold = 0;
    private int energy = 0;
    private int shield = 0;

    private Stats stats = new Stats();

    public GameMember(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public User getUser() {
        return player.getUser();
    }

    public String getUsername() {
        return getUser().getUsername();
    }

    public String getMention() {
        return getUser().getMention();
    }

    public Player.UnitData getUnitData() {
        return getPlayer().getUnitData(unit.getName());
    }

    public float getRankedPoints() {
        return getPlayer().getRankedPoints();
    }

    public List<GameObject> getData() {
        List<GameObject> data = new ArrayList<>();
        data.add(getUnit());
        data.addAll(getItems());
        data.addAll(getEffects());
        data.addAll(getBuffs());
        return data;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Effect> getEffects() {
        return new ArrayList<>(effects.values());
    }

    public Effect getEffect(Class<? extends Effect> effect) {
        return effects.get(effect);
    }

    public void addEffect(Effect effect) {
        effects.put(effect.getClass(), effect);
    }

    public boolean hasEffect(Effect effect) {
        return effects.containsKey(effect.getClass());
    }

    public List<Buff> getBuffs() {
        return buffs;
    }

    public boolean hasBuff(Class<? extends Buff> buffType) {
        return buffs.stream().anyMatch(buff -> buff.getClass().equals(buffType));
    }

    public boolean alreadyPickedUnit() {
        return getUnit() != null;
    }

    public void updateStats() {
        effects.clear();

        stats.putAll(unit.getStats());

        for (Item item : getItems()) {
            stats.addAll(item.getStats());

            for (Effect newEffect : item.getEffects())
                if (!hasEffect(newEffect)) addEffect(newEffect);
                else {
                    // If this effect is stronger than the old one, replace it
                    Effect oldEffect = getEffect(newEffect.getClass());
                    if (newEffect.getPower() > oldEffect.getPower())
                        addEffect(newEffect);
                }
        }

        for (Effect effect : getEffects())
            stats.addAll(effect.getStats());

        for (Buff buff : getBuffs())
            stats.addAll(buff.getStats());

        critBag.setChance(stats.get(CRIT_CHANCE));
    }

    public void act(GameAction action) {
        if (getEnergy() < action.getEnergy())
            Util.sendFailure(game.getChannel(), "You do not have **" + action.getEnergy() + "** energy.");
        else {
            game.getChannel().createMessage(action.execute(this)).block();
            game.getActions().add(action);
            takeEnergy(action.getEnergy());
            if (!hasEnergy()) game.nextTurn();
            else game.updateInfo(this);
        }
    }

    public String buff(Buff buff) {
        buffs.add(buff);
        return Emote.BLEED + "**" + buff.getSource().getUsername() + "** applied **" + buff.getName() + "** " +
                (buff.hasPower() ? "(" + buff.formatPower() + ") " : "") +
                (buff.getTotalTurns() > 1 ? "for **" + buff.getTotalTurns() + "** turns" : "") + "!";
    }

    public String shield(float shieldAmount) {
        for (GameObject o : getData()) shieldAmount = o.onShield(shieldAmount);

        giveShield(Math.round(shieldAmount));

        return Emote.HEAL + "**" + getUsername() + "** shielded by **" + Math.round(shieldAmount)
                + "**! [**" + getShield() + "**]";
    }

    public String heal(float healAmount) {
        return heal(healAmount, null, true);
    }

    public String heal(float healAmount, String source) {
        return heal(healAmount, source, true);
    }

    public String heal(float healAmount, String source, boolean message) {
        for (GameObject o : getData()) healAmount = o.onHeal(healAmount);

        giveHealth(Math.round(healAmount));

        if (message)
            return Emote.HEAL + "**" + getUsername() + "** healed by **" + Math.round(healAmount) + "**! [**"
                + getHealth() + " / " + stats.getInt(MAX_HEALTH) + "**]"
                + (source == null ? "" : " (" + source + ")");

        return null;
    }

    public String defend() {
        if (!defensive) {
            defensive = true;
            List<String> output = getData().stream().map(o -> o.onDefend(this)).collect(Collectors.toList());
            output.add(Emote.SHIELD + "**" + getUsername() + "** is defending (**" + Util.percent(getResist()) + "** resist, **" + (stats.getInt(HEALTH_PER_TURN) * 2) + "** regen)!");
            return Util.joinNonEmpty("\n", output);
        }
        return null;
    }

    public DamageEvent hit(DamageEvent event) {
        for (GameObject o : event.actor.getData()) event = o.hitOut(event);
        for (GameObject o : event.target.getData()) event = o.hitIn(event);

        // Life steal healing
        if (stats.get(LIFE_STEAL) > 0)
            event.heal += stats.get(LIFE_STEAL) * event.damage;

        return event;
    }

    public DamageEvent crit(DamageEvent event) {
        // Crit checks
        if (event.crit || !event.miss && critBag.get()) {
            // Pseudo RNG crit bag
            event.crit = true;

            for (GameObject o : event.actor.getData()) event = o.critOut(event);
            for (GameObject o : event.target.getData()) event = o.critIn(event);
        }

        // Critical strike bonus damage
        if (event.crit) {
            event.critMul += .5f + stats.get(CRIT_DAMAGE);
            event.damage += event.damage * event.critMul;
        }

        return event;
    }

    public DamageEvent ability(DamageEvent event) {
        for (GameObject o : event.actor.getData()) event = o.abilityOut(event);
        for (GameObject o : event.target.getData()) event = o.abilityIn(event);
        return event;
    }

    public DamageEvent attack(GameMember target) {
        DamageEvent event = new DamageEvent(game, this, target);
        event.damage += stats.get(ATTACK_POWER);
        event.actor.giveGold(game.getMode().handleGold(Math.round(Util.nextInt(20, 30) + (game.getTurnCount() * 0.5f))));

        for (GameObject o : event.actor.getData()) event = o.attackOut(event);
        for (GameObject o : event.target.getData()) event = o.attackIn(event);

        event = hit(event);
        event = crit(event);

        return event;
    }

    public String damage(DamageEvent event, String emote) {
        return damage(event, emote, "");
    }

    public String damage(DamageEvent event, String emote, String source) {
        for (GameObject o : event.actor.getData()) event = o.damageOut(event);
        for (GameObject o : event.target.getData()) event = o.damageIn(event);

        event = game.getMode().handleDamage(event);

        if (event.heal > 0) event.output.add(heal(Math.round(event.heal)));
        if (event.shield > 0) event.output.add(shield(Math.round(event.shield)));

        event.damage *= 1 - event.target.getResist();
        event.bonus *= 1 - event.target.getResist();

        // Shield damaging
        if (event.target.hasShield()) {
            // Remove bonus damage first
            float shdBonus = Util.limit(event.bonus, 0, event.target.getShield());
            float shdDamage = 0;
            event.target.takeShield(Math.round(shdBonus));

            // Remove main damage after
            if (event.target.hasShield()) {
                shdDamage = Util.limit(event.damage, 0, event.target.getShield());
                event.target.takeShield(Math.round(shdDamage));
            }

            if (event.target.hasShield())
                event.output.add(0, Util.damageText(event, event.actor.getUsername(), event.target.getUsername() + "'s Shield", emote, source));
            else
                event.output.add(Emote.SHIELD + "**" + event.actor.getUsername() + "** destroyed **" + event.target.getUsername() + "'s Shield**!");

            event.bonus -= shdBonus;
            event.damage -= shdDamage;
        }

        if (!event.target.hasShield() && event.total() > 0) {
            event.target.takeHealth(Math.round(event.total()));
            event.output.add(0, Util.damageText(event, event.actor.getUsername(), event.target.getUsername(), emote, source));
            if (!event.target.hasHealth())
                event.output.add(event.target.lose());
        }

        return Util.joinNonEmpty("\n", event.output);
    }

    public String win() {
        game.getInstance().endGame(game);
        return Emote.TROPHY + getUser().getMention() + ", you have won the game!\n";
    }

    public String lose() {
        List<String> output = new ArrayList<>();
        output.add(Emote.SKULL + getUser().getMention() + " has been slain and removed from the game!");

        alive = false;

        if (game.getAlive().size() == 1) {
            game.setGameState(GameState.FINISHED);
            output.add(game.getAlive().get(0).win());
        } else if (game.getCurrentMember().equals(this))
            game.nextTurn();

        return Util.joinNonEmpty("\n", output);
    }

    public float getResist() {
        return stats.get(RESIST) + (defensive ? 0.2f : 0);
    }

    public float getBonusDamage() {
        return stats.get(ATTACK_POWER) - unit.getStats().get(ATTACK_POWER);
    }

    public float getBonusHealth() {
        return stats.get(MAX_HEALTH) - unit.getStats().get(MAX_HEALTH);
    }

    @Override
    public String toString() {
        return getPlayer().toString();
    }

    public Game getGame() {
        return this.game;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Unit getUnit() {
        return this.unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;

        items.clear();
        effects.clear();
        buffs.clear();

        updateStats();

        items.add(Item.POTION);

        setHealth(stats.getInt(MAX_HEALTH));
        setGold(game.getMode().handleGold(175 + (100 * game.getAlive().indexOf(this))));

        game.getCommandListener().getCommands().addAll(Arrays.asList(unit.getCommands()));

        //if (unit instanceof Berserker)
        //    ((Berserker) unit).getRage().setCurrent(game.getAlive().indexOf(this));
    }

    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isDefensive() {
        return this.defensive;
    }

    public void setDefensive(boolean defensive) {
        this.defensive = defensive;
    }

    public ChanceBag getCritBag() {
        return this.critBag;
    }

    public void setCritBag(ChanceBag critBag) {
        this.critBag = critBag;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int healthAmount) {
        health = Util.limit(healthAmount, 0, stats.getInt(MAX_HEALTH));
    }

    public int giveHealth(int healthAmount) {
        setHealth(getHealth() + healthAmount);
        return getHealth();
    }

    public int takeHealth(int healthAmount) {
        setHealth(getHealth() - healthAmount);
        return getHealth();
    }

    public boolean hasHealth(int healthAmount) {
        return getHealth() > healthAmount;
    }

    public boolean hasHealth() {
        return hasHealth(0);
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energyAmount) {
        energy = Util.limit(energyAmount, 0, stats.getInt(MAX_ENERGY));
    }

    public int giveEnergy(int energyAmount) {
        setEnergy(getEnergy() + energyAmount);
        return getEnergy();
    }

    public int takeEnergy(int energyAmount) {
        setEnergy(getEnergy() - energyAmount);
        return getEnergy();
    }

    public boolean hasEnergy(int energyAmount) {
        return getEnergy() > energyAmount;
    }

    public boolean hasEnergy() {
        return hasEnergy(0);
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int goldAmount) {
        gold = Math.max(goldAmount, 0);
    }

    public int giveGold(int goldAmount) {
        setGold(getGold() + goldAmount);
        return getGold();
    }

    public int takeGold(int goldAmount) {
        setGold(getGold() - goldAmount);
        return getGold();
    }

    public boolean hasGold(int goldAmount) {
        return getGold() >= goldAmount;
    }

    public int getGoldDifference(int targetGoldAmount) {
        return targetGoldAmount - getGold();
    }

    public int getShield() {
        return shield;
    }

    public void setShield(int shieldAmount) {
        shield = Math.max(shieldAmount, 0);
    }

    public int giveShield(int shieldAmount) {
        setShield(getShield() + shieldAmount);
        return getShield();
    }

    public int takeShield(int shieldAmount) {
        setShield(getShield() - shieldAmount);
        return getShield();
    }

    public boolean hasShield() {
        return getShield() > 0;
    }

    public Stats getStats() {
        return this.stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public GameMemberVars getVars() {
        return vars;
    }
}