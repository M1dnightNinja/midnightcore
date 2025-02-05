package org.wallentines.mcore.requirement;

import org.wallentines.mcore.Player;
import org.wallentines.mdcfg.TypeReference;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.requirement.Check;
import org.wallentines.midnightlib.requirement.CheckType;

import java.util.function.BiPredicate;

public class PlayerCheck<T> implements Check<Player> {

    private final Type<T> type;
    private final T value;

    public PlayerCheck(Type<T> type, T value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean check(Player player) {
        return type.checker.test(player, value);
    }

    @Override
    public Type<T> type() {
        return type;
    }

    public T value() {
        return value;
    }

    public static class Type<T> implements CheckType<Player, PlayerCheck<T>> {

        private BiPredicate<Player, T> checker;
        private Serializer<PlayerCheck<T>> serializer;

        public Type(BiPredicate<Player, T> checker, Serializer<T> serializer) {

            this.serializer = serializer.flatMap(PlayerCheck<T>::value, val -> new PlayerCheck<>(this, val));

        }

        @Override
        public Serializer<PlayerCheck<T>> serializer() {
            return serializer;
        }
    }
}
