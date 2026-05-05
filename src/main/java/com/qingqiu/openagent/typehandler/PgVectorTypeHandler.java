package com.qingqiu.openagent.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

/**
 * 于处理 PostgreSQL 的 pgvector 向量类型与 Java 的 float[] 数组之间的转换
 * 当 MyBatis 执行 SQL 时，自动转换数据类型，避免手动处理，比如：
 * 写入数据库（setNonNullParameter）：将 float[] 转换为 pgvector 字符串格式（如 "[1.0,2.5,3.0]"），然后插入 SQL。
 * 读取数据库（getNullableResult）：将 pgvector 字符串解析为 float[] 数组。
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(float[].class)
public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int j = 0; j < parameter.length; j++) {
            sb.append(parameter[j]);
            if (j < parameter.length - 1) sb.append(',');
        }
        sb.append(']');
        ps.setObject(i, sb.toString(), Types.OTHER);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private float[] parse(String vectorText) {
        if (vectorText == null) return null;
        // 去掉 "[ ]"
        vectorText = vectorText.replace("[", "").replace("]", "");
        if (vectorText.isBlank()) return new float[0];
        String[] parts = vectorText.split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i]);
        }
        return arr;
    }
}
