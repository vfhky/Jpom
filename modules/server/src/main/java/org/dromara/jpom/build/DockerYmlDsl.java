/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.setting.yaml.YamlUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.plugins.IPlugin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.IDockerConfigPlugin;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * docker 构建 配置
 * <p>
 * <a href="https://www.jianshu.com/p/54cfa5721d5f">https://www.jianshu.com/p/54cfa5721d5f</a>
 *
 * @author bwcx_jzy
 * @since 2022/1/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class DockerYmlDsl extends BaseJsonModel {

    /**
     * 基础镜像
     */
    private String runsOn;
    /**
     * 使用对应到 docker tag 构建
     */
    private String fromTag;
    /**
     * 构建步骤
     */
    private List<Map<String, Object>> steps;

    /**
     * 将本地文件复制到 容器
     * <p>
     * <host path>:<container path>:true
     * <p>
     * * If this flag is set to true, all children of the local directory will be copied to the remote without the root directory. For ex: if
     * * I have root/titi and root/tata and the remote path is /var/data. dirChildrenOnly = true will create /var/data/titi and /var/data/tata
     * * dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata
     * *
     * * @param dirChildrenOnly
     * *            if root directory is ignored
     */
    private List<String> copy;
    /**
     * bind mounts 将宿主机上的任意位置的文件或者目录挂在到容器 （--mount type=bind,src=源目录,dst=目标目录）
     * /host:/container:ro
     */
    private List<String> binds;
    /**
     * 环境变量
     */
    private Map<String, String> env;
    /**
     * <a href="https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate">https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate</a>
     * <p>
     * cpuCount
     * <p>
     * cpuPercent
     * <p>
     * memoryReservation
     * <p>
     * cpusetCpus 允许执行的CPU（例如，0-3, 0,1）。
     * <p>
     * cpuShares
     */
    private Map<String, String> hostConfig;

    /**
     * 验证信息是否正确
     *
     * @param dockerInfoService   容器server
     * @param machineDockerServer 机器server
     * @param workspaceId         工作空间id
     * @param plugin              插件
     */
    public void check(DockerInfoService dockerInfoService,
                      MachineDockerServer machineDockerServer,
                      String workspaceId,
                      IDockerConfigPlugin plugin) {
        Assert.hasText(runsOn, "请填写runsOn。");
        Validator.validateMatchRegex(StringUtil.GENERAL_STR, runsOn, "runsOn 镜像名称不合法");
        Assert.state(CollUtil.isNotEmpty(steps), "请填写 steps");
        this.stepsCheck(dockerInfoService, machineDockerServer, workspaceId, plugin);
    }

    /**
     * 检查 steps
     */
    private void stepsCheck(DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer,
                            String workspaceId,
                            IDockerConfigPlugin plugin) {
        Set<String> usesSet = new HashSet<>();
        boolean containsRun = false;
        for (Map<String, Object> step : steps) {
            if (!containsRun && step.containsKey("run")) {
                containsRun = true;
            }
            if (step.containsKey("env")) {
                Assert.isInstanceOf(Map.class, step.get("env"), "env 必须是 map 类型");
            }
            if (step.containsKey("uses")) {
                Assert.isInstanceOf(String.class, step.get("uses"), "uses 只支持 String 类型");
                String uses = (String) step.get("uses");
                if ("node".equals(uses)) {
                    nodePluginCheck(step);
                } else if ("java".equals(uses)) {
                    javaPluginCheck(step);
                } else if ("gradle".equals(uses)) {
                    gradlePluginCheck(step);
                } else if ("maven".equals(uses)) {
                    mavenPluginCheck(step, dockerInfoService, machineDockerServer, workspaceId);
                } else if ("cache".equals(uses)) {
                    cachePluginCheck(step);
                } else if ("go".equals(uses)) {
                    goPluginCheck(step);
                } else if ("python3".equals(uses)) {
                    python3PluginCheck(step);
                } else {
                    // 其他自定义插件
                    File tmpDir = FileUtil.file(FileUtil.getTmpDir(), "check-users");
                    File pluginInstallResource = null;
                    try {
                        pluginInstallResource = plugin.getResourceToFile("uses/" + uses + "/install.sh", tmpDir);
                        Assert.notNull(pluginInstallResource, "当前还不支持" + uses + "插件");
                    } finally {
                        FileUtil.del(pluginInstallResource);
                    }
                }
                usesSet.add(uses);
            }
        }
        if (usesSet.contains("maven") && !usesSet.contains("java")) {
            throw new IllegalArgumentException("maven 插件依赖 java , 使用 maven 插件必须优先引入 java 插件");
        }
        if (usesSet.contains("gradle") && !usesSet.contains("java")) {
            throw new IllegalArgumentException("gradle 插件依赖 java , 使用 gradle 插件必须优先引入 java 插件");
        }
        Assert.isTrue(containsRun, "steps 中没有发现任何 run , run 用于执行命令");
    }

    private void cachePluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("path"), "cache 插件 path 不能为空");
    }

    /**
     * 检查 maven 插件
     *
     * @param step 参数
     */
    private void mavenPluginCheck(Map<String, Object> step, DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer, String workspaceId) {
        Assert.notNull(step.get("version"), "maven 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/%s/binaries/apache-maven-%s-bin.tar.gz", version, version);
        HttpRequest request = HttpUtil.createRequest(Method.HEAD, link);
        try (HttpResponse httpResponse = request.execute()) {
            boolean success = httpResponse.isOk()
                || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP
                || httpResponse.getStatus() == HttpStatus.HTTP_BAD_METHOD;
            if (success) {
                return;
            }
        }
        // 判断容器中是否存在
        try {
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels =
                dockerInfoService
                    .queryByTag(workspaceId, fromTag);
            Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
            if (map != null) {
                map.put("pluginName", "maven");
                map.put("version", version);
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
                boolean exists = Convert.toBool(plugin.execute("hasDependPlugin", map), false);
                if (exists) {
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("检查 docker 依赖错误:{}", e.getMessage());
        }
        // 提示远程版本
        Collection<String> pluginVersion = this.listMavenPluginVersion();
        throw new IllegalArgumentException("请填入正确的 maven 版本号,可用的版本如下：" + CollUtil.join(pluginVersion, StrUtil.COMMA));
    }


    private Collection<String> listMavenPluginVersion() {
        String html = HttpUtil.get("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/");
        //使用正则获取所有可用版本
        List<String> versions = ReUtil.findAll("<a\\s+href=\"3.*?/\">(.*?)</a>", html, 1);
        Set<String> set = versions.stream()
            .map(s -> StrUtil.removeSuffix(s, StrUtil.SLASH))
            .filter(StrUtil::isNotEmpty)
            .collect(Collectors.toSet());
        Assert.notEmpty(set, "maven 镜像库中没有找到任何可用的 maven 版本");
        return set;
    }

    /**
     * 检查 java 插件
     *
     * @param step 参数
     */
    private void javaPluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "java 插件 version 不能为空");
        Integer version = Integer.valueOf(String.valueOf(step.get("version")));
        List<Integer> supportedVersions = ListUtil.of(8, 11, 17, 18, 21, 22, 23, 24);
        Assert.isTrue(supportedVersions.contains(version), String.format("目前java 插件支持的版本: %s", supportedVersions));
    }


    /**
     * 检查 gradle 插件
     *
     * @param step 参数
     */
    private void gradlePluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "gradle 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://downloads.gradle-dn.com/distributions/gradle-%s-bin.zip", version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() ||
            httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
            httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, "请填入正确的 gradle 版本号");
    }

    /**
     * 检查 node 插件
     *
     * @param step 参数
     */
    private void nodePluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "node 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://registry.npmmirror.com/-/binary/node/v%s/node-v%s-linux-x64.tar.gz", version, version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, "请填入正确的 node 版本号");
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void goPluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "go 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://studygolang.com/dl/golang/go%s.linux-amd64.tar.gz", version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() ||
            httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
            httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, "请填入正确的 go 版本号");
    }

    /**
     * 检查 python3 插件
     *
     * @param step 参数
     */
    private void python3PluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "python3 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        Assert.state(StrUtil.startWith(version, "3."), "请填入正确的 python3 版本号");
        String link = String.format("https://repo.huaweicloud.com/python/%s/Python-%s.tar.xz", version, version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() ||
            httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, "请填入正确的 python3 版本号");
    }

    /**
     * 构建对象
     *
     * @param yml yml 内容
     * @return DockerYmlDsl
     */
    public static DockerYmlDsl build(String yml) {
        yml = StrUtil.replace(yml, StrUtil.TAB, StrUtil.SPACE + StrUtil.SPACE);
        InputStream inputStream = new ByteArrayInputStream(yml.getBytes());
        return YamlUtil.load(inputStream, DockerYmlDsl.class);
    }
}
