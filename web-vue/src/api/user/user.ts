///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import axios from '../config'

// login
export function login(params) {
  return axios({
    url: '/userLogin',
    method: 'post',
    data: params
  })
}

// oauth2Login
export function oauth2Login(params) {
  return axios({
    url: '/oauth2/login',
    method: 'post',
    data: params
  })
}

export function oauth2Url(params) {
  return axios({
    url: '/oauth2-url',
    method: 'get',
    params: params
  })
}

/**
 * 验证输入的验证码
 * @param {JSON} params
 * @returns
 */
export function mfaVerify(params) {
  return axios({
    url: '/mfa_verify',
    method: 'get',
    params: params
  })
}

// refresh token
export function refreshToken() {
  return axios({
    url: '/renewal',
    method: 'post'
  })
}

// 关闭 两步验证信息
export function closeMfa(params) {
  return axios({
    url: '/user/close_mfa',
    method: 'get',
    params
  })
}

// 生成 两步验证信息
export function generateMfa() {
  return axios({
    url: '/user/generate_mfa',
    method: 'get'
  })
}

/**
 *  绑定 mfa
 * @param {JSON} params
 * @returns
 */
export function bindMfa(params) {
  return axios({
    url: '/user/bind_mfa',
    method: 'get',
    params: params
  })
}

// 获取用户信息
export function getUserInfo() {
  return axios({
    url: '/user/user-basic-info',
    method: 'post'
  })
}

// 退出登录
export function loginOut(params) {
  return axios({
    url: '/logout2',
    method: 'get',
    data: params
  })
}

// 修改密码
export function updatePwd(params) {
  return axios({
    url: '/user/updatePwd',
    method: 'post',
    data: params
  })
}

// 所有管理员列表
export function getUserListAll() {
  return axios({
    url: '/user/get_user_list_all',
    method: 'post'
  })
}

// 用户列表
export function getUserList(params) {
  return axios({
    url: '/user/get_user_list',
    method: 'post',
    data: params
  })
}

// 编辑
export function editUser(params) {
  return axios({
    url: '/user/edit',
    method: 'post',
    data: params
  })
}

// // 修改用户
// export function updateUser(params) {
//   return axios({
//     url: '/user/updateUser',
//     method: 'post',
//     data: params
//   })
// }

// 删除用户
export function deleteUser(id) {
  return axios({
    url: '/user/deleteUser',
    method: 'post',
    data: { id }
  })
}

/**
 * 编辑用户资料
 * @param {
 *  token: token,
 *  email: 邮箱地址,
 *  code: 邮箱验证码
 *  dingDing: 钉钉群通知地址,
 *  workWx: 企业微信群通知地址
 * } params
 */
export function editUserInfo(params) {
  return axios({
    url: '/user/save_basicInfo.json',
    method: 'post',
    data: params
  })
}

/**
 * 发送邮箱验证码
 * @param {String} email 邮箱地址
 */
export function sendEmailCode(email) {
  return axios({
    url: '/user/sendCode.json',
    method: 'post',
    timeout: 0,
    data: { email }
  })
}

/**
 * 解锁管理员
 * @param {String} id 管理员 ID
 * @returns
 */
export function unlockUser(id) {
  return axios({
    url: '/user/unlock',
    method: 'get',
    params: { id }
  })
}

/**
 * 关闭用户 mfa 两步验证
 * @param {String} id 管理员 ID
 * @returns
 */
export function closeUserMfa(id) {
  return axios({
    url: '/user/close_user_mfa',
    method: 'get',
    params: { id }
  })
}

/**
 * 重置用户密码
 * @param {String} id 管理员 ID
 * @returns
 */
export function restUserPwd(id) {
  return axios({
    url: '/user/rest-user-pwd',
    method: 'get',
    params: { id }
  })
}

/**
 * 用户的工作空间列表
 * @param {String} userId 管理员 ID
 * @returns
 */
export function workspaceList(userId) {
  return axios({
    url: '/user/workspace_list',
    method: 'get',
    params: { userId: userId }
  })
}

/**
 * 我的工作空间
 *
 * @returns
 */
export function myWorkspace() {
  return axios({
    url: '/user/my-workspace',
    method: 'get',
    params: {}
  })
}

export function statWorkspace() {
  return axios({
    url: '/stat/workspace',
    method: 'get',
    params: {}
  })
}

export function statSystemOverview() {
  return axios({
    url: '/stat/system',
    method: 'get',
    params: {}
  })
}

/**
 * 我的集群
 *
 * @returns
 */
export function clusterList() {
  return axios({
    url: '/user/cluster-list',
    method: 'get',
    params: {}
  })
}

/**
 * 保存我的工作空间
 *
 * @returns
 */
export function saveWorkspace(data) {
  return axios({
    url: '/user/save-workspace',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 登录页面 信息
 *
 * @returns
 */
export function loginConfig() {
  return axios({
    url: '/login-config',
    method: 'get',
    params: {}
  })
}

/**
 * 登录验证码
 *
 * @returns
 */
export function loginRandCode(params) {
  return axios({
    url: '/rand-code',
    method: 'get',
    params: params
  })
}

export function listLoginLog(params) {
  return axios({
    url: '/user/list-login-log-data',
    method: 'post',
    data: params
  })
}

export function listOperaterLog(params) {
  return axios({
    url: '/user/list-operate-log-data',
    method: 'post',
    data: params
  })
}

export function recentLogData(params) {
  return axios({
    url: '/user/recent-log-data',
    method: 'post',
    data: params
  })
}
