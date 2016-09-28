/*
 * #%L
 * NetRelay-Controller
 * %%
 * Copyright (C) 2016 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller.filemanager.elfinder.io.impl;

import java.util.List;

import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import io.vertx.core.file.FileSystem;

/**
 * An implementation of {@link IVolume} based on vertx filesystem
 * 
 * @author Michael Remme
 * 
 */
public class VertxVolume implements IVolume {
  private FileSystem fs;
  private VertxTarget rootDir;
  private String volumeId;
  private String alias;

  /**
   * 
   */
  public VertxVolume(FileSystem fs, String rootDir, String volumeId, String alias) {
    this.fs = fs;
    this.rootDir = new VertxTarget(this, rootDir, true);
    this.volumeId = volumeId;
    this.alias = alias == null ? volumeId : alias;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.Volume#fromPath(java.lang.String)
   */
  @Override
  public ITarget fromPath(String path) {
    if (path.equals(rootDir.getPath())) {
      return rootDir;
    }
    return new VertxTarget(this, path);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#getId()
   */
  @Override
  public String getId() {
    return volumeId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#getAlias()
   */
  @Override
  public String getAlias() {
    return alias;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#getRoot()
   */
  @Override
  public ITarget getRoot() {
    return this.rootDir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#deleteFile(de.braintags.netrelay.controller.
   * filemanager.elfinder.io.ITarget)
   */
  @Override
  public void deleteFile(ITarget target) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#deleteFolder(de.braintags.netrelay.controller.
   * filemanager.elfinder.io.ITarget)
   */
  @Override
  public void deleteFolder(ITarget target) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#exists(de.braintags.netrelay.controller.
   * filemanager.elfinder.io.ITarget)
   */
  @Override
  public boolean exists(ITarget target) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#rename(de.braintags.netrelay.controller.
   * filemanager.elfinder.io.ITarget, de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget)
   */
  @Override
  public void rename(ITarget origin, ITarget destination) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#search(java.lang.String)
   */
  @Override
  public List<ITarget> search(String target) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the underlaying {@link FileSystem} of vertx
   * 
   * @return
   */
  FileSystem getFileSystem() {
    return fs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "VertxVolume [rootDir=" + rootDir + ", volumeId=" + volumeId + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#getParent(java.lang.String)
   */
  @Override
  public ITarget getParent(String path) {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (path.equals(rootDir.getPath())) {
      return rootDir;
    }
    int index = path.lastIndexOf("/");
    String parentPath = path.substring(0, index);
    return fromPath(parentPath);
  }

}
