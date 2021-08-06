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

package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.api.domain.Action;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.server.rest.exception.APIError;
import com.boardgamefiesta.server.rest.exception.APIException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;

@JsonbTypeDeserializer(ActionRequest.Deserializer.class)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ActionRequest {

    private final JsonObject jsonObject;

    public static final class Deserializer implements JsonbDeserializer<ActionRequest> {
        @Override
        public ActionRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
            return new ActionRequest(jsonParser.getObject());
        }
    }

    public <T extends State> Action toAction(Game game, T state) {
        try {
            return game.getProvider().getActionMapper().toAction(jsonObject, state);
        } catch (JsonException e) {
            throw APIException.badRequest(APIError.INVALID_ACTION);
        }
    }

}
