package com.wetjens.gwt.server.user.repository;

import javax.enterprise.context.ApplicationScoped;

import com.wetjens.gwt.server.user.domain.User;
import com.wetjens.gwt.server.user.domain.Users;

@ApplicationScoped
public class UserRepository implements Users {

    // Table
    // PK             | SK              | Data
    // FirstId          SecondId
    // Game-<GameId>    Game-<GameId>     Status=..., State=...
    // Game-<GameId>    User-<UserId>     Role=Owner, <Owner Attributes>
    // Game-<GameId>    User-<UserId>     Role=Player, <Player Attributes>
    // User-<UserId>    <Username>        Email=...
    //

    // GSI GamesByUserId
    // PK             | SK
    // SecondId       | FirstId         | Data
    // User-<UserId>  | <GameId>        |

    @Override
    public User findById(String id) {
        return null;
    }

}
