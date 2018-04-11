/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay;

import java.util.Optional;

import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.vertx.auth.datastore.IAuthenticatable;
import de.braintags.vertx.auth.datastore.impl.DatastoreUser;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.exception.NoSuchRecordException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
  public static final void recoverContextUser(final RoutingContext context) {
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
  public static final void setContextUser(final RoutingContext context, final User user) {
    context.setUser(user);
    if (context.session() != null) {
      context.session().put(USER_PROPERTY_BT, user);
    }
  }

  /**
   * Gets the current context user if it exists.
   * 
   * @param context
   * @return the current user as {@link Optional}
   */
  public static final Optional<User> getContextUser(final RoutingContext context) {
    return Optional.ofNullable(context.session().get(USER_PROPERTY_BT));
  }

  /**
   * Perform a logout, means. removes current user and user infos from context and session
   *
   * @param context
   */
  public static final void logout(final RoutingContext context) {
    context.clearUser();
    if (context.session() != null) {
      context.session().remove(USER_PROPERTY_BT);
      removeCurrentUser(context);
      context.session().destroy();
    }
  }

  /**
   * If a user is logged in, this method fetches the fitting instance of {@link IAuthenticatable} from the datastore,
   * stores it in the context, so that it can be fetched again by this method or by the method
   * {@link #getCurrentUser(RoutingContext)}. Additionally the IAuthenticatable is stored as result in the result
   * handler
   *
   * @param context
   *          the current context
   * @param netRelay
   *          the instance of NetRelay
   * @param resultHandler
   *          the result habndler, which is getting a found instance of IAuthenticatable or null, if no user is logged
   *          in
   */
  public static final void getCurrentUser(final RoutingContext context, final NetRelay netRelay,
      final Handler<AsyncResult<IAuthenticatable>> resultHandler) {
    IAuthenticatable member = getCurrentUser(context);
    if (member != null) {
      resultHandler.handle(Future.succeededFuture(member));
    } else {
      recoverContextUser(context);
      User user = context.user();
      if (user == null) {
        resultHandler.handle(Future.succeededFuture(null));
      } else {
        readUser(context, netRelay, user, resultHandler);
      }
    }
  }

  /**
   * Fetch the instance of IAuthenticatable from the datastore and store it into the context
   *
   * @param context
   * @param netRelay
   * @param user
   * @param resultHandler
   */
  private static void readUser(final RoutingContext context, final NetRelay netRelay, final User user,
      final Handler<AsyncResult<IAuthenticatable>> resultHandler) {
    Class<? extends IAuthenticatable> mapperClass = getMapperClass(context, netRelay);
    if (user instanceof MongoUser) {
      readMongoUser(context, netRelay, (MongoUser) user, resultHandler, mapperClass);
    } else if (user instanceof DatastoreUser) {
      resultHandler.handle(Future.succeededFuture(((DatastoreUser) user).getAuthenticatable()));
    } else {
      Future<IAuthenticatable> future = Future
          .failedFuture(new UnsupportedOperationException("user type not supported: " + user.getClass().getName()));
      resultHandler.handle(future);
    }
  }

  /**
   * @param context
   * @param netRelay
   * @param user
   * @param resultHandler
   * @param mapperClass
   */
  private static void readMongoUser(final RoutingContext context, final NetRelay netRelay, final MongoUser user,
      final Handler<AsyncResult<IAuthenticatable>> resultHandler, final Class<? extends IAuthenticatable> mapperClass) {
    String id = user.principal().getString("_id");
    IQuery<? extends IAuthenticatable> query = netRelay.getDatastore().createQuery(mapperClass);
    query.setSearchCondition(ISearchCondition.isEqual(query.getMapper().getIdInfo().getIndexedField(), id));
    query.execute(qr -> {
      if (qr.failed()) {
        resultHandler.handle(Future.failedFuture(qr.cause()));
      } else {
        if (qr.result().size() <= 0) {
          resultHandler.handle(Future.failedFuture(new NoSuchRecordException(
              "no record found for principal with id " + id + " in mapper " + mapperClass.getName())));
        }
        qr.result().iterator().next(ir -> {
          if (ir.failed()) {
            resultHandler.handle(Future.failedFuture(ir.cause()));
          } else {
            IAuthenticatable auth = ir.result();
            setCurrentUser(auth, context);
            resultHandler.handle(Future.succeededFuture(auth));
          }
        });
      }
    });
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends IAuthenticatable> getMapperClass(final RoutingContext context, final NetRelay netRelay) {
    String mapperName = context.user().principal().getString(AuthenticationController.MAPPERNAME_IN_PRINCIPAL);
    if (mapperName == null) {
      throw new IllegalArgumentException("No mapper definition found in principal");
    }
    Class<? extends IAuthenticatable> mapperClass = netRelay.getSettings().getMappingDefinitions()
        .getMapperClass(mapperName);
    if (mapperClass == null) {
      throw new IllegalArgumentException("No MapperClass definition for: " + mapperName);
    }
    return mapperClass;
  }

  /**
   * Get a member, which was previously added during the session
   *
   * @param context
   * @return
   */
  public static IAuthenticatable getCurrentUser(final RoutingContext context) {
    return context.session().get(IAuthenticatable.CURRENT_USER_PROPERTY);
  }

  /**
   * Set the current user as property into the Context session
   *
   * @param user
   * @param context
   */
  public static final void setCurrentUser(final IAuthenticatable user, final RoutingContext context) {
    context.session().put(IAuthenticatable.CURRENT_USER_PROPERTY, user);
  }

  /**
   * Remove a current user from the session, if set
   *
   * @param context
   */
  public static final void removeCurrentUser(final RoutingContext context) {
    context.session().remove(IAuthenticatable.CURRENT_USER_PROPERTY);
  }

}
