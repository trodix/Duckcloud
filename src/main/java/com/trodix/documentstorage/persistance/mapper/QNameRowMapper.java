package com.trodix.documentstorage.persistance.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.trodix.documentstorage.persistance.entity.QName;

public class QNameRowMapper implements RowMapper<QName> {

    @Override
    public QName mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final QName qname = new QName();

        qname.setId(rs.getLong("q_id"));
        qname.setName(rs.getString("q_name"));
        qname.setNamespace(new NamespaceRowMapper().mapRow(rs, rowNum));
    
        return qname;
    }

}
