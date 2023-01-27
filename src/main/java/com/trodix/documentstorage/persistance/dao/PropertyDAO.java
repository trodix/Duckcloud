package com.trodix.documentstorage.persistance.dao;

import java.sql.Types;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.Property;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.repository.ModelRepository;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class PropertyDAO {

    private final NamedParameterJdbcTemplate tpl;

    private final QNameDAO qnameDAO;

    private final ModelRepository modelRepository;

    public Property save(final Property property) {

        // persist QName relation
        if (property.getQname().getId() == null) {
            final QName savedQName = qnameDAO.save(property.getQname());
            property.setQname(savedQName);
        }

        // get model
        if (property.getModel() == null || property.getModel().getId() == null) {
            final Optional<Model> persistedModel = modelRepository.findByQname(property.getQname());
            if (persistedModel.isPresent()) {
                property.setModel(persistedModel.get());
            } else {
                throw new DataIntegrityViolationException("Model not registered for QName: " + property.getQname());
            }
        }

        final KeyHolder keyHolder = new GeneratedKeyHolder();

        final MapSqlParameterSource params = new MapSqlParameterSource();

        final String query;

        if (property.getId() == null) {
            query = """
                    INSERT INTO property (date_value, double_value, long_value, serializable_value, string_value, qname_id, model_id)
                    VALUES (:date_value, :double_value, :long_value, :serializable_value, :string_value, :qname_id, :model_id)""";
        } else {
            params.addValue("id", property.getId());
            query = """
                    UPDATE property SET
                        date_value = :date_value,
                        double_value = :double_value,
                        long_value = :long_value,
                        serializable_value = :serializable_value,
                        string_value = :string_value,
                        qname_id = :qname_id,
                        model_id = :model_id
                    WHERE id = :id""";
        }

        params.addValue("date_value", property.getDateValue(), Types.DATE);
        params.addValue("double_value", property.getDoubleValue());
        params.addValue("long_value", property.getLongValue());
        params.addValue("serializable_value", property.getSerializableValue());
        params.addValue("string_value", property.getStringValue());
        params.addValue("qname_id", property.getQname().getId());
        params.addValue("model_id", property.getModel().getId());

        tpl.update(query, params, keyHolder);

        property.setId((Long) keyHolder.getKeys().get("id"));

        return property;
    }

}
