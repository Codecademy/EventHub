package com.mobicrave.eventtracker.web.commands;

import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.model.User;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/users/add_or_update")
public class AddOrUpdateUser extends Command {
  private final EventTracker eventTracker;

  @Inject
  public AddOrUpdateUser(EventTracker eventTracker) {
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    int userId = eventTracker.addOrUpdateUser(new User.Builder(
        request.getParameter("external_user_id"),
        toProperties(request)).build());
    response.getWriter().println(userId);
  }
}
