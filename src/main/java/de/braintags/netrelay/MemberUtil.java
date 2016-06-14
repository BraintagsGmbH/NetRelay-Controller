/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay;

import de.braintags.io.vertx.pojomapper.IDataStore;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.exception.NoSuchRecordException;
import de.braintags.netrelay.model.Member;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.impl.MongoUser;
import io.vertx.ext.web.RoutingContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class MemberUtil {
  public static final String USER_PROPERTY_BT = "userPropertyBt";

  /**
   * 
   */
  private MemberUtil() {
  }

  /**
   * Method checks, wether the context.user must be set from a value from the current session
   * 
   * @param context
   */
  public static final void recoverContextUser(RoutingContext context) {
    if (context.user() == null && context.session() != null && context.session().get(USER_PROPERTY_BT) != null) {
      context.setUser(context.session().get(USER_PROPERTY_BT));
    }
  }

  /**
   * Add the given user into the context and place it into the session, so that method
   * {@link #recoverContextUser(RoutingContext)} can recover it
   * 
   * @param context
   * @param user
   */
  public static final void setContextUser(RoutingContext context, User user) {
    context.setUser(user);
    if (context.session() != null) {
      context.session().put(USER_PROPERTY_BT, user);
    }
  }

  /**
   * Perform a logout, means. removes current user and user infos from context and session
   * 
   * @param context
   */
  public static final void logout(RoutingContext context) {
    context.clearUser();
    if (context.session() != null) {
      context.session().remove(USER_PROPERTY_BT);
      removeCurrentUser(context);
    }
  }

  /**
   * This method searches for a logged in User and returns it
   * 
   * @param context
   * @param mongoClient
   * @param collectionName
   * @param resultHandler
   */
  public static final void getCurrentUser(RoutingContext context, IDataStore datastore, Class mapperClass,
      Handler<AsyncResult<Member>> resultHandler) {
    User user = context.user();

    if (user == null) {
      UnsupportedOperationException ex = new UnsupportedOperationException(
          "To call this method a user must be logged in");
      resultHandler.handle(Future.failedFuture(ex));
      return;
    }

    if (user instanceof Member) {
      resultHandler.handle(Future.succeededFuture((Member) user));
    } else if (user instanceof MongoUser) {
      JsonObject principal = user.principal();
      String id = user.principal().getString("_id");
      IQuery<Member> query = datastore.createQuery(mapperClass);
      query.field(query.getMapper().getIdField().getName()).is(id);
      query.execute(qr -> {
        if (qr.failed()) {
          resultHandler.handle(Future.failedFuture(qr.cause()));
        } else {
          if (qr.result().size() <= 0) {
            resultHandler
                .handle(Future.failedFuture(new NoSuchRecordException("no record found for principal with id " + id)));
          }
          qr.result().iterator().next(ir -> {
            if (ir.failed()) {
              resultHandler.handle(Future.failedFuture(ir.cause()));
            } else {
              resultHandler.handle(Future.succeededFuture(ir.result()));
            }
          });
        }
      });
    } else {
      Future<Member> future = Future
          .failedFuture(new UnsupportedOperationException("user type not supported: " + user.getClass().getName()));
      resultHandler.handle(future);
      return;
    }
  }

  /**
   * Get a member, which was previously added during the session
   * 
   * @param context
   * @return
   */
  public static Member getCurrentUser(RoutingContext context) {
    return context.session().get(Member.CURRENT_USER_PROPERTY);
  }

  /**
   * Set the current user as property into the Context session
   * 
   * @param user
   * @param context
   */
  public static final void setCurrentUser(Member user, RoutingContext context) {
    context.session().put(Member.CURRENT_USER_PROPERTY, user);
  }

  /**
   * Remove a current user from the session, if set
   * 
   * @param context
   */
  public static final void removeCurrentUser(RoutingContext context) {
    context.session().remove(Member.CURRENT_USER_PROPERTY);
  }

}
