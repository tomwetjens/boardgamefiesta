package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.auth.Roles;
import com.boardgamefiesta.server.rest.CurrentUser;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.Data;
import lombok.NonNull;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.USER)
public class UserResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    Users users;

    @GET
    public UserView get() {
        var user = currentUser.get();
        return new UserView(user.getId(), user, user.getId());
    }

    @POST
    @Path("/change-username")
    public void changeUsername(ChangeUsernameRequest request) {
        var user = currentUser.get();

        user.changeUsername(request.getUsername());

        users.update(user);
    }

    @POST
    @Path("/change-email-preferences")
    public void changeEmailPreferences(@NonNull ChangeEmailPreferences changeEmailPreferences) {
        var user = currentUser.get();

        if (changeEmailPreferences.getSendInviteEmail() != null) {
            user.getEmailPreferences().setSendInviteEmail(changeEmailPreferences.getSendInviteEmail());
        }
        if (changeEmailPreferences.getTurnBasedPreferences() != null) {
            if (changeEmailPreferences.getTurnBasedPreferences().getSendTurnEmail() != null) {
                user.getEmailPreferences().getTurnBasedPreferences().setSendTurnEmail(changeEmailPreferences.getTurnBasedPreferences().getSendTurnEmail());
            }
            if (changeEmailPreferences.getTurnBasedPreferences().getSendEndedEmail() != null) {
                user.getEmailPreferences().getTurnBasedPreferences().setSendEndedEmail(changeEmailPreferences.getTurnBasedPreferences().getSendEndedEmail());
            }
        }

        users.update(user);
    }


    @POST
    @Path("/delete")
    public void delete(DeleteCommand request) {
        if (request.isConfirm()) {
            var user = currentUser.get();

            user.markDeleted();

            users.update(user);
        }
    }

    @Data
    public static class DeleteCommand {
        boolean confirm;
    }
}
