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

package com.boardgamefiesta.server.rest.user.view;

import com.boardgamefiesta.domain.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class UserView {

    String id;
    String username;
    String avatarUrl;
    String language;
    String email;
    String location;
    String timeZone;
    EmailPreferencesView emailPreferences;
    ColorPreferencesView colorPreferences;

    public UserView(User user) {
        this(user.getId(), user, null);
    }

    public UserView(User.Id userId, User user, User.Id viewer) {
        this.id = userId.getId();

        if (user != null) {
            this.username = user.getUsername();
            this.avatarUrl = user.getAvatarUrl().toString();
            this.language = user.getLanguage();
            this.location = user.getLocation().orElse(null);
            this.timeZone = user.getTimeZone().getId();

            if (user.getId().equals(viewer)) {
                this.email = user.getEmail();

                this.emailPreferences = new EmailPreferencesView(user.getEmailPreferences());
                this.colorPreferences = new ColorPreferencesView(user.getColorPreferences());
            }
        }
    }
}
