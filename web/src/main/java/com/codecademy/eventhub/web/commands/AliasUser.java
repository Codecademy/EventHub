package com.codecademy.eventhub.web.commands;

import com.codecademy.eventhub.EventHub;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/users/alias")
public class AliasUser extends Command {
  private final EventHub eventHub;

  @Inject
  public AliasUser(EventHub eventHub) {
    this.eventHub = eventHub;
  }

  @Override
  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    eventHub.aliasUser(
        request.getParameter("from_external_user_id"),
        request.getParameter("to_external_user_id"));
    response.getWriter().println("\"OK\"");
  }
}
