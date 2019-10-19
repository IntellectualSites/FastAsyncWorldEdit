package com.boydti.fawe.object.visitor;

import org.jetbrains.annotations.Range;

public interface FaweChunkVisitor {

  /**
   * This will run for each set block in the chunk.
   *
   * @param localX The x position in the chunk (0-15)
   * @param y The y position (0 - 255)
   * @param localZ The z position in the chunk (0-15)
   * @param combined The combined id
   */
  void run(int localX, @Range(from = 0, to = 255) int y, int localZ, int combined);
}
