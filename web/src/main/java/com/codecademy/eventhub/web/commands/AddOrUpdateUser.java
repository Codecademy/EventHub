package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;
import com.codecademy.eventhub.model.User;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/users/add_or_update")
public class AddOrUpdateUser extends Command {
  private final EventHub eventHub;

  @Inject
  public AddOrUpdateUser(EventHub eventHub) {
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    int userId = eventHub.addOrUpdateUser(new User.Builder(
        request.getParameter("external_user_id"),
        toProperties(request)).build());
    response.getWriter().println(userId);
  }
}
