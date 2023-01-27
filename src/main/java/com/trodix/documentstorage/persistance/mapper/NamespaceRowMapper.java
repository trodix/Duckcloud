package com.trodix.documentstorage.persistance.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.trodix.documentstorage.persistance.entity.Namespace;

public class NamespaceRowMapper implements RowMapper<Namespace> {

    @Override
    public Namespace mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final Namespace entity = new Namespace();

        while (rs.next()) {
            entity.setId(rs.getLong("n_id"));
            entity.setName(rs.getString("n_name"));
        }

        return entity;
    }

}
