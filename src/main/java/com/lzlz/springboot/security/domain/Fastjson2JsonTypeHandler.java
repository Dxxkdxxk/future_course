package com.lzlz.springboot.security.domain;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

/**
 * MyBatis JSON类型处理器（Fastjson2）
 */
@MappedTypes(JSONObject.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class Fastjson2JsonTypeHandler extends BaseTypeHandler<JSONObject> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JSONObject parameter, JdbcType jdbcType) throws SQLException {
        // 序列化JSON为字符串
        ps.setString(i, parameter.toJSONString());
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public JSONObject getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    /**
     * 通用JSON解析方法（适配所有Fastjson2版本）
     */
    private JSONObject parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return new JSONObject();
        }
        // 直接解析，不使用任何特殊特性（核心修改点）
        return JSONObject.parseObject(jsonStr);
    }
}
