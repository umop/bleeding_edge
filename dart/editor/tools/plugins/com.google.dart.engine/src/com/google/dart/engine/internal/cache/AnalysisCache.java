/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Instances of the class {@code AnalysisCache} implement an LRU cache of information related to
 * analysis.
 */
public class AnalysisCache {
  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  private final HashMap<Source, SourceEntry> sourceMap = new HashMap<Source, SourceEntry>();

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  private int maxCacheSize;

  /**
   * The policy used to determine which pieces of data to remove from the cache.
   */
  private CacheRetentionPolicy retentionPolicy;

  /**
   * A list containing the most recently accessed sources with the most recently used at the end of
   * the list. When more sources are added than the maximum allowed then the least recently used
   * source will be removed and will have it's cached AST structure flushed.
   */
  private ArrayList<Source> recentlyUsed;

  /**
   * Initialize a newly created cache to maintain at most the given number of AST structures in the
   * cache.
   * 
   * @param maxCacheSize the maximum number of sources for which AST structures should be kept in
   *          the cache
   * @param retentionPolicy the policy used to determine which pieces of data to remove from the
   *          cache
   */
  public AnalysisCache(int maxCacheSize, CacheRetentionPolicy retentionPolicy) {
    this.maxCacheSize = maxCacheSize;
    this.retentionPolicy = retentionPolicy;
    recentlyUsed = new ArrayList<Source>(maxCacheSize);
  }

  /**
   * Record that the given source was just accessed.
   * 
   * @param source the source that was accessed
   */
  public void accessed(Source source) {
    if (recentlyUsed.remove(source)) {
      recentlyUsed.add(source);
      return;
    }
    while (recentlyUsed.size() >= maxCacheSize) {
      if (!flushAstFromCache()) {
        break;
      }
    }
    recentlyUsed.add(source);
  }

  /**
   * Return a collection containing all of the map entries mapping sources to cache entries. Clients
   * should not modify the returned collection.
   * 
   * @return a collection containing all of the map entries mapping sources to cache entries
   */
  public Collection<Entry<Source, SourceEntry>> entrySet() {
    return sourceMap.entrySet();
  }

  /**
   * Return the entry associated with the given source.
   * 
   * @param source the source whose entry is to be returned
   * @return the entry associated with the given source
   */
  public SourceEntry get(Source source) {
    return sourceMap.get(source);
  }

  /**
   * Associate the given entry with the given source.
   * 
   * @param source the source with which the entry is to be associated
   * @param entry the entry to be associated with the source
   */
  public void put(Source source, SourceEntry entry) {
    sourceMap.put(source, entry);
  }

  /**
   * Remove all information related to the given source from this cache.
   * 
   * @param source the source to be removed
   */
  public void remove(Source source) {
    sourceMap.remove(source);
  }

  /**
   * Set the maximum size of the cache to the given size.
   * 
   * @param size the maximum number of sources for which AST structures should be kept in the cache
   */
  public void setMaxCacheSize(int size) {
    maxCacheSize = size;
    while (recentlyUsed.size() > maxCacheSize) {
      if (!flushAstFromCache()) {
        break;
      }
    }
  }

  /**
   * Return the number of sources that are mapped to cache entries.
   * 
   * @return the number of sources that are mapped to cache entries
   */
  public int size() {
    return sourceMap.size();
  }

  /**
   * Attempt to flush one AST structure from the cache.
   * 
   * @return {@code true} if a structure was flushed
   */
  private boolean flushAstFromCache() {
    Source removedSource = removeAstToFlush();
    if (removedSource == null) {
      return false;
    }
    SourceEntry sourceEntry = sourceMap.get(removedSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.FLUSHED);
      sourceMap.put(removedSource, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.flushAstStructures();
      sourceMap.put(removedSource, dartCopy);
    }
    return true;
  }

  /**
   * Remove and return one source from the list of recently used sources whose AST structure can be
   * flushed from the cache. The source that will be returned will be the source that has been
   * unreferenced for the longest period of time but that is not a priority for analysis.
   * 
   * @return the source that was removed
   */
  private Source removeAstToFlush() {
    int sourceToRemove = -1;
    for (int i = 0; i < recentlyUsed.size(); i++) {
      Source source = recentlyUsed.get(i);
      RetentionPriority priority = retentionPolicy.getAstPriority(source, sourceMap.get(source));
      if (priority == RetentionPriority.LOW) {
        return recentlyUsed.remove(i);
      } else if (priority == RetentionPriority.MEDIUM && sourceToRemove < 0) {
        sourceToRemove = i;
      }
    }
    if (sourceToRemove < 0) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Internal error: Could not flush data from the cache",
          new Exception());
      return null;
    }
    return recentlyUsed.remove(sourceToRemove);
  }
}
