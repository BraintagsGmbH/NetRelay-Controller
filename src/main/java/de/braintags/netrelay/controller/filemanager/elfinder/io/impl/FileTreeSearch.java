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
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class FileTreeSearch extends SimpleFileVisitor<Path> {

  enum MatchMode {
    EXACT,
    ANYWHERE
  }

  private final String query;
  private final MatchMode mode;
  private final boolean ignoreCase;
  private final List<Path> foundPaths;

  public FileTreeSearch(String query, MatchMode mode, boolean ignoreCase) {
    this.query = query;
    this.mode = mode;
    this.ignoreCase = ignoreCase;
    this.foundPaths = new ArrayList<>();
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    // if (exc != null) {
    // System.err.println(String.format("Failed to visit the file. %s", String.valueOf(exc)));
    // }
    // if something goes wrong continue...
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    Objects.requireNonNull(dir);
    if (exc == null) {
      search(dir);
    }
    // else {
    // // if something goes wrong continue...
    // System.err.println(String.format("Failed to visit the directory. %s", String.valueOf(exc)));
    // }

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    super.visitFile(file, attrs);
    search(file);
    return FileVisitResult.CONTINUE;
  }

  private void search(Path path) {
    if (path != null && path.getFileName() != null) {
      String fileName = path.getFileName().toString();
      boolean found;
      switch (mode) {
      case EXACT:
        if (ignoreCase) {
          found = fileName.equalsIgnoreCase(query);
        } else {
          found = fileName.equals(query);
        }
        break;
      case ANYWHERE:
        if (ignoreCase) {
          found = fileName.toLowerCase().contains(query.toLowerCase());
        } else {
          found = fileName.contains(query);
        }
        break;
      default:
        // NOP - This Should Never Happen
        throw new AssertionError();
      }
      if (found) {
        foundPaths.add(path);
      }
    }
  }

  public List<Path> getFoundPaths() {
    return Collections.unmodifiableList(foundPaths);
  }

}
