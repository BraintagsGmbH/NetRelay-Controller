package de.braintags.netrelay.controller.persist;

import java.util.List;

import de.braintags.io.vertx.util.security.CRUDPermissionMap;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import de.braintags.netrelay.controller.Action;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.controller.authentication.RedirectAuthHandlerBt;
import de.braintags.netrelay.model.IAuthenticatable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;

/**
 * DataAuthorizator checks, wether the current user is authorized to execute a persistence action ( if authorizations
 * are set )
 * See explanation in {@link AuthenticationController}, how to define permissions
 * 
 * @author Michael Remme
 * 
 */
public class DataAuthorizator {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(DataAuthorizator.class);

  private DataAuthorizator() {
  }

  /**
   * Check if an AuthHandler exists in Context and wether current user has the rights for all actions to be processed
   * 
   * @param context
   * @param resolvedCaptureCollections
   * @param handler
   *          is getting true, if rights to all actions are granted or if no Authentication handler is active;
   *          false, if right for only one action is not granted
   */
  public static void checkAuthorization(RoutingContext context, IAuthenticatable member,
      List<CaptureMap> resolvedCaptureCollections, Handler<AsyncResult<Boolean>> handler) {
    AuthHandler auth = context.get(AuthenticationController.AUTH_HANDLER_PROP);
    if (auth != null && auth instanceof RedirectAuthHandlerBt) {
      if (member == null) {
        // this is an error
        handler.handle(Future.failedFuture(
            new IllegalArgumentException("This should not happen, we need an instance of IAuthenticatable here")));
      } else {
        checkAuthorization(resolvedCaptureCollections, auth, member, handler);
      }
    } else {
      handler.handle(Future.succeededFuture(true));
    }
  }

  /**
   * @param resolvedCaptureCollections
   * @param auth
   * @param member
   * @param handler
   */
  private static void checkAuthorization(List<CaptureMap> resolvedCaptureCollections, AuthHandler auth,
      IAuthenticatable member, Handler<AsyncResult<Boolean>> handler) {
    boolean granted = true;
    for (CaptureMap map : resolvedCaptureCollections) {
      if (!checkAuthorization(auth, member, map)) {
        granted = false;
        break;
      }
    }
    handler.handle(Future.succeededFuture(granted));
  }

  /**
   * @param auth
   * @param member
   * @param map
   */
  private static boolean checkAuthorization(AuthHandler auth, IAuthenticatable member, CaptureMap map) {
    char crud = resolveCRUD(map);
    if (member.getRoles() == null || member.getRoles().isEmpty()) {
      if (((RedirectAuthHandlerBt) auth).getPermissionMap().hasPermission(CRUDPermissionMap.DEFAULT_PERMISSION_KEY_NAME,
          crud)) {
        return true;
      }
    } else {
      for (String group : member.getRoles()) {
        if (((RedirectAuthHandlerBt) auth).getPermissionMap().hasPermission(group, crud)) {
          return true;
        }
      }
    }
    LOGGER.info("No permission granted for action " + crud);
    return false;
  }

  private static char resolveCRUD(CaptureMap map) {
    String actionKey = map.get(PersistenceControllerNew.ACTION_CAPTURE_KEY);
    Action action = actionKey == null ? Action.DISPLAY : Action.valueOf(actionKey);
    LOGGER.info("action is " + action);
    return action.getCRUD();
  }

}
