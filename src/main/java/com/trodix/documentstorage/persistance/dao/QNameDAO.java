package com.trodix.documentstorage.persistance.dao;

import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.documentstorage.persistance.entity.Namespace;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.mapper.QNameRowMapper;
import com.trodix.documentstorage.persistance.repository.QNameRepository;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class QNameDAO {

    private final NamedParameterJdbcTemplate tpl;

    private final NamespaceDAO namespaceDAO;

    private final QNameRepository qnameRepository;

    public QName save(final QName qname) {

        if (qname.getNamespace().getId() == null) {

            final Optional<Namespace> existingNameSpace = namespaceDAO.findByName(qname.getNamespace().getName());
            if (existingNameSpace.isPresent()) {
                qname.setNamespace(existingNameSpace.get());
            } else {
                final Namespace savedNamespace = namespaceDAO.save(qname.getNamespace());
                qname.setNamespace(savedNamespace);
            }
        }

        final KeyHolder keyHolder = new GeneratedKeyHolder();

        final String query;
        final MapSqlParameterSource params = new MapSqlParameterSource();

        if (qname.getId() == null) {
            final Optional<QName> existingQName = qnameRepository.findOneByNamespaceIdAndName(qname.getNamespace().getId(), qname.getName());
            if (existingQName.isEmpty()) {
                query = "INSERT INTO qname (name, namespace_id) VALUES (:name, :namespace_id)";
            } else {
                params.addValue("id", existingQName.get().getId());
                query = "UPDATE qname SET name = :name, namespace_id = :namespace_id WHERE id = :id";
            }
        } else {
            params.addValue("id", qname.getId());
            query = "UPDATE qname SET name = :name, namespace_id = :namespace_id WHERE id = :id";
        }

        params.addValue("name", qname.getName());
        params.addValue("namespace_id", qname.getNamespace().getId());

        tpl.update(query, params, keyHolder);

        qname.setId((Long) keyHolder.getKeys().get("id"));

        return qname;
    }

    public Optional<QName> findByName(final String name) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", name);

        final String query = "SELECT q.id as q_id, q.name as q_name FROM qname WHERE q.name = ':name'";

        return Optional.ofNullable(tpl.queryForObject(query, params, new QNameRowMapper()));
    }

}
