/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.db;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.exceptions.CheckedUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.*;
import cn.hutool.db.*;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.sql.Wrapper;
import cn.hutool.setting.Setting;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.util.Assert;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据存储服务
 *
 * @author bwcx_jzy
 * @since 2023/1/5
 */
@Slf4j
public class StorageServiceFactory {

    private static final String DB = "db";
    /**
     * 当前运行模式
     */
    private static DbExtConfig.Mode mode;

    /**
     * 配置当前数据库 模式
     *
     * @param mode mode
     */
    public static void setMode(DbExtConfig.Mode mode) {
        StorageServiceFactory.mode = mode;
    }

    public static DbExtConfig.Mode getMode() {
        return mode;
    }

    /**
     * 将数据迁移到当前环境
     */
    public static void migrateH2ToNow(DbExtConfig dbExtConfig, String h2Url, String h2User, String h2Pass,DbExtConfig.Mode targetNode) {
        log.info("开始迁移 h2 数据到 {}", dbExtConfig.getMode());
        Assert.notNull(mode,"未指定目标数据库信息");
        try {
            IStorageService h2StorageService = doCreateStorageService(DbExtConfig.Mode.H2);
            boolean hasDbData = h2StorageService.hasDbData();
            if (!hasDbData) {
                throw new JpomRuntimeException("没有 h2 数据信息不用迁移");
            }
            long time = SystemClock.now();
            DSFactory h2DsFactory = h2StorageService.create(dbExtConfig, h2Url, h2User, h2Pass);
            h2DsFactory.getDataSource();
            log.info("成功连接 H2 ,开始尝试自动备份");
            // 设置默认备份 SQL 的文件地址
            String fileName = LocalDateTimeUtil.format(LocalDateTimeUtil.now(), DatePattern.PURE_DATETIME_PATTERN);
            File file = FileUtil.file(StorageServiceFactory.dbLocalPath(), DbExtConfig.BACKUP_DIRECTORY_NAME, fileName + DbExtConfig.SQL_FILE_SUFFIX);
            String backupSqlPath = FileUtil.getAbsolutePath(file);
            Setting setting = h2StorageService.createSetting(dbExtConfig, h2Url, h2User, h2Pass);
            // 数据源参数
            String url = setting.get("url");
            String user = setting.get("user");
            String pass = setting.get("pass");
            h2StorageService.backupSql(url, user, pass, backupSqlPath, null);
            log.info("H2 数据库备份成功：{}", backupSqlPath);
            //
            IStorageService nowStorageService = doCreateStorageService(dbExtConfig.getMode());
            DSFactory nowDsFactory = nowStorageService.create(dbExtConfig, null, null, null);
            nowDsFactory.getDataSource();
            log.info("成功连接 {} {}", dbExtConfig.getMode(), dbExtConfig.getUrl());
            Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("org.dromara.jpom", TableName.class);
            classes = classes.stream()
                    .filter(aClass -> {
                        TableName tableName = aClass.getAnnotation(TableName.class);
                        DbExtConfig.Mode[] modes = tableName.modes();
                        if (ArrayUtil.isEmpty(modes)) {
                            return true;
                        }
                        return ArrayUtil.contains(modes, dbExtConfig.getMode());
                    })
                    .sorted((o1, o2) -> StrUtil.compare(o1.getSimpleName(), o2.getSimpleName(), false))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            log.info("准备迁移数据");
            int total = 0;
            for (Class<?> aClass : classes) {
                total += migrateH2ToNowItem(aClass, h2DsFactory, nowDsFactory,targetNode);
            }
            long endTime = SystemClock.now();
            log.info("迁移完成,累计迁移 {} 条数据,耗时：{}", total, DateUtil.formatBetween(endTime - time));
            h2DsFactory.destroy();
            nowDsFactory.destroy();
            log.info("准备删除当前数据库文件");
            String dbFiles = h2StorageService.deleteDbFiles();
            log.info("自动备份 h2 数据库文件,备份文件位于：{}", dbFiles);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    private static int migrateH2ToNowItem(Class<?> aClass, DSFactory h2DsFactory, DSFactory targetDsFactory,DbExtConfig.Mode targetNode) throws SQLException {
        TableName tableName = aClass.getAnnotation(TableName.class);
        Wrapper targetModeWrapper = DialectUtil.getDialectByMode(targetNode).getWrapper();
        Set<String> boolFieldSet = Arrays.stream(ReflectUtil.getFields(aClass, field -> Boolean.class.equals(field.getType()) || boolean.class.equals(field.getType())))
            .map(Field::getName)
            .collect(Collectors.toSet());

        log.info("开始迁移 {} {}", tableName.name(), tableName.value());
        int total = 0;
        while (true) {
            Entity where = Entity.create(tableName.value());
            PageResult<Entity> pageResult;
            Db db = Db.use(h2DsFactory.getDataSource(),DialectUtil.getH2Dialect());
            Page page = new Page(1, 200);
            pageResult = db.page(where, page);
            if (pageResult.isEmpty()) {
                break;
            }
            // 过滤需要忽略迁移的数据
            List<Entity> newResult = pageResult.stream()
                .map(entity -> entity.toBeanIgnoreCase(aClass))
                .map(o -> {
                    // 兼容大小写
                    Entity entity = Entity.create(targetModeWrapper.wrap(tableName.value()));
                    return entity.parseBean(o, false, true);
                }).peek(entity -> {
                    if( DbExtConfig.Mode.POSTGRESQL.equals(targetNode) ) {
                        // tinyint类型查出来是数字，需转为bool
                        boolFieldSet.forEach(fieldName->{
                            Object field = entity.get(fieldName);
                            if( field instanceof Number ) {
                                entity.set(fieldName,BooleanUtil.toBoolean(field.toString()));
                            }
                        });
                    }
                }).collect(Collectors.toList());
            if (newResult.isEmpty()) {
                if (pageResult.isLast()) {
                    // 最后一页
                    break;
                }
                // 继续
                continue;
            }
            total += newResult.size();
            // 插入信息数据
            Db db2 = Db.use(targetDsFactory.getDataSource(),DialectUtil.getDialectByMode(targetNode));

            Connection connection = db2.getConnection();
            try {
                SqlConnRunner runner = db2.getRunner();
                for (Entity entity : newResult) {
                    // hutool的批量insert方法有坑，可能导致部分参数被丢弃
                    runner.insert(connection,entity);
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }finally {
                db2.closeConnection(connection);
            }

            // 删除数据
            Entity deleteWhere = Entity.create(tableName.value());
            deleteWhere.set("id", newResult.stream().map(entity -> entity.getStr("id")).collect(Collectors.toList()));
            db.del(deleteWhere);
        }
        log.info("{} 迁移成功 {} 条数据", tableName.name(), total);
        return total;
    }

    /**
     * 加载 本地已经执行的记录
     *
     * @return sha1 log
     * @author bwcx_jzy
     */
    public static Set<String> loadExecuteSqlLog() {
        File localPath = dbLocalPath();
        File file = FileUtil.file(localPath, "execute.init.sql.log");
        if (!FileUtil.isFile(file)) {
            // 不存在或者是文件夹
            FileUtil.del(file);
            return new LinkedHashSet<>();
        }
        List<String> strings = FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8);
        return new LinkedHashSet<>(strings);
    }

    /**
     * 获取数据库保存路径
     *
     * @return 默认本地数据目录下面的 db 目录
     * @author bwcx_jzy
     */
    public static File dbLocalPath() {
        return FileUtil.file(ExtConfigBean.getPath(), DB);
    }

    /**
     * 清除执行记录
     */
    public static void clearExecuteSqlLog() {
        File localPath = dbLocalPath();
        File file = FileUtil.file(localPath, "execute.init.sql.log");
        FileUtil.del(file);
    }

    /**
     * 保存本地已经执行的记录
     *
     * @author bwcx_jzy
     */
    public static void saveExecuteSqlLog(Set<String> logs) {
        File localPath = dbLocalPath();
        File file = FileUtil.file(localPath, "execute.init.sql.log");
        FileUtil.writeUtf8Lines(logs, file);
    }

    /**
     * 获得单例的 IStorageService
     *
     * @return 单例的 IStorageService
     */
    public static IStorageService get() {
        Assert.notNull(mode, "当前数据库模式未知");
        return Singleton.get(IStorageService.class.getName(), (CheckedUtil.Func0Rt<IStorageService>) () -> doCreateStorageService(mode));
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static IStorageService doCreateStorageService(DbExtConfig.Mode mode) {
        final List<IStorageService> storageServiceList = ServiceLoaderUtil.loadList(IStorageService.class);
        if (storageServiceList != null) {
            for (IStorageService storageService : storageServiceList) {
                if (storageService.mode() == mode) {
                    return storageService;
                }
            }
        }
        throw new RuntimeException("No Jpom Storage " + mode + " jar found ! Please add one of it to your project !");
    }
}
