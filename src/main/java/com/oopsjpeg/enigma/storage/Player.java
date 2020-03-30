package com.oopsjpeg.enigma.storage;

import com.oopsjpeg.enigma.Enigma;
import com.oopsjpeg.enigma.game.Game;
import com.oopsjpeg.enigma.game.GameMode;
import com.oopsjpeg.enigma.game.obj.Unit;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Player {
    @Getter private final long id;
    private transient GameMode queueMode;
    @Getter @Setter private transient Game game;
    @Getter @Setter private int gems;
    @Getter @Setter private int wins;
    @Getter @Setter private int losses;
    private List<UnitData> unitDatas;

    public Player(long id) {
        this.id = id;
    }

    public User getUser() {
        return Enigma.getInstance().getClient().getUserById(Snowflake.of(id)).block();
    }

    public Member getMember(Snowflake guildId) {
        return getUser().asMember(guildId).block();
    }

    public String getUsername() {
        return getUser().getUsername();
    }

    public GameMode getQueueMode() {
        return queueMode;
    }

    public List<Player> getQueue() {
        return queueMode != null ? Enigma.getInstance().getQueue(queueMode) : null;
    }

    public void setQueue(GameMode mode) {
        if (queueMode == mode) return;
        if (queueMode != null) getQueue().remove(this);
        queueMode = mode;
        if (queueMode != null) getQueue().add(this);
    }

    public void removeQueue() {
        setQueue(null);
    }

    public void removeGame() {
        setGame(null);
    }

    public void addGems(int gems) {
        this.gems += gems;
    }

    public void removeGems(int gems) {
        this.gems -= gems;
    }

    public void win() {
        wins++;
    }

    public void lose() {
        losses++;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public float getWinRate() {
        return getTotalGames() > 0 ? (float) wins / getTotalGames() : 0;
    }

    public List<UnitData> getUnitDatas() {
        if (unitDatas == null)
            unitDatas = new ArrayList<>();
        return unitDatas;
    }

    public UnitData getUnitData(String unitName) {
        return getUnitDatas().stream()
                .filter(ud -> ud.unitName.equalsIgnoreCase(unitName))
                .findAny().orElseGet(() -> {
                    UnitData data = new UnitData(unitName);
                    getUnitDatas().add(data);
                    return data;
                });
    }

    @Override
    public int hashCode() {
        return getUser().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Player && ((Player) o).id == id) || o.equals(getUser());
    }

    public static class UnitData {
        private String unitName;
        private int points;

        public UnitData(String unitName) {
            this.unitName = unitName;
        }

        public Unit getUnit() {
            return Unit.fromName(unitName);
        }

        public String getUnitName() {
            return unitName;
        }

        public void setUnitName(String unitName) {
            this.unitName = unitName;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = Math.max(0, points);
        }

        public void addPoints(int points) {
            setPoints(getPoints() + points);
        }
    }
}
