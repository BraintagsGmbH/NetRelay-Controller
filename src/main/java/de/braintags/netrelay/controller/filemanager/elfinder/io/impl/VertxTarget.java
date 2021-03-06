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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tika.Tika;

import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITargetSerializer;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import de.braintags.vertx.util.file.BufferInputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileProps;
import io.vertx.core.json.JsonObject;

/**
 * An implementation of ITarget for vertx
 * 
 * @author Michael Remme
 * 
 */
public class VertxTarget implements ITarget<JsonObject> {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(VertxTarget.class);
  private final Tika tika = new Tika();

  private VertxVolume volume;
  private Path path;
  private boolean isRoot = false;
  private FileProps fileProps;
  private String mimeType = null;
  private String relativePath;

  /**
   * Create a new instance
   * 
   * @param volume
   *          the {@link VertxVolume} where the new instance is contained
   * @param path
   *          the relative path inside the volume
   */
  public VertxTarget(VertxVolume volume, Path path) {
    this(volume, path, false);
  }

  /**
   * Create a new instance
   * 
   * @param volume
   *          the {@link VertxVolume} where the new instance is contained
   * @param path
   *          the relative path inside the volume
   * @param isRoot
   *          defines wether the target is the root target of the volume
   * 
   */
  public VertxTarget(VertxVolume volume, Path path, boolean isRoot) {
    this.volume = volume;
    if (!isRoot) {
      String volumeRootPath = volume.getRoot().getAbsolutePath();
      String pathPath = path.toAbsolutePath().toString();
      if (!pathPath.startsWith(volumeRootPath)) {
        throw new IllegalArgumentException(
            "path '" + path + "' is not a subpath of volume root " + volume.getRoot().getPath());
      }
      this.relativePath = pathPath.substring(volumeRootPath.length() + 1);
    }
    this.isRoot = isRoot;
    if (path == null) {
      throw new IllegalArgumentException("Path must not be null");
    }
    this.path = path;
  }

  @Override
  public String getRelativePath() {
    return relativePath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getVolume()
   */
  @Override
  public IVolume getVolume() {
    return volume;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getPath()
   */
  @Override
  public Path getPath() {
    return path;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#isFolder()
   */
  @Override
  public boolean isFolder() {
    loadDetails();
    return fileProps.isDirectory();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getHash()
   */
  @Override
  public String getHash() {
    return ElFinderContext.getHash(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getLastModified()
   */
  @Override
  public long getLastModified() {
    return fileProps.lastModifiedTime();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getMimeType()
   */
  @Override
  public String getMimeType() {
    if (mimeType == null) {
      if (isFolder()) {
        mimeType = "directory";
      } else {
        mimeType = tika.detect(getAbsolutePath());
      }
    }
    return mimeType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getName()
   */
  @Override
  public String getName() {
    return path.getFileName().toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getParent()
   */
  @Override
  public ITarget getParent() {
    return getVolume().getParent(path);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getSize()
   */
  @Override
  public long getSize() {
    return fileProps.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#hasChildFolder()
   */
  @Override
  public boolean hasChildFolder() {
    if (isFolder()) {
      List<ITarget> children = listChildren();
      return children.stream().anyMatch(target -> target.isFolder());
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#hasChildren()
   */
  @Override
  public boolean hasChildren() {
    List<ITarget> children = listChildren();
    return !children.isEmpty();
  }

  @Override
  public List<ITarget> listChildren() {
    LOGGER.debug("Listing children for : " + toString());
    loadDetails();
    if (!isFolder()) {
      throw new IllegalArgumentException("not a directory: " + getPath());
    }
    List<String> childList = volume.getFileSystem().readDirBlocking(getAbsolutePath());
    List<ITarget> targetList = new ArrayList<>();
    for (String child : childList) {
      Path tmpPath = path.resolve(child);
      if (!toSuppress(tmpPath)) {
        targetList.add(getVolume().fromPath(tmpPath));
      }
    }
    targetList.forEach(target -> ((VertxTarget) target).loadDetails());
    return targetList;
  }

  /**
   * Files and directories are displayed, if we are not inside aroot directory or if we are in root and the file is not
   * defined to be excluded
   * 
   * @param fileName
   * @return
   */
  private boolean toSuppress(Path path) {
    if (volume.getIgnores() == null || volume.getIgnores().isEmpty()) {
      return Boolean.FALSE;
    } else {
      for (Pattern p : volume.getIgnores()) {
        if (p.matcher(path.toString()).matches()) {
          return Boolean.TRUE;
        }
      }
    }
    return Boolean.FALSE;
  }

  @Override
  public void createFile() {
    volume.getFileSystem().createFileBlocking(getAbsolutePath());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#createChildTarget(java.lang.String)
   */
  @Override
  public ITarget createChildTarget(String childName) {
    loadDetails();
    if (!isFolder()) {
      throw new IllegalArgumentException("not a directory: " + getPath());
    }
    return volume.fromPath(path.resolve(childName));
  }

  @Override
  public void createFolder() {
    volume.getFileSystem().mkdirBlocking(getAbsolutePath());
  }

  private void loadDetails() {
    if (fileProps == null) {
      fileProps = volume.getFileSystem().propsBlocking(getAbsolutePath());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#isReadable()
   */
  @Override
  public boolean isReadable() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#isWritable()
   */
  @Override
  public boolean isWritable() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#isLocked()
   */
  @Override
  public boolean isLocked() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#isRoot()
   */
  @Override
  public boolean isRoot() {
    return this.isRoot;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "VertxTarget [absolutePath=" + path + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#delete()
   */
  @Override
  public void delete() {
    volume.getFileSystem().deleteBlocking(getAbsolutePath());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#exists()
   */
  @Override
  public boolean exists() {
    return volume.getFileSystem().existsBlocking(getAbsolutePath());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#rename(java.lang.String)
   */
  @Override
  public void rename(ITarget destination) {
    volume.getFileSystem().moveBlocking(getAbsolutePath(), destination.getAbsolutePath());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#readFile()
   */
  @Override
  public Buffer readFile() {
    if (isFolder()) {
      throw new IllegalArgumentException("is a directory: " + getPath());
    }
    return volume.getFileSystem().readFileBlocking(getAbsolutePath());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#writeFile(io.vertx.core.buffer.Buffer)
   */
  @Override
  public void writeFile(Buffer buffer) {
    if (isFolder()) {
      throw new IllegalArgumentException("is a directory: " + getPath());
    }
    volume.getFileSystem().writeFileBlocking(getAbsolutePath(), buffer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#openInputStream()
   */
  @Override
  public InputStream openInputStream() {
    return new BufferInputStream(readFile());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getAsyncFile()
   */
  @Override
  public AsyncFile getAsyncFile() {
    return volume.getFileSystem().openBlocking(getAbsolutePath(), null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getAbsolutePath()
   */
  @Override
  public String getAbsolutePath() {
    return getPath().toAbsolutePath().toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#isChild(de.braintags.netrelay.controller.
   * filemanager.elfinder.io.ITarget)
   */
  @Override
  public boolean isChild(ITarget parent) {
    return getAbsolutePath().startsWith(parent.getAbsolutePath());
  }

  @Override
  public String getExtension() {
    String name = getName();
    if (name.lastIndexOf('.') > 0) {
      return name.substring(name.lastIndexOf('.') + 1);
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getSerializer()
   */
  @Override
  public ITargetSerializer<JsonObject> getSerializer() {
    return getVolume().getTargetSerializer();
  }

}
