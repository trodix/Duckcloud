package com.trodix.documentstorage.persistance.dao;

import java.sql.Types;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.mapper.ModelRowMapper;
import com.trodix.documentstorage.persistance.repository.ModelRepository;
import com.trodix.documentstorage.persistance.utils.DaoUtils;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class ModelDAO {

    private final NamedParameterJdbcTemplate tpl;

    private final ModelRepository modelRepository;

    private final QNameDAO qnameDAO;

    public Model save(final Model model) {

        if (model.getQname().getId() == null) {
            final Optional<QName> existionQName = qnameDAO.findByName(model.getQname().getName());
            if (existionQName.isPresent()) {
                model.setQname(existionQName.get());
            } else {
                qnameDAO.save(model.getQname());
            }
        }

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final String query;
        final Optional<Model> existingModel = findByQname(model.getQname());

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("qname_id", model.getQname().getId());

        if (model.getType() != null) {
            params.addValue("type", model.getType().ordinal(), Types.INTEGER);
            if (existingModel.isEmpty()) {
                query = "INSERT INTO model (qname_id, type) VALUES (:qname_id, :type)";
            } else {
                query = "UPDATE model SET type = :type WHERE qname_id = :qname_id";
            }
        } else {
            if (existingModel.isEmpty()) {
                query = "INSERT INTO model (qname_id) VALUES (:qname_id)";
            } else {
                throw new IllegalArgumentException("Nothing to update for model " + model);
            }
        }

        tpl.update(query, params, keyHolder);

        model.setId((Long) keyHolder.getKeys().get("id"));

        return model;
    }

    public Optional<Model> findByQname(final QName qname) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("name", qname.getName());

        final String query = """
                SELECT m.id as m_id, m.type as m_type, q.id as q_id, q.name as q_name, n.id as n_id, n.name as n_name
                FROM qname q
                INNER JOIN model m ON m.qname_id = q.id
                INNER JOIN namespace n ON q.namespace_id = n.id
                WHERE q.name = :name
                """;

        return DaoUtils.findOne(tpl.query(query, params, new ModelRowMapper()));
    }

    public long count() {
        return modelRepository.count();
    }

}
