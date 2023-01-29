package com.trodix.documentstorage.persistance.dao;

import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.trodix.documentstorage.persistance.entity.Aspect;
import com.trodix.documentstorage.persistance.entity.QName;
import com.trodix.documentstorage.persistance.repository.AspectRepository;
import lombok.AllArgsConstructor;

@Repository
@Transactional
@AllArgsConstructor
public class AspectDAO {

    private final NamedParameterJdbcTemplate tpl;

    private final AspectRepository aspectRepository;

    public Aspect save(final Aspect aspect) {

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final MapSqlParameterSource params = new MapSqlParameterSource();
        final String query;

        if (aspect.getId() == null) {
            final Optional<Aspect> existingAspect = findByQname(aspect.getQname());
            if (existingAspect.isPresent()) {
                query = "UPDATE aspect SET qname_id = :qname_id WHERE id = :id";
                params.addValue("id", existingAspect.get().getId());
            } else {
                query = "INSERT INTO aspect (qname_id) VALUES (:qname_id)";
            }
        } else {
            query = "UPDATE aspect SET qname_id = :qname_id WHERE id = :id";
            params.addValue("id", aspect.getId());
        }

        params.addValue("qname_id", aspect.getQname().getId());

        tpl.update(query, params, keyHolder);

        aspect.setId((Long) keyHolder.getKeys().get("id"));

        return aspect;
    }

    public Optional<Aspect> findByQname(final QName qname) {
        return aspectRepository.findOneByQname(qname);
    }

}
