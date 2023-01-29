package com.trodix.documentstorage.persistance.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.PropertyType;

public class ModelRowMapper implements RowMapper<Model> {

    @Override
    public Model mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final Model model = new Model();

        model.setId(rs.getLong("m_id"));
        model.setType(PropertyType.values()[rs.getInt("m_type")]);
        model.setQname(new QNameRowMapper().mapRow(rs, rowNum));

        return model;
    }

}
