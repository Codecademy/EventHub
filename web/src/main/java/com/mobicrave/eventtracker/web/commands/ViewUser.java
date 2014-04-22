package com.mobicrave.eventtracker.web.commands;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mobicrave.eventtracker.EventTracker;
import com.mobicrave.eventtracker.model.User;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Path("/users/view")
public class ViewUser extends Command {
  private final Gson gson;
  private final EventTracker eventTracker;

  @Inject
  public ViewUser(Gson gson, EventTracker eventTracker) {
    this.gson = gson;
    this.eventTracker = eventTracker;
  }

  public synchronized void execute(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {
    List<Integer> userIds = Lists.transform(
        Arrays.asList(request.getParameterValues("user_id[]")), new Function<String, Integer>() {
      @Override
      public Integer apply(String input) {
        return Integer.parseInt(input);
      }
    });
    List<User> user = eventTracker.getUsers(userIds);
    response.getWriter().println(gson.toJson(user));
  }
}
