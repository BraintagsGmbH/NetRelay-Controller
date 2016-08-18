/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.persistence;

import java.util.Arrays;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.dataaccess.query.IQueryResult;
import de.braintags.io.vertx.pojomapper.exception.NoSuchRecordException;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Executes the commands to search for a record or for records and to put them into the context.
 * 
 * @author Michael Remme
 * 
 */
public class DisplayAction extends AbstractAction {

  /**
   * 
   */
  public DisplayAction(PersistenceController persitenceController) {
    super(persitenceController);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.persistence.AbstractAction#handle(io.vertx.ext.web.RoutingContext,
   * de.braintags.netrelay.controller.AbstractCaptureController.CaptureMap)
   */
  @Override
  void handle(String entityName, RoutingContext context, CaptureMap map, Handler<AsyncResult<Void>> handler) {
    IMapper mapper = getMapper(entityName);
    IQuery<?> query = getPersistenceController().getNetRelay().getDatastore().createQuery(mapper.getMapperClass());
    RecordContractor.extractId(mapper, map, query);
    addQueryCritera(query, map);
    handleQuery(query, entityName, context, map, mapper, handler);
  }

  protected void handleQuery(IQuery<?> query, String entityName, RoutingContext context, CaptureMap map, IMapper mapper,
      Handler<AsyncResult<Void>> handler) {
    try {
      query.execute(result -> {
        if (result.failed()) {
          handler.handle(Future.failedFuture(result.cause()));
        } else {
          IQueryResult<?> qr = result.result();
          if (query.hasQueryArguments()) {
            storeSingleResult(entityName, context, handler, qr);
          } else {
            storeListResult(entityName, context, handler, qr);
          }
        }
      });
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  /**
   * @param entityName
   * @param context
   * @param handler
   * @param qr
   */
  private void storeListResult(String entityName, RoutingContext context, Handler<AsyncResult<Void>> handler,
      IQueryResult<?> qr) {
    qr.toArray(arr -> {
      if (arr.failed()) {
        handler.handle(Future.failedFuture(arr.cause()));
      } else {
        addToContext(context, entityName, Arrays.asList(arr.result()));
        handler.handle(Future.succeededFuture());
      }
    });
  }

  /**
   * @param entityName
   * @param context
   * @param handler
   * @param qr
   */
  private void storeSingleResult(String entityName, RoutingContext context, Handler<AsyncResult<Void>> handler,
      IQueryResult<?> qr) {
    if (qr.isEmpty()) {
      handler.handle(Future.failedFuture(
          new NoSuchRecordException(String.format(ERRORMESSAGE_RECNOTFOUND, qr.getOriginalQuery().toString()))));
    } else {
      qr.iterator().next(ir -> {
        if (ir.failed()) {
          handler.handle(Future.failedFuture(ir.cause()));
        } else {
          addToContext(context, entityName, ir.result());
          handler.handle(Future.succeededFuture());
        }
      });
    }
  }

  /**
   * @param query
   * @param map
   */
  private void addQueryCritera(IQuery<?> query, CaptureMap map) {
    if (map.containsKey(PersistenceController.SELECTION_SIZE_CAPTURE_KEY)) {
      query.setLimit(Integer.parseInt(map.get(PersistenceController.SELECTION_SIZE_CAPTURE_KEY)));
    }
    if (map.containsKey(PersistenceController.SELECTION_START_CAPTURE_KEY)) {
      query.setStart(Integer.parseInt(map.get(PersistenceController.SELECTION_START_CAPTURE_KEY)));
    }
    if (map.containsKey(PersistenceController.ORDERBY_CAPTURE_KEY)) {
      addSortDefintions(query, map);
    }
  }

  private void addSortDefintions(IQuery<?> query, CaptureMap map) {
    String str = map.get(PersistenceController.ORDERBY_CAPTURE_KEY);
    String tmpString = str;
    do {
      tmpString = str;
      str = str.replaceAll("  ", " ");
      str = str.replaceAll(", ", ",");
    } while (!tmpString.equals(str));

    String[] defs = str.split(",");
    for (String def : defs) {
      String[] sort = def.split(" ");
      if (sort.length == 1) {
        query.addSort(sort[0]);
      } else if (sort.length == 2) {
        boolean ascending = sort[1].equalsIgnoreCase("asc");
        query.addSort(sort[0], ascending);
      }
    }

  }

}
