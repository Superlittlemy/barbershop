package com.slm.barbershop.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 周内休息标记 List<Integer> ↔ VARCHAR(7) 转换
 * <p>
 * 7 位 0/1,索引 0=周一 ... 6=周日。
 * 仅在 ShopStatsVO.weeklyOff 这种"字符串列映射 List 字段"的场景使用,
 * 不要注册为全局 TypeHandler(避免污染其它 List 字段)。
 */
public class WeeklyOffListTypeHandler extends BaseTypeHandler<List<Integer>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, joinString(parameter));
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseString(rs.getString(columnName));
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseString(rs.getString(columnIndex));
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseString(cs.getString(columnIndex));
    }

    private String joinString(List<Integer> list) {
        if (list == null) return "0000000";
        StringBuilder sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            sb.append(i < list.size() && list.get(i) != null && list.get(i) == 1 ? '1' : '0');
        }
        return sb.toString();
    }

    private List<Integer> parseString(String s) {
        List<Integer> list = new ArrayList<>(7);
        if (s == null || s.length() < 7) {
            for (int i = 0; i < 7; i++) list.add(0);
            return list;
        }
        for (int i = 0; i < 7; i++) {
            list.add(s.charAt(i) == '1' ? 1 : 0);
        }
        return list;
    }

}
