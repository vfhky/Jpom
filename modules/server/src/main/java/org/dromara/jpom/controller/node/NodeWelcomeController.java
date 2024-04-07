/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.model.MachineNodeStatLogModel;
import org.dromara.jpom.func.assets.server.MachineNodeStatLogServer;
import org.dromara.jpom.model.BaseMachineModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * 节点统计信息
 *
 * @author bwcx_jzy
 */
@RestController
@RequestMapping(value = "/node")
public class NodeWelcomeController extends BaseServerController {

    private final MachineNodeStatLogServer machineNodeStatLogServer;
    private final NodeConfig nodeConfig;

    public NodeWelcomeController(MachineNodeStatLogServer machineNodeStatLogServer,
                                 ServerConfig serverConfig) {
        this.machineNodeStatLogServer = machineNodeStatLogServer;
        this.nodeConfig = serverConfig.getNode();
    }

    @PostMapping(value = "node_monitor_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<MachineNodeStatLogModel>> nodeMonitorJson(String machineId) {
        NodeModel node = tryGetNode();
        List<MachineNodeStatLogModel> list = this.getList(node, machineId);
        return JsonMessage.success("", list);
    }

    private List<MachineNodeStatLogModel> getList(NodeModel node, String machineId) {
        String useMachineId = Optional.ofNullable(node).map(BaseMachineModel::getMachineId).orElse(machineId);
        String startDateStr = getParameter("startTime");
        String endDateStr = getParameter("endTime");
        if (StrUtil.hasEmpty(startDateStr, endDateStr)) {
            MachineNodeStatLogModel systemMonitorLog = new MachineNodeStatLogModel();
            systemMonitorLog.setMachineId(useMachineId);
            return machineNodeStatLogServer.queryList(systemMonitorLog, 500, new Order("monitorTime", Direction.DESC));
        }
        //  处理时间
        DateTime startDate = DateUtil.parse(startDateStr);
        long startTime = startDate.getTime();
        DateTime endDate = DateUtil.parse(endDateStr);
        if (startDate.equals(endDate)) {
            // 时间相等
            endDate = DateUtil.endOfDay(endDate);
        }
        long endTime = endDate.getTime();
        // 开启了节点信息采集
        Page pageObj = new Page(1, 5000);
        pageObj.addOrder(new Order("monitorTime", Direction.DESC));
        Entity entity = Entity.create();
        entity.set("machineId", useMachineId);
        entity.set(" MONITORTIME", ">= " + startTime);
        entity.set("MONITORTIME", "<= " + endTime);
        return machineNodeStatLogServer.listPageOnlyResult(entity, pageObj);
    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> getProcessList(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.ProcessList);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.ProcessList);
    }

    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public IJsonMessage<String> kill(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.Kill);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.Kill);
    }

    @GetMapping(value = "machine-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> machineInfo(String machineId) {
        NodeModel nodeModel = tryGetNode();
        String useMachineId = Optional.ofNullable(nodeModel).map(BaseMachineModel::getMachineId).orElse(machineId);
        MachineNodeModel model = machineNodeServer.getByKey(useMachineId);
        Assert.notNull(model, "没有找到对应的机器");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", model);
        jsonObject.put("heartSecond", nodeConfig.getHeartSecond());
        return JsonMessage.success("", jsonObject);
    }

    @GetMapping(value = "disk-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> diskInfo(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.DiskInfo);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.DiskInfo);
    }

    @GetMapping(value = "hw-disk-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> hwDiskInfo(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.HwDiskInfo);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.HwDiskInfo);
    }

    @GetMapping(value = "network-interfaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> networkInterfaces(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.NetworkInterfaces);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.NetworkInterfaces);
    }
}
