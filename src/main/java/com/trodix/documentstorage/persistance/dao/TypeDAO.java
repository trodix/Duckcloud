package com.trodix.documentstorage.persistance.dao;

import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.entity.Type;
import com.trodix.documentstorage.persistance.mapper.TypeRowMapper;
import com.trodix.documentstorage.persistance.utils.DaoUtils;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class TypeDAO {

    private final NamedParameterJdbcTemplate tpl;

    public Type save(final Type type) {

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final MapSqlParameterSource params = new MapSqlParameterSource();
        final String query;

        if (type.getId() == null) {
            final Optional<Type> existingType = findOneByQname(type.getQname());
            if (existingType.isPresent()) {
                query = "UPDATE type SET qname_id = :qname_id WHERE id = :id";
                params.addValue("id", existingType.get().getId());
            } else {
                query = "INSERT INTO type (qname_id) VALUES (:qname_id)";
            }
        } else {
            query = "UPDATE type SET qname_id = :qname_id WHERE id = :id";
            params.addValue("id", type.getId());
        }

        params.addValue("qname_id", type.getQname().getId());

        tpl.update(query, params, keyHolder);

        type.setId((Long) keyHolder.getKeys().get("id"));

        return type;
    }

    public Optional<Type> findOneByQname(final QName qname) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("qname_name", qname.getName());
        params.addValue("namespace_name", qname.getNamespace().getName());

        final String query = """
                    SELECT t.id as t_id, q.id as q_id, q.name as q_name, n.id as n_id, n.name as n_name
                    FROM type t
                    INNER JOIN qname q ON q.id = t.qname_id
                    INNER JOIN namespace n ON n.id = q.namespace_id
                    INNER JOIN model m ON m.qname_id = q.id
                    WHERE q.name = :qname_name
                    AND n.name = :namespace_name""";

        return DaoUtils.findOne(tpl.query(query, params, new TypeRowMapper()));
    }

}
