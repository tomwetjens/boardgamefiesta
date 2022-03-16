/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.email;import com.boardgamefiesta.domain.email.Emailer;
import com.boardgamefiesta.domain.email.Message;
import com.boardgamefiesta.domain.user.User;

import javax.enterprise.context.ApplicationScoped;

// TODO Refactor so it is not necessary to provide this bean
@ApplicationScoped
public class NoopEmailer implements Emailer {
    @Override
    public void sendEmailToUser(Message message, User user) {
        throw new UnsupportedOperationException();
    }
}
