package org.gbif.geocode.ws.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class MakeColoursTest {

  @Test
  public void testColours() {
    // Ensure sufficient, distinct colours are generated.

    int[] minima = new int[]{1, 10, 50, 100, 300, 500, 1_000, 5_000, 10_000, 50_000, 250_000};

    for (int m : minima) {
      Assert.assertTrue(new HashSet<>(MakeColours.makeColours(m)).size() > m);
    }
  }
}
