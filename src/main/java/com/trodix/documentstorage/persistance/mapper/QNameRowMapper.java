package com.trodix.documentstorage.persistance.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.trodix.documentstorage.persistance.entity.QName;

public class QNameRowMapper implements RowMapper<QName> {

    @Override
    public QName mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final QName entity = new QName();

        while (rs.next()) {
            entity.setId(rs.getLong("q_id"));
            entity.setName(rs.getString("q_name"));
        }

        return entity;
    }

}
