package com.mobicrave.eventtracker.web.commands;

import java.lang.annotation.ElementType;

@java.lang.annotation.Target({ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Path {
  String value();
}
