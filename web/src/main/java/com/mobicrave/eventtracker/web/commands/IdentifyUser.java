package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.model.User;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/users/identify")
public class IdentifyUser extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public IdentifyUser(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    int userId = eventTracker.getOrCreatedUserId(request.getParameter("external_user_id"));
    User user = eventTracker.getUser(userId);
    response.getWriter().println(gson.toJson(user));
  }
}
