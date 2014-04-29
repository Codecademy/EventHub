package com.mobicrave.eventtracker.web.commands;

import com.mobicrave.eventtracker.EventTracker;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/users/alias")
public class AliasUser extends Command {
  private final EventTracker eventTracker;

  @Inject
  public AliasUser(EventTracker eventTracker) {
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    eventTracker.aliasUser(
        request.getParameter("from_external_user_id"),
        request.getParameter("to_external_user_id"));
    response.getWriter().println("\"OK\"");
  }
}
