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
package de.braintags.netrelay.controller.filemanager.elfinder.io;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;

/**
 * interface which is used to serialize an {@link ITarget}
 * 
 * @author Michael Remme
 * @param T
 *          the result of the serialization
 * 
 */
public interface ITargetSerializer<T> {

  /**
   * Serializes the given target into the destination format
   * 
   * @param efContext
   * @param target
   * @return
   */
  T serialize(ElFinderContext efContext, ITarget<T> target);

  /**
   * Serializes the given target options into the destination format
   * 
   * @param efContext
   * @param target
   * @return
   */
  T serializeoptions(ElFinderContext efContext, ITarget<T> target);

}
