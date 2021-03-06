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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.vertx.util.ExceptionUtil;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;

/**
 * An implementation of {@link IVolume} based on vertx filesystem
 * 
 * @author Michael Remme
 * 
 */
public class VertxVolume implements IVolume<JsonObject> {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(VertxVolume.class);

  private FileSystem fs;
  private VertxTarget rootDir;
  private String volumeId;
  private String alias;
  private ITargetSerializer<JsonObject> serializer;
  private List<Pattern> ignorePatterns = new ArrayList<>();

  public VertxVolume(FileSystem fs, String rootDir, String volumeId, String alias,
      ITargetSerializer<JsonObject> serializer) {
    this(fs, FileSystems.getDefault().getPath(rootDir), volumeId, alias, serializer, null);
  }

  /**
   * 
   */
  public VertxVolume(FileSystem fs, Path rootDir, String volumeId, String alias,
      ITargetSerializer<JsonObject> serializer, List<String> ignores) {
    this.fs = fs;
    this.rootDir = new VertxTarget(this, rootDir, true);
    this.volumeId = volumeId;
    this.alias = alias == null ? volumeId : alias;
    this.serializer = serializer;
    if (ignores != null) {
      ignores.forEach(pat -> ignorePatterns.add(Pattern.compile(pat)));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.Volume#fromPath(java.lang.String)
   */
  @Override
  public ITarget fromPath(Path path) {
    if (path == null) {
      throw new NullPointerException("Path must not be null");
    }
    if (path.toAbsolutePath().equals(rootDir.getPath().toAbsolutePath())) {
      return rootDir;
    }
    return new VertxTarget(this, path);
  }

  @Override
  public ITarget fromPath(String path) {
    return fromPath(getPath(path));
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
    Path path = getRoot().getPath();
    try {
      List<Path> sr = search(path, target, FileTreeSearch.MatchMode.ANYWHERE, false);
      List<ITarget> result = new ArrayList<>();
      for (Path p : sr) {
        ITarget t = fromPath(p);
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
    LOGGER.info("searching in path " + path.toString());
    FileTreeSearch fileTreeSearch = new FileTreeSearch(target, mode, ignoreCase);
    Files.walkFileTree(path, fileTreeSearch);

    List<Path> paths = fileTreeSearch.getFoundPaths();
    return Collections.unmodifiableList(paths);
  }

  private Path getPath(String relativePath) {
    String rdir = getRoot().getAbsolutePath();
    Path path;
    if (relativePath.startsWith(rdir)) {
      path = Paths.get(relativePath);
    } else {
      path = Paths.get(rdir, relativePath);
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
  public ITarget getParent(Path path) {
    return fromPath(path.getParent());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume#getTargetSerializer()
   */
  @Override
  public ITargetSerializer<JsonObject> getTargetSerializer() {
    return serializer;
  }

  /**
   * @return the ignores
   */
  public List<Pattern> getIgnores() {
    return ignorePatterns;
  }

}
