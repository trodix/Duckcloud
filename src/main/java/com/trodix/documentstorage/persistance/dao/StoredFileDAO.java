package com.trodix.documentstorage.persistance.dao;

import com.trodix.documentstorage.persistance.entity.StoredFile;
import com.trodix.documentstorage.persistance.mapper.QNameRowMapper;
import com.trodix.documentstorage.persistance.mapper.StoredFileRowMapper;
import com.trodix.documentstorage.persistance.utils.DaoUtils;
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

    public int findLatestVersion(Long nodeDbId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_id", nodeDbId);

        final String existRelationQuery = """
                    SELECT sf.version as sf_version FROM stored_file sf
                        WHERE sf.node_id = :node_id""";

        final RowCountCallbackHandler rowCount = new RowCountCallbackHandler();
        tpl.query(existRelationQuery, params, rowCount);

        return rowCount.getRowCount();
    }

    public int findLatestVersion(String nodeUuid) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_uuid", nodeUuid);

        final String existRelationQuery = """
                    SELECT sf.version as sf_version FROM stored_file sf
                        INNER JOIN node n ON n.id = sf.node_id
                        WHERE n.uuid = :node_uuid""";

        final RowCountCallbackHandler rowCount = new RowCountCallbackHandler();
        tpl.query(existRelationQuery, params, rowCount);

        return rowCount.getRowCount();
    }

    public String findStoredFile(String nodeUuid, int version) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_uuid", nodeUuid);
        params.addValue("version", version);

        final String query = """
                    SELECT sf.uuid as sf_uuid FROM stored_file sf
                        INNER JOIN node n ON n.id = sf.node_id
                        WHERE n.uuid = :node_uuid
                        AND sf.version = :version""";

        return DaoUtils.findOne(tpl.query(query, params, new StoredFileRowMapper())).orElse(null);
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
