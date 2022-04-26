package org.gbif.geocode.ws.util;

import com.google.common.base.Stopwatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class MyBatisMaximumSpeedTest {
  // About 17,000 per second with a local database.
  static final String DB_URL = "jdbc:postgresql://localhost/eez";
  static final String USER = "eez";
  static final String PASS = "eez";
  static final String QUERY = "SELECT 'Political' AS type, 1 AS id, 'http://www.naturalearthdata.com/' AS source, 'title' AS title, 'AL' AS isoCountryCode2Digit, 0 AS distance;";

  Stopwatch sf = Stopwatch.createUnstarted();

  @Test
  @Disabled
  public void testFastestMyBatisQuery() {
    for (int a = 0; a < 5; a++) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           PreparedStatement ps = conn.prepareStatement(QUERY)) {
        sf.reset();
        sf.start();
        int count = 0;
        for (int i = 0; i < 100_000; i++) {
          count++;

          ResultSet rs = ps.executeQuery();

          while (rs.next()) {
            rs.getString("id");
          }
        }
        sf.stop();

        System.out.println("Test: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
          + ((double) count) / sf.elapsed(TimeUnit.SECONDS) + " per second");
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
