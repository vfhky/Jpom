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

// 日志搜索列表
export function getLogReadList(params) {
  return axios({
    url: '/log-read/list',
    method: 'post',
    data: params
  })
}

/**
 * 编辑日志搜索
 * @param {
 *  id: 监控 ID
 *  name: 监控名称
 *  nodeProject: { nodeId:'',projectId:''}
 *
 * } params
 */
export function editLogRead(params) {
  return axios({
    url: '/log-read/save.json',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

export function updateCache(params) {
  return axios({
    url: '/log-read/update-cache.json',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 删除日志搜索
 * @param {*} id
 */
export function deleteLogRead(id) {
  return axios({
    url: '/log-read/del.json',
    method: 'post',
    data: { id }
  })
}
