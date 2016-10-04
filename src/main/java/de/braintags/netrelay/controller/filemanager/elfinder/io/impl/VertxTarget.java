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
import java.util.ArrayList;
import java.util.List;

import org.apache.tika.Tika;

import de.braintags.io.vertx.util.file.BufferInputStream;
import de.braintags.netrelay.controller.filemanager.elfinder.ElFinderContext;
import de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget;
import de.braintags.netrelay.controller.filemanager.elfinder.io.IVolume;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;

/**
 * An implementation of ITarget for vertx
 * 
 * @author Michael Remme
 * 
 */
public class VertxTarget implements ITarget {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(VertxTarget.class);
  private final Tika tika = new Tika();

  private VertxVolume volume;
  private String absolutePath;
  private boolean isRoot = false;
  private String name;
  private FileProps fileProps;
  private String mimeType = null;

  /**
   * Create a new instance
   * 
   * @param volume
   *          the {@link VertxVolume} where the new instance is contained
   * @param path
   *          the relative path inside the volume
   */
  public VertxTarget(VertxVolume volume, String path) {
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
  public VertxTarget(VertxVolume volume, String path, boolean isRoot) {
    this.volume = volume;
    if (!isRoot && !path.startsWith(volume.getRoot().getPath())) {
      throw new IllegalArgumentException(
          "path '" + path + "' is not a subpath of volume root " + volume.getRoot().getPath());
    }
    this.isRoot = isRoot;
    if (path == null || path.trim().hashCode() == 0) {
      throw new IllegalArgumentException("Path must not be null");
    }
    this.absolutePath = path;
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    this.name = path.substring(path.lastIndexOf("/") + 1);
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
  public String getPath() {
    return absolutePath;
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
        mimeType = tika.detect(absolutePath);
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
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#getParent()
   */
  @Override
  public ITarget getParent() {
    return getVolume().getParent(absolutePath);
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
    loadDetails();
    if (!isFolder()) {
      throw new IllegalArgumentException("not a directory: " + getPath());
    }
    List<String> childList = volume.getFileSystem().readDirBlocking(absolutePath);
    List<ITarget> targetList = new ArrayList<>();
    childList.forEach(sub -> targetList.add(getVolume().fromPath(sub)));
    targetList.forEach(target -> ((VertxTarget) target).loadDetails());
    return targetList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#createFile(java.lang.String)
   */
  @Override
  public ITarget createFile(String fileName) {
    loadDetails();
    if (!isFolder()) {
      throw new IllegalArgumentException("not a directory: " + getPath());
    }
    String path = absolutePath + "/" + fileName;
    volume.getFileSystem().createFileBlocking(path);
    return volume.fromPath(path);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#createFolder(java.lang.String)
   */
  @Override
  public ITarget createFolder(String folderName) {
    loadDetails();
    if (!isFolder()) {
      throw new IllegalArgumentException("not a directory: " + getPath());
    }
    String path = absolutePath + "/" + folderName;
    volume.getFileSystem().mkdirBlocking(path);
    return volume.fromPath(path);
  }

  private void loadDetails() {
    if (fileProps == null) {
      fileProps = volume.getFileSystem().propsBlocking(absolutePath);
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
    return "VertxTarget [absolutePath=" + absolutePath + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#delete()
   */
  @Override
  public void delete() {
    volume.getFileSystem().deleteBlocking(absolutePath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#exists()
   */
  @Override
  public boolean exists() {
    return volume.getFileSystem().existsBlocking(absolutePath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.filemanager.elfinder.io.ITarget#rename(java.lang.String)
   */
  @Override
  public void rename(String destination) {
    volume.getFileSystem().moveBlocking(absolutePath, destination);
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
    return volume.getFileSystem().readFileBlocking(getPath());
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
    volume.getFileSystem().writeFileBlocking(getPath(), buffer);
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

}
