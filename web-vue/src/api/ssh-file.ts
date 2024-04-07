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
 * 上传文件到 SSH 节点
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: ssh ID,
 *  name: 当前目录,
 *  path: 父级目录
 * } formData
 */
export function uploadFile(baseUrl, formData) {
  return axios({
    url: baseUrl + 'upload',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    // 0 表示无超时时间
    timeout: 0,
    data: formData
  })
}

/**
 * 授权目录列表
 * @param {String} id
 */
export function getRootFileList(baseUrl, id) {
  return axios({
    url: baseUrl + 'root_file_data.json',
    method: 'post',
    data: { id }
  })
}

/**
 * 文件列表
 * @param {id, path, children} params
 */
export function getFileList(baseUrl, params) {
  return axios({
    url: baseUrl + 'list_file_data.json',
    method: 'post',
    data: params
  })
}

/**
 * 下载文件
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {id, path, name} params
 */
export function downloadFile(baseUrl, params) {
  return loadRouterBase(baseUrl + 'download', params)
}

/**
 * 删除文件
 * @param {id, path, name} params x
 */
export function deleteFile(baseUrl, params) {
  return axios({
    url: baseUrl + 'delete.json',
    method: 'post',
    data: params
  })
}

/**
 * 读取文件
 * @param {id, path, name} params x
 */
export function readFile(baseUrl, params) {
  return axios({
    url: baseUrl + 'read_file_data.json',
    method: 'post',
    data: params
  })
}

/**
 * 保存文件
 * @param {id, path, name,content} params x
 */
export function updateFileData(baseUrl, params) {
  return axios({
    url: baseUrl + 'update_file_data.json',
    method: 'post',
    data: params
  })
}

/**
 * 新增目录  或文件
 * @param params
 * @returns {id, path, name,unFolder} params x
 */
export function newFileFolder(baseUrl, params) {
  return axios({
    url: baseUrl + 'new_file_folder.json',
    method: 'post',
    data: params
  })
}

/**
 * 修改目录或文件名称
 * @param params
 * @returns {id, levelName, filename,newname} params x
 */
export function renameFileFolder(baseUrl, params) {
  return axios({
    url: baseUrl + 'rename.json',
    method: 'post',
    data: params
  })
}

/**
 * 修改文件权限
 * @param {*} baseUrl
 * @param {
 *  String id,
 *  String allowPathParent,
 *  String nextPath,
 *  String fileName,
 *  String permissionValue
 * } params
 * @returns
 */
export function changeFilePermission(baseUrl, params) {
  return axios({
    url: baseUrl + 'change_file_permission.json',
    method: 'post',
    data: params
  })
}

/**
 * 权限字符串转权限对象
 * @param {String} str "lrwxr-xr-x"
 * @returns
 */
export function parsePermissions(str) {
  const permissions = { owner: {}, group: {}, others: {} }

  const chars = str.split('')
  permissions.owner.read = chars[1] === 'r'
  permissions.owner.write = chars[2] === 'w'
  permissions.owner.execute = chars[3] === 'x'

  permissions.group.read = chars[4] === 'r'
  permissions.group.write = chars[5] === 'w'
  permissions.group.execute = chars[6] === 'x'

  permissions.others.read = chars[7] === 'r'
  permissions.others.write = chars[8] === 'w'
  permissions.others.execute = chars[9] === 'x'

  return permissions
}

/**
 * 文件权限字符串转权限值
 * @param {
 *  owner: { read: false, write: false, execute: false, },
 *  group: { read: false, write: false, execute: false, },
 *  others: { read: false, write: false, execute: false, },
 * } permissions
 * @returns
 */
export function calcFilePermissionValue(permissions) {
  let value = 0
  if (permissions.owner.read) {
    value += 400
  }
  if (permissions.owner.write) {
    value += 200
  }
  if (permissions.owner.execute) {
    value += 100
  }
  if (permissions.group.read) {
    value += 40
  }
  if (permissions.group.write) {
    value += 20
  }
  if (permissions.group.execute) {
    value += 10
  }
  if (permissions.others.read) {
    value += 4
  }
  if (permissions.others.write) {
    value += 2
  }
  if (permissions.others.execute) {
    value += 1
  }
  return value
}
