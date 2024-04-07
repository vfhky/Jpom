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

import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/1/5
 */
public interface IStorageSqlBuilderService extends IMode {

    /**
     * 生成表 sql
     *
     * @param name 表名称
     * @param desc 表描述
     * @param row  字段
     * @return sql
     */
    String generateTableSql(String name, String desc, List<TableViewData> row);

    /**
     * 生成 修改表 sql
     *
     * @param row 需要修改的字段
     * @return sql
     */
    String generateAlterTableSql(List<TableViewAlterData> row);

    /**
     * 生成 修改表 sql
     *
     * @param row 需要修改的字段
     * @return sql
     */
    String generateIndexSql(List<TableViewIndexData> row);

    /**
     * 根据字段信息生成 sql
     *
     * @param tableViewRowData 字段信息
     * @return sql
     */
    String generateColumnSql(TableViewRowData tableViewRowData);

    /**
     * 根据表名和字段信息生成 sql
     *
     * @param tableName 表名
     * @param tableViewRowData 字段信息
     * @return sql
     */
    default String generateColumnSql(String tableName,TableViewRowData tableViewRowData) {
        return generateColumnSql(tableViewRowData);
    }

    /**
     * sql 分隔执行标记
     *
     * @return 分隔标记
     */
    default String delimiter() {
        return StrUtil.EMPTY;
    }
}
