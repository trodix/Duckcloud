package com.trodix.documentstorage.persistance.dao;

import com.trodix.documentstorage.persistance.entity.StoredFile;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@AllArgsConstructor
public class StoredFileDAO {

    private final NamedParameterJdbcTemplate tpl;

    public int findLatestVersion(Long nodeId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_id", nodeId);

        final String existRelationQuery = """
                    SELECT sf.version FROM stored_file sf
                        WHERE sf.node_id = :node_id""";

        final RowCountCallbackHandler rowCount = new RowCountCallbackHandler();
        tpl.query(existRelationQuery, params, rowCount);

        return rowCount.getRowCount();
    }

    public StoredFile save(StoredFile storedFile) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_id", storedFile.getNode().getDbId());
        params.addValue("uuid", storedFile.getUuid());
        params.addValue("version", storedFile.getVersion());

        final String query = "INSERT INTO stored_file (node_id, uuid, version) VALUES (:node_id, :uuid, :version)";
        tpl.update(query, params, keyHolder);

        storedFile.setId((Long) keyHolder.getKeys().get("id"));

        return storedFile;
    }

}
