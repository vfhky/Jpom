/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.JpomAppType;
import cn.keepbx.jpom.Type;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Jpom
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Slf4j
@Configuration
@Getter
public class JpomApplication implements DisposableBean, InitializingBean {
    /**
     * 程序端口
     */
    @Value("${server.port}")
    private int port;

    @Value("${server.address:}")
    private String address;
    /**
     * 数据目录缓存大小
     */
    private long dataSizeCache;


    private static volatile JpomApplication jpomApplication;

    private static final Map<String, ExecutorService> LINK_EXECUTOR_SERVICE = new SafeConcurrentHashMap<>();

    /**
     * 单利模式
     *
     * @return config
     */
    public static JpomApplication getInstance() {
        if (jpomApplication == null) {
            synchronized (JpomApplication.class) {
                if (jpomApplication == null) {
                    jpomApplication = SpringUtil.getBean(JpomApplication.class);
                }
            }
        }
        return jpomApplication;
    }

    /**
     * 获取项目运行数据存储文件夹路径
     *
     * @return 文件夹路径
     */
    public String getDataPath() {
        String dataPath = FileUtil.normalize(ExtConfigBean.getPath() + StrUtil.SLASH + Const.DATA);
        FileUtil.mkdir(dataPath);
        return dataPath;
    }

//    /**
//     * 执行脚本
//     *
//     * @param inputStream 脚本内容
//     * @param function    回调分发
//     * @param <T>         值类型
//     * @return 返回值
//     */
//    public <T> T execScript(InputStream inputStream, Function<File, T> function) {
//        String sshExecTemplate = IoUtil.readUtf8(inputStream);
//        return this.execScript(sshExecTemplate, function);
//    }

    /**
     * 执行脚本
     *
     * @param context  脚本内容
     * @param function 回调分发
     * @param <T>      值类型
     * @return 返回值
     */
    public <T> T execScript(String context, Function<File, T> function) {
        String dataPath = this.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        try {
            return function.apply(scriptFile);
        } finally {
            FileUtil.del(scriptFile);
        }
    }

    /**
     * 获取临时文件存储路径
     *
     * @return file
     */
    public File getTempPath() {
        File file = new File(this.getDataPath());
        file = FileUtil.file(file, "temp");
        FileUtil.mkdir(file);
        return file;
    }

    /**
     * 数据目录大小
     *
     * @return byte
     */
    public long dataSize() {
        String dataPath = getDataPath();
        long size = FileUtil.size(FileUtil.file(dataPath));
        dataSizeCache = size;
        return size;
    }

    /**
     * 获取脚本模板路径
     *
     * @return file
     */
    public File getScriptPath() {
        return FileUtil.file(this.getDataPath(), Const.SCRIPT_DIRECTORY);
    }

    /**
     * 获取当前程序的类型
     *
     * @return Agent 或者 Server
     */
    public static Type getAppType() {
        Map<String, Object> beansWithAnnotation = SpringUtil.getApplicationContext().getBeansWithAnnotation(JpomAppType.class);
        Class<?> jpomAppClass = Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到 Jpom 类型配置"));
        JpomAppType jpomAppType = jpomAppClass.getAnnotation(JpomAppType.class);
        return jpomAppType.value();
    }

    public static Class<?> getAppClass() {
        Map<String, Object> beansWithAnnotation = SpringUtil.getApplicationContext().getBeansWithAnnotation(SpringBootApplication.class);
        return Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到运行的主类"));
    }

    /**
     * 重启自身
     * 分发会延迟2秒执行正式升级 重启命令
     *
     * @see JpomManifest#releaseJar
     */
    public static void restart() {
        File runFile = JpomManifest.getRunPath();
        File runPath = runFile.getParentFile();
        if (!runPath.isDirectory()) {
            throw new JpomRuntimeException(runPath.getAbsolutePath() + " error");
        }
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isWindows()) {
            // 需要重新变更 stdout_log 文件来保证进程不被占用
            String format = StrUtil.format("stdout_{}.log", System.currentTimeMillis());
            FileUtil.writeString(format, FileUtil.file(runPath, "run.log"), CharsetUtil.CHARSET_UTF_8);
        }
        File scriptFile = JpomManifest.getScriptFile();
        ThreadUtil.execute(() -> {
            // Waiting for method caller,For example, the interface response
            ThreadUtil.sleep(2, TimeUnit.SECONDS);
            try {
                String command = CommandUtil.generateCommand(scriptFile, "restart upgrade");
                File parentFile = scriptFile.getParentFile();
                if (osInfo.isWindows()) {
                    //String result = CommandUtil.execSystemCommand(command, scriptFile.getParentFile());
                    //log.debug("windows restart {}", result);
                    CommandUtil.asyncExeLocalCommand("start /b" + command, parentFile);
                } else {
                    String jpomService = SystemUtil.get("JPOM_SERVICE");
                    if (StrUtil.isEmpty(jpomService)) {
                        CommandUtil.asyncExeLocalCommand(command, parentFile);
                    } else {
                        // 使用了服务
                        CommandUtil.asyncExeLocalCommand("systemctl restart " + jpomService, parentFile, null, true);
                    }
                }
            } catch (Exception e) {
                log.error("重启自身异常", e);
            }
        });
    }

    public static ScheduledExecutorService getScheduledExecutorService() {
        return (ScheduledExecutorService) LINK_EXECUTOR_SERVICE.computeIfAbsent("jpom-system-task",
            s -> Executors.newScheduledThreadPool(4,
                r -> new Thread(r, "jpom-system-task")));
    }

    /**
     * 注册线程池
     *
     * @param name            线程池名
     * @param executorService 线程池
     */
    public static void register(String name, ExecutorService executorService) {
        LINK_EXECUTOR_SERVICE.put(name, executorService);
    }

    /**
     * 关闭全局线程池
     */
    public static void shutdownGlobalThreadPool() {
        LINK_EXECUTOR_SERVICE.forEach((s, executorService) -> {
            if (!executorService.isShutdown()) {
                log.debug("shutdown {} ThreadPool", s);
                executorService.shutdownNow();
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        Type appType = getAppType();
        log.info("Jpom {} disposable", appType);
        shutdownGlobalThreadPool();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        register("Global", GlobalThreadPool.getExecutor());
    }
}
