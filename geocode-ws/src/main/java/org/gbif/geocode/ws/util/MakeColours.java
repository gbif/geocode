package org.gbif.geocode.ws.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/**
 * Generate evenly distributed colours, to make human interpretation of the lookup map image nicer.
 */
public class MakeColours {

  public static Stack<String> makeColours(int minimum) {
    System.out.println(minimum + " minimum colours");
    final double[] saturations, values;
    final int hues; // Number of sectors in which to divide the colour wheel

    if (minimum < 600) {
      saturations = new double[]{2/6d, 3/6d, 4/6d, 5/6d, 1.0d};
      values = new double[]{2/6d, 3/6d, 4/6d, 5/6d, 1.0d};
      hues = (int)Math.ceil(1.1 * minimum / 25d);

    } else {
      saturations = new double[99];
      values = new double[99];
      // Zero saturation is greys, and zero value is black. These are reserved for other uses,
      // so start at 1.
      for (int d = 0; d < 99; d++) {
        saturations[d] = (d+1)/100d;
        values[d] = (d+1)/100d;
      }
      hues = Math.max(6, (int)Math.ceil(1.25 * minimum / (99d*99d)));
    }

    Set<String> colours = new HashSet<>();

    for (int hueSegment = 0; hueSegment < hues; hueSegment++) {
      double h = (hueSegment / ((double)hues)) * 360;

      for (double s : saturations) {
        for (double v : values) {
          String hex = HSVtoRGB(h, s, v);
          if (hex != null) {
            colours.add(hex);
          }
        }
      }
    }

    if (colours.size() < minimum) {
      throw new RuntimeException("Not enough colours were generated, needed "+minimum+" but "+
        colours.size());
    }

    List<String> colourList = new ArrayList<>();
    colourList.addAll(colours);
    Collections.shuffle(colourList, new Random(2345)); // Avoid changes if this is rerun.
    Stack<String> shuffledColours = new Stack<>();
    shuffledColours.addAll(colourList);
    return shuffledColours;
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
      case 5:
      default:
        return toRgbHex(v, p, q);
    }
  }

  private static String toRgbHex(double r, double g, double b) {
    int rr = (int) (255 * r);
    int gg = (int) (255 * g);
    int bb = (int) (255 * b);

    if (rr == gg && gg == bb) {
      // Greys are reserved.
      return null;
    }

    return String.format("#%02x%02x%02x", rr, gg, bb);
  }
}
