package com.boardgamefiesta.server.rest.table.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.json.jackson.JacksonJsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.util.Optional;

@JsonSerialize(using = StateView.JsonSerializer.class)
@Value
public class StateView {

    ViewMapper<State> viewMapper;
    Player viewer;
    State state;

    public StateView(@NonNull Table table,
                     @NonNull State state,
                     User.Id currentUserId) {
        this.viewMapper = table.getGame().getProvider().getViewMapper();
        this.state = state;
        this.viewer = Optional.ofNullable(currentUserId)
                .flatMap(table::getPlayerByUserId)
                .map(com.boardgamefiesta.domain.table.Player::asPlayer)
                .orElse(null);
    }

    public static final class JsonSerializer extends com.fasterxml.jackson.databind.JsonSerializer<StateView> {
        @Override
        public void serialize(StateView obj, com.fasterxml.jackson.core.JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (obj.viewMapper.isJsonGeneratorSupported()) {
                obj.viewMapper.serialize(obj.state, obj.viewer, new JacksonJsonGenerator(jsonGenerator));
            } else {
                serializerProvider.defaultSerializeValue(obj.viewMapper.toView(obj.state, obj.viewer), jsonGenerator);
            }
        }
    }
}
