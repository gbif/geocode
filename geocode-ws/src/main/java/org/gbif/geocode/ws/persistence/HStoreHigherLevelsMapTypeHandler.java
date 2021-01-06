package org.gbif.geocode.ws.persistence;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

/**
 * Collect an Hstore value in a map sorted by the key length, assuming that higher levels has shorter ids.
 */
public class HStoreHigherLevelsMapTypeHandler extends BaseTypeHandler<Map<String, String>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
    throws SQLException {
    ps.setString(i, HStoreConverter.toString(parameter));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private Map<String, String> fromString(String hstring) {
    Map<String, String> higherLevels = new TreeMap<>(
      Comparator.comparingInt(String::length)
        .thenComparing(Function.identity()));

    if (!Strings.isNullOrEmpty(hstring)) {
      Map<String, String> rawMap = HStoreConverter.fromString(hstring);
      for (Map.Entry<String, String> entry : rawMap.entrySet()) {
        try {
          higherLevels.put(entry.getKey(), entry.getValue());
        } catch (IllegalArgumentException e) {
          // ignore this entry
        }
      }
    }
    return higherLevels;
  }





}
