package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/users/keys")
public class GetUserKeys extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public GetUserKeys(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    response.getWriter().println(gson.toJson(eventTracker.getUserKeys()));
  }
}
