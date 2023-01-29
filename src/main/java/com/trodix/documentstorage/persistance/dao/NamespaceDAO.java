package com.trodix.documentstorage.persistance.dao;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.documentstorage.persistance.entity.Namespace;
import com.trodix.documentstorage.persistance.mapper.NamespaceRowMapper;
import com.trodix.documentstorage.persistance.utils.DaoUtils;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class NamespaceDAO {

    private final NamedParameterJdbcTemplate tpl;

    public Namespace save(final Namespace namespace) {

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final MapSqlParameterSource params = new MapSqlParameterSource();
        final String query;

        final Optional<Namespace> existingNamespace = findByName(namespace.getName());

        if (existingNamespace.isEmpty()) {
            query = "INSERT INTO namespace (name) VALUES (:name)";
        } else {
            params.addValue("id", existingNamespace.get().getId());
            query = "UPDATE namespace SET name = :name WHERE name = :old_name";
            params.addValue("old_name", existingNamespace.get().getName());
        }

        params.addValue("name", namespace.getName());

        tpl.update(query, params, keyHolder);

        namespace.setId((Long) keyHolder.getKeys().get("id"));

        return namespace;
    }

    public Optional<Namespace> findByName(final String name) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", name);

        final String query = "SELECT n.id as n_id, n.name as n_name FROM namespace n WHERE n.name = :name";

        final List<Namespace> result = tpl.query(query, params, new NamespaceRowMapper());

        return DaoUtils.findOne(result);
    }

}
