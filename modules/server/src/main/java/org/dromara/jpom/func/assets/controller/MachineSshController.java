/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.text.csv.*;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.AssetsConfig;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.func.BaseGroupNameController;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.log.SshTerminalExecuteLog;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.dblog.SshTerminalExecuteLogService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
@RestController
@RequestMapping(value = "/system/assets/ssh")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_SSH)
@SystemPermission
@Slf4j
public class MachineSshController extends BaseGroupNameController {

    private final MachineSshServer machineSshServer;
    private final SshService sshService;
    private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private final WorkspaceService workspaceService;
    private final ServerConfig serverConfig;
    private final AssetsConfig.SshConfig sshConfig;

    public MachineSshController(MachineSshServer machineSshServer,
                                SshService sshService,
                                SshTerminalExecuteLogService sshTerminalExecuteLogService,
                                WorkspaceService workspaceService,
                                ServerConfig serverConfig,
                                AssetsConfig assetsConfig) {
        super(machineSshServer);
        this.machineSshServer = machineSshServer;
        this.sshService = sshService;
        this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
        this.workspaceService = workspaceService;
        this.serverConfig = serverConfig;
        this.sshConfig = assetsConfig.getSsh();
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<MachineSshModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineSshModel> pageResultDto = machineSshServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }


    @Override
    @GetMapping(value = "list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Collection<String>> listGroup() {
        Collection<String> list = dbService.listGroupName();
        // 合并配置禁用分组
        List<String> monitorGroupName = sshConfig.getDisableMonitorGroupName();
        if (monitorGroupName != null) {
            list.addAll(monitorGroupName);
            //
            list.remove("*");
            list = new HashSet<>(list);
        }
        return JsonMessage.success("", list);
    }

    /**
     * 编辑
     *
     * @param name        名称
     * @param host        端口
     * @param user        用户名
     * @param password    密码
     * @param connectType 连接方式
     * @param privateKey  私钥
     * @param port        端口
     * @param charset     编码格式
     * @param id          ID
     * @return json
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "ssh名称不能为空") String name,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host不能为空") String host,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user不能为空") String user,
                                     String password,
                                     MachineSshModel.ConnectType connectType,
                                     String privateKey,
                                     @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port错误") int port,
                                     String charset,
                                     String id,
                                     Integer timeout,
                                     String allowEditSuffix,
                                     String groupName) {
        boolean add = StrUtil.isEmpty(id);
        if (add) {
            // 优先判断参数 如果是 password 在修改时可以不填写
            if (connectType == MachineSshModel.ConnectType.PASS) {
                Assert.hasText(password, "请填写登录密码");
            } else if (connectType == MachineSshModel.ConnectType.PUBKEY) {
                //Assert.hasText(privateKey, "请填写证书内容");
            }
        } else {
            boolean exists = machineSshServer.exists(new MachineSshModel(id));
            Assert.state(exists, "不存在对应ssh");
        }
        MachineSshModel sshModel = new MachineSshModel();
        sshModel.setId(id);
        sshModel.setGroupName(groupName);
        sshModel.setHost(host);
        // 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
        Opt.ofBlankAble(password).ifPresent(sshModel::setPassword);
        if (StrUtil.startWith(privateKey, URLUtil.FILE_URL_PREFIX)) {
            String rsaPath = StrUtil.removePrefix(privateKey, URLUtil.FILE_URL_PREFIX);
            Assert.state(FileUtil.isFile(rsaPath), "配置的私钥文件不存在");
        }
        Opt.ofNullable(privateKey).ifPresent(sshModel::setPrivateKey);

        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        sshModel.allowEditSuffix(allowEditSuffixList);
        sshModel.setPort(port);
        sshModel.setUser(user);
        sshModel.setName(name);
        sshModel.setConnectType(connectType.name());
        sshModel.setTimeout(timeout);
        try {
            Charset.forName(charset);
            sshModel.setCharset(charset);
        } catch (Exception e) {
            return new JsonMessage<>(405, "请填写正确的编码格式," + e.getMessage());
        }
        // 判断重复
        Entity entity = Entity.create();
        entity.set("host", sshModel.getHost());
        entity.set("port", sshModel.getPort());
        entity.set(DialectUtil.wrapField("user"), sshModel.getUser());
        entity.set("connectType", sshModel.getConnectType());
        Opt.ofBlankAble(id).ifPresent(s -> entity.set("id", StrUtil.format(" <> {}", s)));
        boolean exists = machineSshServer.exists(entity);
        Assert.state(!exists, "对应的SSH已经存在啦");
        try {

            String workspaceId = getWorkspaceId();
            Session session = machineSshServer.getSessionByModel(sshModel);
            JschUtil.close(session);
        } catch (Exception e) {
            log.warn("ssh连接失败", e);
            return new JsonMessage<>(505, "ssh连接失败,请检查用户名、密码、host、端口等填写是否正确，超时时间是否合理：" + e.getMessage());
        }
        sshModel.setStatus(1);
        int i = add ? machineSshServer.insert(sshModel) : machineSshServer.updateById(sshModel);
        return JsonMessage.success("操作成功");
    }


    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(@ValidatorItem String id) {
        long count = sshService.countByMachine(id);
        Assert.state(count <= 0, "当前机器SSH还关联" + count + "个ssh不能删除");
        machineSshServer.delByKey(id);
        return JsonMessage.success("操作成功");
    }

    /**
     * 执行记录
     *
     * @return json
     */
    @PostMapping(value = "log-list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<SshTerminalExecuteLog>> logListData(HttpServletRequest request) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(paramMap);
        return JsonMessage.success("获取成功", pageResult);
    }

    @GetMapping(value = "list-workspace-ssh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<SshModel>> listWorkspaceSsh(@ValidatorItem String id) {
        MachineSshModel machineSshModel = machineSshServer.getByKey(id);
        Assert.notNull(machineSshModel, "没有对应的机器");
        SshModel sshModel = new SshModel();
        sshModel.setMachineSshId(id);
        List<SshModel> modelList = sshService.listByBean(sshModel);
        modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
        for (SshModel model : modelList) {
            model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
        }
        return JsonMessage.success("", modelList);
    }

    /**
     * 保存工作空间配置
     *
     * @param fileDirs          文件夹
     * @param id                ID
     * @param notAllowedCommand 禁止输入的命令
     * @return json
     */
    @PostMapping(value = "save-workspace-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> saveWorkspaceConfig(
        String fileDirs,
        @ValidatorItem String id,
        String notAllowedCommand,
        String allowEditSuffix) {
        SshModel sshModel = new SshModel(id);
        // 目录
        if (StrUtil.isEmpty(fileDirs)) {
            sshModel.fileDirs(null);
        } else {
            List<String> list = StrSplitter.splitTrim(fileDirs, StrUtil.LF, true);
            for (String s : list) {
                String normalize = FileUtil.normalize(s + StrUtil.SLASH);
                int count = StrUtil.count(normalize, StrUtil.SLASH);
                Assert.state(count >= 2, "ssh 授权目录不能是根目录");
            }
            //
            UserModel userModel = getUser();
            Assert.state(!userModel.isDemoUser(), PermissionInterceptor.DEMO_TIP);
            sshModel.fileDirs(list);
        }
        sshModel.setNotAllowedCommand(notAllowedCommand);
        // 获取允许编辑的后缀
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        sshModel.allowEditSuffix(allowEditSuffixList);
        sshService.updateById(sshModel);
        return JsonMessage.success("操作成功");
    }

    /**
     * 将 ssh 分配到指定工作空间
     *
     * @param ids         ssh id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String id : list) {
            MachineSshModel machineSshModel = machineSshServer.getByKey(id);
            Assert.notNull(machineSshModel, "没有对应的ssh");
            boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
            Assert.state(exists, "不存在对应的工作空间");
            //
            if (!sshService.existsSsh2(workspaceId, id)) {
                //
                sshService.insert(machineSshModel, workspaceId);
            }
        }

        return JsonMessage.success("操作成功");
    }

    /**
     * edit
     *
     * @param id ssh id
     * @return json
     */
    @PostMapping(value = "rest-hide-field", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> restHideField(@ValidatorItem String id) {
        MachineSshModel machineSshModel = new MachineSshModel();
        machineSshModel.setId(id);
        machineSshModel.setPassword(StrUtil.EMPTY);
        machineSshModel.setPrivateKey(StrUtil.EMPTY);
        machineSshServer.updateById(machineSshModel);
        return new JsonMessage<>(200, "操作成功");
    }

    /**
     * 下载导入模板
     */
    @GetMapping(value = "import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "ssh导入模板.csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("name", "groupName", "host", "port", "user", "password", "charset", "connectType", "privateKey", "timeout");
        writer.flush();
    }


    /**
     * 导出数据
     */
    @GetMapping(value = "export-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void exportData(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String fileName = "导出的 ssh 数据 " + DateTime.now().toString(DatePattern.NORM_DATE_FORMAT) + ".csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        int pageInt = 0;
        writer.writeLine("name", "groupName", "host", "port", "user", "password", "charset", "connectType", "privateKey", "timeout");
        while (true) {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            paramMap.remove("workspaceId");
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<MachineSshModel> listPage = machineSshServer.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<MachineSshModel, List<Object>>) machineSshModel -> CollUtil.newArrayList(
                    machineSshModel.getName(),
                    machineSshModel.getGroupName(),
                    machineSshModel.getHost(),
                    machineSshModel.getPort(),
                    machineSshModel.getUser(),
                    machineSshModel.getPassword(),
                    machineSshModel.getCharset(),
                    machineSshModel.getConnectType(),
                    machineSshModel.getPrivateKey(),
                    machineSshModel.getTimeout()
                ))
                .map(objects -> objects.stream().map(StrUtil::toStringOrNull).toArray(String[]::new))
                .forEach(writer::writeLine);
            if (ObjectUtil.equal(listPage.getPage(), listPage.getTotalPage())) {
                // 最后一页
                break;
            }
        }
        writer.flush();
    }

    /**
     * 导入数据
     *
     * @return json
     */
    @PostMapping(value = "import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> importData(MultipartFile file) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.state(StrUtil.endWithIgnoreCase(extName, "csv"), "不允许的文件格式");
        assert originalFilename != null;
        File csvFile = FileUtil.file(serverConfig.getUserTempPath(), originalFilename);
        int addCount = 0, updateCount = 0;
        Charset fileCharset;
        try {
            file.transferTo(csvFile);
            fileCharset = CharsetDetector.detect(csvFile);
            Reader bomReader = FileUtil.getReader(csvFile, fileCharset);
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
            CsvData csvData;
            try {
                csvData = reader.read();
            } catch (Exception e) {
                log.error("解析 csv 异常", e);
                return new JsonMessage<>(405, "解析文件异常," + e.getMessage());
            } finally {
                IoUtil.close(reader);
            }
            List<CsvRow> rows = csvData.getRows();
            Assert.notEmpty(rows, "没有任何数据");

            for (int i = 0; i < rows.size(); i++) {
                CsvRow csvRow = rows.get(i);
                String name = csvRow.getByName("name");
                int finalI = i;
                Assert.hasText(name, () -> StrUtil.format("第 {} 行 name 字段不能位空", finalI + 1));
                String groupName = csvRow.getByName("groupName");
                String host = csvRow.getByName("host");
                Assert.hasText(host, () -> StrUtil.format("第 {} 行 host 字段不能位空", finalI + 1));
                Integer port = Convert.toInt(csvRow.getByName("port"));
                Assert.state(port != null && NetUtil.isValidPort(port), () -> StrUtil.format("第 {} 行 port 字段不能位空或者不正确", finalI + 1));
                String user = csvRow.getByName("user");
                Assert.hasText(host, () -> StrUtil.format("第 {} 行 user 字段不能位空", finalI + 1));
                String password = csvRow.getByName("password");
                String charset = csvRow.getByName("charset");
                //
                String type = csvRow.getByName("connectType");
                type = StrUtil.emptyToDefault(type, "").toUpperCase();
                MachineSshModel.ConnectType connectType = EnumUtil.fromString(MachineSshModel.ConnectType.class, type, MachineSshModel.ConnectType.PASS);
                String privateKey = csvRow.getByName("privateKey");
                Integer timeout = Convert.toInt(csvRow.getByName("timeout"));
                //
                MachineSshModel where = new MachineSshModel();
                where.setHost(host);
                where.setUser(user);
                where.setPort(port);
                where.setConnectType(connectType.name());
                MachineSshModel machineSshModel = machineSshServer.queryByBean(where);
                if (machineSshModel == null) {
                    // 添加
                    where.setName(name);
                    where.setGroupName(groupName);
                    where.setPassword(password);
                    where.setPrivateKey(privateKey);
                    where.setTimeout(timeout);
                    where.setCharset(charset);
                    machineSshServer.insert(where);
                    addCount++;
                } else {
                    MachineSshModel update = new MachineSshModel();
                    update.setId(machineSshModel.getId());
                    update.setName(name);
                    update.setGroupName(groupName);
                    update.setPassword(password);
                    update.setPrivateKey(privateKey);
                    update.setTimeout(timeout);
                    update.setCharset(charset);
                    machineSshServer.updateById(update);
                    updateCount++;
                }
            }
        } finally {
            FileUtil.del(csvFile);
        }
        String fileCharsetStr = Optional.ofNullable(fileCharset).map(Charset::name).orElse(StrUtil.EMPTY);
        return JsonMessage.success("导入成功(编码格式：{}),添加 {} 条数据,修改 {} 条数据", fileCharsetStr, addCount, updateCount);
    }
}
