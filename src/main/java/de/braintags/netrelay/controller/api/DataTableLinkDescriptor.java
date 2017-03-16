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
package de.braintags.netrelay.controller.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import de.braintags.vertx.jomnigate.IDataStore;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;
import de.braintags.vertx.jomnigate.dataaccess.query.ISearchCondition;
import de.braintags.vertx.jomnigate.mapping.IProperty;
import de.braintags.vertx.jomnigate.mapping.IMapperFactory;
import de.braintags.vertx.jomnigate.typehandler.ITypeHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Contains all relevant parameters from a request which was sent to {@link DataTablesController}.
 * From those data the reply is processed
 *
 * @author Michael Remme
 *
 */
public class DataTableLinkDescriptor {
  private static final String ICOLUMNS = "iColumns";
  private static final String SCOLUMNS = "sColumns";
  private static final String DISPLAY_START = "iDisplayStart";
  private static final String DISPLAY_LENGTH = "iDisplayLength";

  private Class<?> mapperClass;
  private ColDef[] columns;
  private int displayStart;
  private int displayLength;

  /**
   *
   */
  public DataTableLinkDescriptor(Class<?> mapperClass, RoutingContext context) {
    Objects.requireNonNull(mapperClass, "Mapper cass must not be null");
    this.mapperClass = mapperClass;
    extractColumns(context);
    extractStartLength(context);

  }

  /**
   * Generate an instance of IQuery from the information in here
   *
   * @param dataStore
   * @return
   */
  public IQuery<?> toRecordsInTableQuery(IDataStore dataStore) {
    return dataStore.createQuery(mapperClass);
  }

  /**
   * Generate an instance of IQuery from the information in here
   *
   * @param dataStore
   *          the datastore to be used
   * @param mf
   *          the {@link IMapperFactory} which converts seach values
   * @return
   */
  public void toQuery(IDataStore dataStore, IMapperFactory mf, Handler<AsyncResult<IQuery<?>>> handler) {
    IQuery<?> query = dataStore.createQuery(mapperClass);
    query.setReturnCompleteCount(true);
    List<ColDef> defs = clearColDefs();
    if (defs.isEmpty()) {
      querySuccess(query, handler);
    } else {
      loopColumns(query, defs, dataStore, mf, handler);
    }
  }

  private List<ColDef> clearColDefs() {
    ArrayList<ColDef> ret = new ArrayList<>();
    for (ColDef def : columns) {
      if (def != null && StringUtils.isNotBlank(def.name)) {
        ret.add(def);
      }
    }
    return ret;
  }

  @SuppressWarnings("rawtypes")
  private void loopColumns(IQuery<?> query, List<ColDef> defs, IDataStore dataStore, IMapperFactory mf,
      Handler<AsyncResult<IQuery<?>>> handler) {
    List<Future> fl = new ArrayList<>(defs.size());
    for (ColDef def : defs) {
      fl.add(handleColumn(query, mf, def));
    }
    CompositeFuture cf = CompositeFuture.all(fl);
    cf.setHandler(cfr -> {
      if (cfr.failed()) {
        handler.handle(Future.failedFuture(cfr.cause()));
      } else {
        querySuccess(query, handler);
      }
    });
  }

  @SuppressWarnings({ "rawtypes" })
  private Future handleColumn(IQuery<?> query, IMapperFactory mf, ColDef def) {
    Future f = Future.future();
    IProperty field = mf.getMapper(mapperClass).getField(def.name);
    ITypeHandler th = field.getTypeHandler();
    th.fromStore(def.searchValue, field, null, thResult -> {
      if (thResult.failed()) {
        f.fail(thResult.cause());
      } else {
        Object value = thResult.result().getResult();
        if (value != null && value.hashCode() != 0) {
          if (allowContains(value)) {
            query.setSearchCondition(ISearchCondition.contains(def.name, value));
          } else {
            query.setSearchCondition(ISearchCondition.isEqual(def.name, value));
          }
        }
        if (def.sortable) {
          query.addSort(def.name, def.asc);
        }
        f.complete();
      }
    });
    return f;
  }

  private boolean allowContains(Object value) {
    return !value.getClass().isEnum();
  }

  private void querySuccess(IQuery<?> q, Handler<AsyncResult<IQuery<?>>> handler) {
    handler.handle(Future.succeededFuture(q));
  }

  // "iDisplayStart=0&iDisplayLength=10&"
  private void extractStartLength(RoutingContext context) {
    displayStart = Integer.parseInt(context.request().getParam(DISPLAY_START));
    displayLength = Integer.parseInt(context.request().getParam(DISPLAY_LENGTH));
  }

  // &iColumns=7&sColumns=id%2Cusername%2Cfirstname%2Clastname%2Cemail%2Cid%2C&"
  private void extractColumns(RoutingContext context) {
    String iCols = context.request().getParam(ICOLUMNS);
    String sCols = context.request().getParam(SCOLUMNS);
    int colCount = Integer.parseInt(iCols);
    String[] cols = sCols.split(",");

    columns = new ColDef[colCount];
    for (int i = 0; i < cols.length; i++) {
      columns[i] = new ColDef(context, cols[i], i);
    }
  }

  /**
   * @return the columns
   */
  public final ColDef[] getColumns() {
    return columns;
  }

  /**
   * @return the displayStart
   */
  public final int getDisplayStart() {
    return displayStart;
  }

  /**
   * @return the displayLength
   */
  public final int getDisplayLength() {
    return displayLength;
  }

  class ColDef {
    String name;
    String searchValue;
    boolean sortable = false;
    boolean asc = true;

    ColDef(RoutingContext context, String name, int position) {
      this.name = name;
      extract(context, position);
    }

    // mDataProp_0=0&sSearch_0=11&bRegex_0=false&bSearchable_0=true&bSortable_0=true&
    // iSortCol_0: "1" -> es wird nach der zweiten Spalte sortiert, startIndex=0
    // sSortDir_0: "desc" oder "asc"
    void extract(RoutingContext context, int position) {
      searchValue = context.request().getParam("sSearch_" + position);
      Iterator<Map.Entry<String, String>> it = context.request().params().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> entry = it.next();
        if (entry.getKey().startsWith("iSortCol_") && entry.getValue().equals(String.valueOf(position))) {
          this.sortable = true;
          String dirProp = "sSortDir_" + entry.getKey().substring("iSortCol_".length());
          String dir = context.request().getParam(dirProp);
          if (dir != null && "desc".equals(dir)) {
            this.asc = false;
          }
          break;
        }
      }
    }

  }

}
