package com.mobicrave.eventtracker.web.commands;

import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.filter.Filter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Path("/users/find")
public class FindUser extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public FindUser(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    Filter filter = getFilter(
        request.getParameterValues("ufk[]"),
        request.getParameterValues("ufv[]"));
    List<User> users = eventTracker.findUsers(filter);
    response.getWriter().println(gson.toJson(users));
  }
}
