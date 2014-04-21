package com.atlassian.activeobjects.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface PackageExcludes
{
    String[] value() default {};
}
