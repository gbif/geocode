package org.gbif.geocode.ws.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Stack;

/**
 * Generate evenly distributed colours, to make human interpretation of the lookup map image nicer.
 * <br/>
 * Reads a list of identifiers from stdin, one per line, and outputs (to stdout) a very basic HTML file with the generated colours
 * and CSS declarations for them.
 */
public class MakeColours {

  public static void main(String[] args) throws Exception {

    int hues = 36; // Number of sectors to divide the colour wheel
    double[] saturations = {2/6d, 3/6d, 4/6d, 5/6d, 1.0d};
    double[] values = {2/6d, 3/6d, 4/6d, 5/6d, 1.0d};

    Stack<String> colours = new Stack<>();

    System.out.println("<pre>");
    for (int hueSegment = 0; hueSegment < hues; hueSegment++) {
      double h = (hueSegment / ((double)hues)) * 360;

      for (double s : saturations) {
        for (double v : values) {
          String hex = HSVtoRGB(h, s, v);
          System.out.print(String.format("<span style='background: %s'>%s (%.2f&deg;, %.2f, %.2f)</span>  ", hex, hex, h, s, v));
          colours.add(hex);
        }
        System.out.println();
      }
      System.out.println();
    }

    System.out.println("<p>" + colours.size() + " colours.</p>");

    Collections.shuffle(colours);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    String line;
    while ((line = br.readLine()) != null) {
      System.out.println(String.format("#%s { fill: %s; }", line, colours.pop()));
    }

    System.out.println("</pre>");
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
