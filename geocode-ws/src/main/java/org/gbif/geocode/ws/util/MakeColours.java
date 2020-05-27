package org.gbif.geocode.ws.util;

import java.util.Collections;
import java.util.Stack;

/**
 * Generate evenly distributed colours, to make human interpretation of the lookup map image nicer.
 */
public class MakeColours {

  public static Stack<String> makeColours(int minimum) {
    int hues = (int)Math.ceil(minimum / 25d); // Number of sectors in which to divide the colour wheel
    double[] saturations = {2/6d, 3/6d, 4/6d, 5/6d, 1.0d};
    double[] values = {2/6d, 3/6d, 4/6d, 5/6d, 1.0d};

    Stack<String> colours = new Stack<>();

    for (int hueSegment = 0; hueSegment < hues; hueSegment++) {
      double h = (hueSegment / ((double)hues)) * 360;

      for (double s : saturations) {
        for (double v : values) {
          String hex = HSVtoRGB(h, s, v);
          colours.add(hex);
        }
      }
    }

    Collections.shuffle(colours);
    return colours;
  }

  /**
   * Convert an HSV colour to RGB in hexadecimal format.
   */
  private static String HSVtoRGB(double h, double s, double v) {
    int i;
    double f, p, q, t;

    if (s == 0) {
      // grey
      return toRgbHex(v, v, v);
    }

    h /= 60;	// sector 0 to 5
    i = (int) Math.floor(h);
    f = h - i;	// factorial part of h
    p = v * (1 - s);
    q = v * (1 - s * f);
    t = v * (1 - s * (1 - f));

    switch(i) {
      case 0:
        return toRgbHex(v, t, p);
      case 1:
        return toRgbHex(q, v, p);
      case 2:
        return toRgbHex(p, v, t);
      case 3:
        return toRgbHex(p, q, v);
      case 4:
        return toRgbHex(t, p, v);
      default: // case 5:
        return toRgbHex(v, p, q);
    }
  }

  private static String toRgbHex(double r, double g, double b) {
    int rr = (int) (255 * r);
    int gg = (int) (255 * g);
    int bb = (int) (255 * b);

    return String.format("#%02x%02x%02x", rr, gg, bb);
  }
}
