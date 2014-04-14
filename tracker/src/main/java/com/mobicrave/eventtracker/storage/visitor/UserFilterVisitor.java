package com.mobicrave.eventtracker.storage.visitor;

import com.mobicrave.eventtracker.model.User;
import com.mobicrave.eventtracker.storage.filter.ExactMatch;
import com.mobicrave.eventtracker.storage.filter.Regex;

public class UserFilterVisitor implements Visitor {
  private final User user;

  public UserFilterVisitor(User user) {
    this.user = user;
  }

  @Override
  public boolean visit(ExactMatch exactMatch) {
    return exactMatch.getValue().equals(user.get(exactMatch.getKey()));
  }

  @Override
  public boolean visit(Regex regex) {
    return regex.getPattern().matcher(user.get(regex.getKey())).matches();
  }
}
