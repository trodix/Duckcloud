package com.trodix.documentstorage.persistance.dao;

import java.sql.SQLType;
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
import com.trodix.documentstorage.persistance.repository.ModelRepository;
import com.trodix.documentstorage.persistance.repository.QNameRepository;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class ModelDAO {

    private final NamedParameterJdbcTemplate tpl;

    private final ModelRepository modelRepository;

    private final QNameRepository qnameRepository;

    public Model save(final Model model) {

        if (model.getQname().getId() == null) {
            QName existionQName = qnameRepository.findOneByName(model.getQname().getName()).orElseThrow();
            model.setQname(existionQName);
        }

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final String query;
        Optional<Model> existingModel = findByQname(model.getQname());

        if (existingModel.isEmpty()) {
            query = "INSERT INTO model (qname_id, type) VALUES (:qname_id, :type)";
        } else {
            query = "UPDATE model SET type = :type WHERE qname_id = :qname_id";
        }

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("qname_id", model.getQname().getId());
        params.addValue("type", model.getType().ordinal(), Types.INTEGER);

        tpl.update(query, params, keyHolder);

        model.setId((Long) keyHolder.getKeys().get("id"));

        return model;
    }

    public Optional<Model> findByQname(final QName qname) {
        return modelRepository.findByQname(qname);
    }

}
