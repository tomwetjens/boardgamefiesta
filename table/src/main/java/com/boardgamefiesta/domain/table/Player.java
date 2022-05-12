/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.table;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.Entity;
import com.boardgamefiesta.domain.exception.DomainException;
import com.boardgamefiesta.domain.user.User;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public class Player implements Entity {

    private static final int MIN_FORCE_END_TURNS_TO_KICK = 3;

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private final Type type;

    private final User.Id userId;

    @Getter
    @NonNull
    private final Instant created;

    @Getter
    @NonNull
    private Status status;

    @Getter
    @NonNull
    private Instant updated;

    private PlayerColor color;

    @Getter
    private boolean turn;

    private Instant turnLimit;

    @Getter
    private int forceEndTurns;

    private Integer score;

    private Boolean winner;

    static Player accepted(User.Id userId) {
        Instant created = Instant.now();

        return Player.builder()
                .id(Id.generate())
                .type(Type.USER)
                .userId(userId)
                .status(Status.ACCEPTED)
                .created(created)
                .updated(created)
                .build();
    }

    static Player invite(User.Id userId) {
        Instant invited = Instant.now();

        return Player.builder()
                .id(Id.generate())
                .type(Type.USER)
                .userId(userId)
                .status(Status.INVITED)
                .created(invited)
                .updated(invited)
                .build();
    }

    static Player computer() {
        Instant created = Instant.now();

        return Player.builder()
                .id(Id.generate())
                .type(Type.COMPUTER)
                .status(Status.ACCEPTED)
                .created(created)
                .updated(created)
                .build();
    }

    void accept() {
        if (status != Status.INVITED) {
            throw new AlreadyRespondedException();
        }

        status = Status.ACCEPTED;
        updated = Instant.now();
    }

    void reject() {
        if (status != Status.INVITED) {
            throw new AlreadyRespondedException();
        }

        status = Status.REJECTED;
        updated = Instant.now();
    }

    void assignColor(PlayerColor color) {
        if (this.color != null) {
            throw new AlreadyAssignedColor();
        }

        this.color = color;

        updated = Instant.now();
    }

    void assignScore(int score, boolean winner) {
        this.score = score;
        this.winner = winner;

        updated = Instant.now();
    }

    public Optional<PlayerColor> getColor() {
        return Optional.ofNullable(color);
    }

    void leave() {
        if (status == Status.LEFT) {
            throw new AlreadyLeftException();
        }

        if (status != Status.ACCEPTED && status != Status.PROPOSED_TO_LEAVE && status != Status.AGREED_TO_LEAVE) {
            throw new NotAcceptedException();
        }

        status = Status.LEFT;
        updated = Instant.now();
    }

    void proposeToLeave() {
        if (status != Status.ACCEPTED) {
            throw new NotAcceptedException();
        }

        status = Status.PROPOSED_TO_LEAVE;
        updated = Instant.now();
    }

    public void agreeToLeave() {
        if (status != Status.ACCEPTED) {
            throw new NotAcceptedException();
        }

        status = Status.AGREED_TO_LEAVE;
        updated = Instant.now();
    }

    public boolean isPlaying() {
        return status == Status.ACCEPTED || status == Status.PROPOSED_TO_LEAVE || status == Status.AGREED_TO_LEAVE;
    }

    public boolean isUser() {
        return type == Type.USER;
    }

    /**
     * Is this player still considered "active" in this table?
     * I.e. must the player see this table in their active tables overview.
     */
    public boolean isActive() {
        return status != Status.LEFT && status != Status.REJECTED;
    }

    public boolean hasResponded() {
        return status != Status.INVITED;
    }

    public boolean hasAgreedToLeave() {
        return status == Status.PROPOSED_TO_LEAVE || status == Status.AGREED_TO_LEAVE;
    }

    public void beginTurn(Duration timeLimit) {
        this.turn = true;
        this.turnLimit = Instant.now().plus(timeLimit);
        this.updated = Instant.now();
    }

    public void endTurn() {
        this.turn = false;
        this.turnLimit = null;
        this.updated = Instant.now();
    }

    public void forceEndTurn() {
        if (!canForceEndTurn()) {
            throw new CannotForceEndTurn();
        }

        forceEndTurns++;

        endTurn();
    }

    private boolean canForceEndTurn() {
        return isAfterTurnLimit();
    }

    public void kick() {
        if (!canKick()) {
            throw new CannotKick();
        }

        leave();
    }

    public boolean canKick() {
        return canKickAfterTurnLimit() && isAfterTurnLimit();
    }

    public boolean canKickAfterTurnLimit() {
        return forceEndTurns >= MIN_FORCE_END_TURNS_TO_KICK;
    }

    public boolean isAfterTurnLimit() {
        return turnLimit != null && !turnLimit.isAfter(Instant.now());
    }

    public Optional<User.Id> getUserId() {
        return Optional.ofNullable(userId);
    }

    public Optional<Integer> getScore() {
        return Optional.ofNullable(score);
    }

    public Optional<Boolean> getWinner() {
        return Optional.ofNullable(winner);
    }

    public Optional<Instant> getTurnLimit() {
        return Optional.ofNullable(turnLimit);
    }

    public com.boardgamefiesta.api.domain.Player asPlayer() {
        return new com.boardgamefiesta.api.domain.Player(id.id, color, type.asType());
    }

    public enum Status {
        INVITED,
        ACCEPTED,
        REJECTED,
        LEFT,
        PROPOSED_TO_LEAVE,
        AGREED_TO_LEAVE
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type {
        USER(com.boardgamefiesta.api.domain.Player.Type.HUMAN),
        COMPUTER(com.boardgamefiesta.api.domain.Player.Type.COMPUTER);

        private final com.boardgamefiesta.api.domain.Player.Type type;

        public com.boardgamefiesta.api.domain.Player.Type asType() {
            return type;
        }
    }

    @Value(staticConstructor = "of")
    public static class Id {
        String id;

        private static Player.Id generate() {
            return of(UUID.randomUUID().toString());
        }
    }

    public static final class AlreadyAssignedColor extends DomainException {
        public AlreadyAssignedColor() {
            super("ALREADY_ASSIGNED_COLOR");
        }
    }

    public static final class AlreadyRespondedException extends AggregateRoot.InvalidCommandException {
        public AlreadyRespondedException() {
            super("ALREADY_RESPONDED");
        }
    }

    public static final class NotAcceptedException extends AggregateRoot.InvalidCommandException {
        public NotAcceptedException() {
            super("NOT_ACCEPTED");
        }
    }

    public static final class AlreadyLeftException extends AggregateRoot.InvalidCommandException {
        public AlreadyLeftException() {
            super("ALREADY_LEFT");
        }
    }

    public static final class CannotForceEndTurn extends AggregateRoot.InvalidCommandException {
        public CannotForceEndTurn() {
            super("CANNOT_FORCE_END_TURN");
        }
    }

    public static final class CannotKick extends AggregateRoot.InvalidCommandException {
        public CannotKick() {
            super("CANNOT_KICK");
        }
    }
}
