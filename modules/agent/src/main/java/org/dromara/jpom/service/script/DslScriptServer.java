/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.configuration.ProjectLogConfig;
import org.dromara.jpom.exception.IllegalArgument2Exception;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.script.DslScriptBuilder;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.system.AgentWorkspaceEnvVarService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author bwcx_jzy
 * @since 23/12/30 030
 */
@Service
public class DslScriptServer {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;
    private final NodeScriptServer nodeScriptServer;
    private final ProjectLogConfig logConfig;
    private final JpomApplication jpomApplication;
    private final ProjectInfoService projectInfoService;

    public DslScriptServer(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService,
                           NodeScriptServer nodeScriptServer,
                           ProjectLogConfig logConfig,
                           JpomApplication jpomApplication,
                           ProjectInfoService projectInfoService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
        this.nodeScriptServer = nodeScriptServer;
        this.logConfig = logConfig;
        this.jpomApplication = jpomApplication;
        this.projectInfoService = projectInfoService;
    }

    /**
     * 异步执行
     *
     * @param dslYmlDto        dsl 配置
     * @param consoleCommandOp 操作
     */
    public void run(DslYmlDto dslYmlDto, ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, boolean sync) throws Exception {
        String log = projectInfoService.resolveAbsoluteLog(nodeProjectInfoModel, originalModel);
        DslScriptBuilder builder = this.create(dslYmlDto, consoleCommandOp, nodeProjectInfoModel, originalModel, log);
        Future<?> execute = ThreadUtil.execAsync(builder);
        if (sync) {
            execute.get();
        }
    }

    /**
     * 同步执行
     *
     * @param dslYmlDto        dsl 配置
     * @param consoleCommandOp 操作
     */
    public Tuple syncRun(DslYmlDto dslYmlDto, ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        try (DslScriptBuilder builder = this.create(dslYmlDto, consoleCommandOp, nodeProjectInfoModel, originalModel, null)) {
            return builder.syncExecute();
        }
    }

    /**
     * 解析流程脚本信息
     *
     * @param nodeProjectInfoModel 项目信息
     * @param dslYml               dsl 配置信息
     * @param op                   流程
     * @return data
     */
    public Tuple resolveProcessScript(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto dslYml, ConsoleCommandOp op) {
        DslYmlDto.BaseProcess baseProcess = dslYml.tryDslProcess(op.name());
        return this.resolveProcessScript(nodeProjectInfoModel, baseProcess);
    }

    /**
     * 解析流程脚本信息
     *
     * @param nodeProjectInfoModel 项目信息
     * @param scriptProcess        流程
     * @return data
     */
    private Tuple resolveProcessScript(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto.BaseProcess scriptProcess) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", false);
        if (scriptProcess == null) {
            jsonObject.put("msg", "流程不存在");
            return new Tuple(jsonObject, null);
        }
        String scriptId = scriptProcess.getScriptId();
        if (StrUtil.isEmpty(scriptId)) {
            jsonObject.put("msg", "请填写脚本模板id");
            return new Tuple(jsonObject, null);
        }
        //
        NodeScriptModel item = nodeScriptServer.getItem(scriptId);
        if (item != null) {
            // 脚本存在
            jsonObject.put("status", true);
            jsonObject.put("type", "script");
            jsonObject.put("scriptId", scriptId);
            return new Tuple(jsonObject, item);
        }
        File lib = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        File scriptFile = FileUtil.file(lib, scriptId);
        if (FileUtil.isFile(scriptFile)) {
            // 文件存在
            jsonObject.put("status", true);
            jsonObject.put("type", "file");
            jsonObject.put("scriptId", scriptId);
            return new Tuple(jsonObject, scriptFile);
        }
        jsonObject.put("msg", "脚本模版不存在:" + scriptId);
        return new Tuple(jsonObject, null);
    }

    /**
     * 构建 DSL 执行器
     *
     * @param dslYmlDto            脚本流程
     * @param nodeProjectInfoModel 项目
     * @param log                  日志路径
     * @param consoleCommandOp     具体操作
     */
    private DslScriptBuilder create(DslYmlDto dslYmlDto, ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, String log) {
        DslYmlDto.BaseProcess scriptProcess = dslYmlDto.getDslProcess(consoleCommandOp.name());
        Tuple tuple = this.resolveProcessScript(originalModel, scriptProcess);
        JSONObject jsonObject = tuple.get(0);
        // 判断状态
        boolean status = jsonObject.getBooleanValue("status");
        cn.hutool.core.lang.Assert.isTrue(status, () -> {
            String msg = jsonObject.getString("msg");
            return new IllegalArgument2Exception(msg);
        });
        String type = jsonObject.getString("type");
        EnvironmentMapBuilder environment = this.environment(nodeProjectInfoModel, scriptProcess);
        environment.put("PROJECT_LOG_FILE", log);
        DslYmlDto.Run run = dslYmlDto.getRun();
        String execPath = run.getExecPath();
        environment.put("JPOM_EXEC_PATH", execPath);
        File scriptFile;
        boolean autoDelete = false;
        if (StrUtil.equals(type, "file")) {
            scriptFile = tuple.get(1);
        } else {
            NodeScriptModel item = tuple.get(1);
            scriptFile = this.initScriptFile(item);
            // 系统生成的脚本需要自动删除
            autoDelete = true;
        }
        DslScriptBuilder builder = new DslScriptBuilder(consoleCommandOp.name(), environment, scriptProcess.getScriptArgs(), log, logConfig.getFileCharset());
        builder.setScriptFile(scriptFile);
        builder.setAutoDelete(autoDelete);
        return builder;
    }

    /**
     * 创建脚本文件
     *
     * @param scriptModel 脚本对象
     * @return file
     */
    private File initScriptFile(NodeScriptModel scriptModel) {
        String dataPath = jpomApplication.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        // 替换内容
        String context = scriptModel.getContext();
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }

    private EnvironmentMapBuilder environment(NodeProjectInfoModel nodeProjectInfoModel, DslYmlDto.BaseProcess scriptProcess) {
        //
        EnvironmentMapBuilder environmentMapBuilder = agentWorkspaceEnvVarService.getEnv(nodeProjectInfoModel.getWorkspaceId());
        // 项目配置的环境变量
        String dslEnv = nodeProjectInfoModel.getDslEnv();
        Opt.ofBlankAble(dslEnv)
            .map(s -> UrlQuery.of(s, CharsetUtil.CHARSET_UTF_8))
            .map(UrlQuery::getQueryMap)
            .map(map -> {
                Map<String, String> map1 = MapUtil.newHashMap();
                for (Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
                    map1.put(StrUtil.toString(entry.getKey()), StrUtil.toString(entry.getValue()));
                }
                return map1;
            })
            .ifPresent(environmentMapBuilder::putStr);
        String lib = projectInfoService.resolveLibPath(nodeProjectInfoModel);
        //
        environmentMapBuilder
            .putStr(scriptProcess.getScriptEnv())
            .put("PROJECT_ID", nodeProjectInfoModel.getId())
            .put("PROJECT_NAME", nodeProjectInfoModel.getName())
            .put("PROJECT_PATH", lib);
        return environmentMapBuilder;
    }
}
