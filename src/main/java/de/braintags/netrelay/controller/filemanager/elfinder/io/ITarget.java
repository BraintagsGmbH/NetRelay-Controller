/*
 * #%L
 * %%
 * Copyright (C) 2015 Trustsystems Desenvolvimento de Sistemas, LTDA.
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Trustsystems Desenvolvimento de Sistemas, LTDA. nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.braintags.netrelay.controller.filemanager.elfinder.io;

import java.io.InputStream;
import java.util.List;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;

public interface ITarget {

  /**
   * The IVolume, where this target resides in
   * 
   * @return
   */
  IVolume getVolume();

  /**
   * The path inside the volume
   * 
   * @return
   */
  String getPath();

  /**
   * Create a new file in the filesystem. Use createChildTarget to gain the target
   */
  void createFile();

  /**
   * Create a new file in the filesystem. Use createChildTarget to gain the target
   */
  void createFolder();

  /**
   * true, if this is a folder
   * 
   * @return
   */
  boolean isFolder();

  /**
   * a hash, specifying the current instance
   * 
   * @return
   */
  String getHash();

  /**
   * the last modification timestamp
   * 
   * @return
   */
  long getLastModified();

  /**
   * Get the mime type of the underlaying file
   * 
   * @return
   */
  String getMimeType();

  /**
   * Get the name of the underlaying file or directory
   * 
   * @return
   */
  String getName();

  /**
   * Gt the parent instance
   * 
   * @return
   */
  ITarget getParent();

  /**
   * Get the size of the underlaying file or directory
   * 
   * @return
   */
  long getSize();

  /**
   * true, if it has sub folders
   * 
   * @return
   */
  boolean hasChildFolder();

  /**
   * true, if it has children
   * 
   * @return
   */
  boolean hasChildren();

  /**
   * Get the list of children
   * 
   * @return
   */
  List<ITarget> listChildren();

  /**
   * @return
   */
  boolean isReadable();

  /**
   * @return
   */
  boolean isWritable();

  /**
   * @return
   */
  boolean isLocked();

  /**
   * @return
   */
  boolean isRoot();

  /**
   * Delete the existing file or directory. If directory is not empty, an exception is thrown
   */
  void delete();

  /**
   * Checks wether file or directory exist
   * 
   * @return
   */
  boolean exists();

  /**
   * Creates a new target as child
   * 
   * @return
   */
  ITarget createChildTarget(String childName);

  /**
   * Rename the target into the new one
   * 
   * @param destination
   */
  void rename(ITarget destination);

  /**
   * Get the content of the file as {@link Buffer}
   * 
   * @return
   */
  Buffer readFile();

  /**
   * Write the content of the file as {@link Buffer}
   * 
   * @param buffer
   */
  void writeFile(Buffer buffer);

  /**
   * Get an inputStream for the file
   * 
   * @return
   */
  InputStream openInputStream();

  /**
   * Get an instance of {@link AsyncFile} from the target
   * 
   * @return
   */
  AsyncFile getAsyncFile();

}