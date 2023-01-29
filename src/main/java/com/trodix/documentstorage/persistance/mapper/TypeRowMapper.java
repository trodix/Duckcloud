package com.trodix.documentstorage.persistance.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.trodix.documentstorage.persistance.entity.Type;

public class TypeRowMapper implements RowMapper<Type> {

    @Override
    public Type mapRow(ResultSet rs, int rowNum) throws SQLException {

        Type type = new Type();

        type.setId(rs.getLong("t_id"));
        type.setQname(new QNameRowMapper().mapRow(rs, rowNum));
        
        return type;
    }

}
