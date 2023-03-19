package com.trodix.documentstorage.persistance.mapper;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NodeDbIdRowMapper implements RowMapper<Long>  {
    @Override
    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getLong("n_id");
    }
}
