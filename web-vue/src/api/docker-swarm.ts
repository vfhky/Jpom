///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import axios from './config'
import { loadRouterBase } from './config'
/**
 * 容器列表
 * @param {JSON} params
 */
export function dockerSwarmList(params) {
  return axios({
    url: '/docker/swarm/list',
    method: 'post',
    data: params
  })
}

export function dockerSwarmListAll(params) {
  return axios({
    url: '/docker/swarm/list-all',
    method: 'get',
    params: params
  })
}

export function editDockerSwarm(data) {
  return axios({
    url: '/docker/swarm/edit',
    method: 'post',
    data: data
  })
}

/**
 * 删除 集群
 * @param {
 *  id: docker ID
 * } params
 */
export function delSwarm(params) {
  return axios({
    url: '/docker/swarm/del',
    method: 'get',
    params
  })
}

/**
 * 容器集群节点列表
 * @param {JSON} params
 */
export function dockerSwarmNodeList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm/node-list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器集群节点修改
 * @param {JSON} params
 */
export function dockerSwarmNodeUpdate(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm/update',
    method: 'post',
    data: params
  })
}

/**
 * 容器集群服务列表
 * @param {JSON} params
 */
export function dockerSwarmServicesList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器集群服务任务列表
 * @param {JSON} params
 */
export function dockerSwarmServicesTaskList(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/task-list',
    method: 'post',
    data: params,
    headers: {
      loading: 'no'
    }
  })
}

/**
 * 容器集群节点 删除服务
 * @param {JSON} params
 */
export function dockerSwarmServicesDel(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/del',
    method: 'get',
    params: params
  })
}

/**
 * 容器集群节点 删除服务
 * @param {JSON} params
 */
export function dockerSwarmServicesEdit(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/edit',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 开始拉取服务日志
 * @param {JSON} params
 */
export function dockerSwarmServicesStartLog(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/start-log',
    method: 'get',
    params: params
  })
}

/**
 * 拉取服务日志
 * @param {JSON} params
 */
export function dockerSwarmServicesPullLog(urlPrefix, params) {
  return axios({
    url: urlPrefix + '/docker/swarm-service/pull-log',
    method: 'get',
    params: params,
    headers: {
      loading: 'no'
    }
  })
}

export function dockerSwarmServicesDownloaLog(urlPrefix, id) {
  return loadRouterBase(urlPrefix + '/docker/swarm-service/download-log', {
    id: id
  })
}

/**
 * <!-- Note: detail description about taskState, please @see https://docs.docker.com/engine/swarm/how-swarm-mode-works/swarm-task-states/ -->
          <!-- reference Java class: com.github.dockerjava.api.model.TaskState -->

            <!-- NEW: The task was initialized. -->
            <a-select-option key="NEW">新建状态</a-select-option>
            <a-select-option key="ALLOCATED">已分配</a-select-option>
            <!-- PENDING: Resources for the task were allocated. -->
            <a-select-option key="PENDING">待处理</a-select-option>
            <!-- ASSIGNED: Docker assigned the task to nodes. -->
            <a-select-option key="ASSIGNED">已分配</a-select-option>
            <!-- ACCEPTED: The task was accepted by a worker node. If a worker node rejects the task, the state changes to REJECTED. -->
            <a-select-option key="ACCEPTED">处理中</a-select-option>
            <!-- PREPARING: Docker is preparing the task. -->
            <a-select-option key="PREPARING">准备中</a-select-option>
            <a-select-option key="READY">准备</a-select-option>
            <!-- STARTING: Docker is starting the task. -->
            <a-select-option key="STARTING">开始执行任务</a-select-option>
            <!-- RUNNING: The task is executing. -->
            <a-select-option key="RUNNING">执行任务中</a-select-option>
            <!-- COMPLETE: The task exited without an error code. -->
            <a-select-option key="COMPLETE">执行成功</a-select-option>
            <!-- SHUTDOWN: Docker requested the task to shut down. -->
            <a-select-option key="SHUTDOWN">停止</a-select-option>
            <!-- FAILED: The task exited with an error code. -->
            <a-select-option key="FAILED">执行失败</a-select-option>
            <!-- REJECTED: The worker node rejected the task. -->
            <a-select-option key="REJECTED">拒绝</a-select-option>
            <!-- REMOVE: The task is not terminal but the associated service was removed or scaled down. -->
            <a-select-option key="REMOVE">移除</a-select-option>
            <!-- ORPHANED: The node was down for too long. -->
            <a-select-option key="ORPHANED">已失联</a-select-option>
 */
export const TASK_STATE = {
  NEW: '新建状态',
  // ALLOCATED: "已分配",
  PENDING: '待处理',
  ASSIGNED: '已分配',
  ACCEPTED: '处理中',
  PREPARING: '准备中',
  READY: '准备',
  STARTING: '开始执行任务',
  RUNNING: '执行任务中',
  COMPLETE: '执行成功',
  SHUTDOWN: '停止',
  FAILED: '执行失败',
  REJECTED: '拒绝',
  REMOVE: '移除',
  ORPHANED: '已失联'
}
