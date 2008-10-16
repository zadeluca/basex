package org.basex.gui.view.scatter;

import java.awt.Color;

/**
 * A bounding box to support item selection.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Lukas Kircher
 */
public class ScatterBoundingBox {
  /** X coordinate of upper left corner. */
  int x1;
  /** Y coordinate of upper left corner. */
  int y1;
  /** X coordinate of lower right corner. */
  int x2;
  /** Y coordinate of lower right corner. */
  int y2;
  /** Background color of selection box. */
  static Color back = new Color(100, 100, 100, 50);
  /** Color of selection frame. */
  static Color frame = new Color(100, 100, 100, 200);
  
  
  /**
   * Setter for box start point. 
   * @param x x coordinate
   * @param y y coordinate
   */
  void setStart(final int x, final int y) {
    x1 = x;
    y1 = y;
  }
  
  /**
   * Setter for box end point.
   * @param x x coordinate 
   * @param y y coordinate
   */
  void setEnd(final int x, final int y) {
    x2 = x;
    y2 = y;
  }
  
  /**
   * Calculates box width.
   * @return box width
   */
  int getWidth() {
    return x2 - x1;
  }
  
  /**
   * Calculates box height.
   * @return box height
   */
  int getHeight() {
    return y2 - y1;
  }
}
