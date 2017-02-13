/*-
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
package de.braintags.netrelay.controller.querypool.template;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.braintags.netrelay.controller.querypool.template.dynamic.QueryPart;
import de.braintags.vertx.jomnigate.dataaccess.query.IQuery;

/**
 * The dynamic query part of the {@link QueryTemplate}. The dynamic query will be translated to a {@link IQuery}, which
 * should always work no matter the current datastore.<br>
 * <br>
 * Copyright: Copyright (c) 19.12.2016 <br>
 * Company: Braintags GmbH <br>
 *
 * @author sschmitt
 */
public class DynamicQuery {

  @JsonProperty(value = "query")
  private QueryPart rootQueryPart;

  /**
   * @return the first {@link QueryPart} of the query, which might either contain a condition or more query parts
   */
  public QueryPart getRootQueryPart() {
    return rootQueryPart;
  }
}
