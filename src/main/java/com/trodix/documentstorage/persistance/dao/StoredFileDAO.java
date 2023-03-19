package com.trodix.documentstorage.persistance.dao;

import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.persistance.entity.StoredFile;
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

import java.util.List;

@Repository
@Transactional
@AllArgsConstructor
public class StoredFileDAO {

    private final NamedParameterJdbcTemplate tpl;

    public int findStoredFileLatestVersion(Long nodeDbId) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_id", nodeDbId);

        final String existRelationQuery = """
                    SELECT sf.version as sf_version FROM stored_file sf
                        WHERE sf.node_id = :node_id""";

        final RowCountCallbackHandler rowCount = new RowCountCallbackHandler();
        tpl.query(existRelationQuery, params, rowCount);

        return rowCount.getRowCount();
    }

    public int findStoredFileLatestVersion(String nodeUuid) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_uuid", nodeUuid);

        final String latestVersionQuery = """
                    SELECT coalesce(max(sf.version), 0) as latest_version FROM stored_file sf
                    INNER JOIN node n ON n.id = sf.node_id
                    WHERE n.uuid = :node_uuid
                    """;

        return tpl.queryForObject(latestVersionQuery, params, Integer.class);
    }

    public String findStoredFile(String nodeUuid, int version) {

        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_uuid", nodeUuid);
        params.addValue("type_content_namespace", ContentModel.TYPE_CONTENT.split(":")[0]);
        params.addValue("type_content_name", ContentModel.TYPE_CONTENT.split(":")[1]);
        params.addValue("version", version);

        final String query = """
                SELECT sf.uuid as sf_uuid FROM stored_file sf
                    INNER JOIN node n ON n.id = sf.node_id
                    INNER JOIN type t ON t.id = n.type_id
                    INNER JOIN qname q ON q.id = t.qname_id
                    INNER JOIN namespace n2 ON q.namespace_id = n2.id
                    WHERE n.uuid = :node_uuid
                    AND sf.version = :version
                    """;

        return DaoUtils.findOne(tpl.query(query, params, new StoredFileRowMapper())).orElse(null);
    }

    public List<String> findAllFileContentVersions(String nodeUuid) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("node_uuid", nodeUuid);

        final String query = """
                SELECT sf.uuid as sf_uuid FROM stored_file sf
                    INNER JOIN node n ON n.id = sf.node_id
                    INNER JOIN type t ON t.id = n.type_id
                    INNER JOIN qname q ON q.id = t.qname_id
                    INNER JOIN namespace n2 ON q.namespace_id = n2.id
                    WHERE n.uuid = :node_uuid
                    """;

        return tpl.query(query, params, new StoredFileRowMapper());
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
