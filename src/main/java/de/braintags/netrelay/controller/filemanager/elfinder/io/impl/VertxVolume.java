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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.braintags.io.vertx.util.ExceptionUtil;
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
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(VertxVolume.class);

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
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#search(java.lang.String)
   */
  @Override
  public List<ITarget> search(String target) {
    Path path = getPath(getRoot().getPath());
    try {
      List<Path> sr = search(path, target, FileTreeSearch.MatchMode.ANYWHERE, false);
      List<ITarget> result = new ArrayList<>();
      for (Path p : sr) {
        ITarget t = fromPath(p.toString());
        if (!t.exists()) {
          throw new IllegalArgumentException("wrong path: " + p.toString());
        }
        result.add(t);
      }
      return result;
    } catch (IOException e) {
      throw ExceptionUtil.createRuntimeException(e);
    }
  }

  /**
   * Searches a given path to get the given target.
   *
   * @param path
   *          the path to be search.
   * @param target
   *          the target.
   * @param mode
   *          the match mode constraint (EXACT, ANYWHERE).
   * @param ignoreCase
   *          the flag that indicates if is to make a ignore case search.
   * @return a list of the found paths that contains the target string.
   * @throws IOException
   *           if something goes wrong.
   */
  public static List<Path> search(Path path, String target, FileTreeSearch.MatchMode mode, boolean ignoreCase)
      throws IOException {
    FileTreeSearch fileTreeSearch = new FileTreeSearch(target, mode, ignoreCase);
    Files.walkFileTree(path, fileTreeSearch);

    List<Path> paths = fileTreeSearch.getFoundPaths();
    return Collections.unmodifiableList(paths);
  }

  private Path getPath(String relativePath) {
    String rootDir = getRoot().getPath();
    Path path;
    if (relativePath.startsWith(rootDir)) {
      path = Paths.get(relativePath);
    } else {
      path = Paths.get(rootDir, relativePath);
    }
    return path;
  }

  /**
   * Get the underlaying {@link FileSystem} of vertx
   * 
   * @return
   */
  @Override
  public FileSystem getFileSystem() {
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
