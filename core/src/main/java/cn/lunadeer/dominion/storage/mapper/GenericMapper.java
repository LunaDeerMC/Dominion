package cn.lunadeer.dominion.storage.mapper;

import cn.lunadeer.dominion.storage.DatabaseType;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Map;

public interface GenericMapper {

    @SelectProvider(type = SqlProvider.class, method = "selectAll")
    List<Map<String, Object>> selectAll(@Param("table") String table);

    @SelectProvider(type = SqlProvider.class, method = "selectWhere")
    List<Map<String, Object>> selectWhere(@Param("table") String table,
                                          @Param("column") String column,
                                          @Param("value") Object value);

    @SelectProvider(type = SqlProvider.class, method = "selectWhereAll")
    List<Map<String, Object>> selectWhereAll(@Param("table") String table,
                                             @Param("values") Map<String, Object> values);

    @SelectProvider(type = SqlProvider.class, method = "selectWhereCompare")
    List<Map<String, Object>> selectWhereCompare(@Param("table") String table,
                                                 @Param("column") String column,
                                                 @Param("operator") String operator,
                                                 @Param("value") Object value);

    @SelectProvider(type = SqlProvider.class, method = "selectDominionsByServer")
    List<Map<String, Object>> selectDominionsByServer(@Param("serverId") Integer serverId);

    @SelectProvider(type = SqlProvider.class, method = "selectValue")
    Object selectValue(@Param("table") String table,
                       @Param("selectColumn") String selectColumn,
                       @Param("whereColumn") String whereColumn,
                       @Param("value") Object value);

    @InsertProvider(type = SqlProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "values.id", keyColumn = "id")
    int insert(@Param("table") String table, @Param("values") Map<String, Object> values);

    @InsertProvider(type = SqlProvider.class, method = "insertIgnore")
    int insertIgnore(@Param("table") String table,
                     @Param("values") Map<String, Object> values,
                     @Param("databaseType") DatabaseType databaseType,
                     @Param("conflictColumn") String conflictColumn);

    @UpdateProvider(type = SqlProvider.class, method = "updateColumns")
    int updateColumns(@Param("table") String table,
                      @Param("idColumn") String idColumn,
                      @Param("id") Object id,
                      @Param("values") Map<String, Object> values);

    @DeleteProvider(type = SqlProvider.class, method = "deleteWhere")
    int deleteWhere(@Param("table") String table,
                    @Param("column") String column,
                    @Param("value") Object value);

    @DeleteProvider(type = SqlProvider.class, method = "deleteWhereAll")
    int deleteWhereAll(@Param("table") String table, @Param("values") Map<String, Object> values);

    @InsertProvider(type = SqlProvider.class, method = "upsertTeleport")
    int upsertTeleport(@Param("uuid") String uuid,
                       @Param("dominionId") Integer dominionId,
                       @Param("databaseType") DatabaseType databaseType);
}