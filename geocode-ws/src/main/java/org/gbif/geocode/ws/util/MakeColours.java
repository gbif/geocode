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
    Set<String> colours = (minimum < 600) ? makeNiceColours(minimum) : makeBoringColours(minimum);

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

  public static Set<String> makeNiceColours(int minimum) {
    double[] saturations = new double[]{2/6d, 3/6d, 4/6d, 5/6d, 1.0d};
    double[] values = new double[]{2/6d, 3/6d, 4/6d, 5/6d, 1.0d};
    // Number of sectors in which to divide the colour whee
    int hues = (int)Math.ceil(1.1 * minimum / 25d);

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

  public static Set<String> makeBoringColours(int minimum) {
    int step =  0xFFFFFF / minimum - 1;

    Set<String> colours = new HashSet<>();

    for (int c = 0x000030; c < 0xFFFFFF; c += step) {
      int r = (c & 0xFF0000) >> 16;
      int g = (c & 0x00FF00) >> 8;
      int b = (c & 0x0000FF);

      if ((r != g) || (r != b)) {
        String hex = String.format("#%02x%02x%02x", r, g, b);
        colours.add(hex);
      }
    }

    return colours;
  }
}
