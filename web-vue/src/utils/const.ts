///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

// 常量池
export const USER_NAME_KEY = 'Jpom-UserName'

export const TOKEN_KEY = 'Jpom-Token'

export const LONG_TERM_TOKEN = 'Jpom-Long-Term-Token'

export const USER_INFO_KEY = 'Jpom-User'

export const MENU_KEY = 'Jpom-Menus'

export const TOKEN_HEADER_KEY = 'Authorization'

export const ACTIVE_TAB_KEY = 'Jpom-ActiveTab'

export const TAB_LIST_KEY = 'Jpom-TabList'

export const ACTIVE_MENU_KEY = 'Jpom-ActiveMenu'

export const MANAGEMENT_ACTIVE_TAB_KEY = 'Jpom-management-ActiveTab'

export const MANAGEMENT_TAB_LIST_KEY = 'Jpom-management-TabList'

export const MANAGEMENT_ACTIVE_MENU_KEY = 'Jpom-management-ActiveMenu'

// export const GUIDE_FLAG_KEY = "Jpom-GuideFlag";

// export const GUIDE_HOME_USED_KEY = "Jpom-Home-Guide-Used";

// export const GUIDE_NODE_USED_KEY = "Jpom-Node-Guide-Used";

export const NO_NOTIFY_KEY = 'tip'

export const NO_LOADING_KEY = 'loading'

const cachePageLimitKeyName = 'page_limit'
import dayjs from 'dayjs'
export function getCachePageLimit(): number {
  return parseInt(localStorage.getItem(cachePageLimitKeyName) || '10')
}

/**
 * 分页选择条
 */
export const PAGE_DEFAULT_SIZW_OPTIONS = ['5', '10', '15', '20', '25', '30', '35', '40', '50']

/**
 * 展示总条数计算方法
 * @param {Number} total 总记录数
 * @returns String
 */
export function PAGE_DEFAULT_SHOW_TOTAL(total: number) {
  return `总计 ${total} 条`
}

export const PAGE_DEFAULT_LIST_QUERY: any = {
  page: 1,
  limit: isNaN(getCachePageLimit()) ? 10 : getCachePageLimit(),
  total: 0
}

/**
 * 计算分页数据
 * @param {JSON} queryParam 分页参数
 * @param {Array} pageSizeOptions 分页选择条选项
 * @returns
 */
export function COMPUTED_PAGINATION(queryParam: any, pageSizeOptions = PAGE_DEFAULT_SIZW_OPTIONS) {
  // console.log(queryParam)
  const limit = queryParam.limit || PAGE_DEFAULT_LIST_QUERY.limit
  const total = queryParam.total || 0
  return {
    total: total,
    current: queryParam.page || 1,
    pageSize: limit,
    pageSizeOptions: pageSizeOptions || PAGE_DEFAULT_SIZW_OPTIONS,
    showSizeChanger: true,
    showQuickJumper: true,
    showLessItems: true,
    // 只有在分页条数在 小于 2 的时候隐藏，避免设置太大无法切回
    hideOnSinglePage: limit <= 20,
    showTotal: (total: number) => {
      return PAGE_DEFAULT_SHOW_TOTAL(total)
    }
  }
}

/**
 * 分页切换
 * @param {JSON} listQuery
 * @param {JSON} param1
 * @returns
 */
export function CHANGE_PAGE(listQuery, { pagination, sorter }) {
  if (pagination && Object.keys(pagination).length) {
    let limit = pagination.pageSize || pagination.limit || listQuery.limit
    if (limit === -1) {
      limit = getCachePageLimit()
    }
    listQuery = {
      ...listQuery,
      page: pagination.current || listQuery.page,
      limit: limit
    }

    //
    localStorage.setItem(cachePageLimitKeyName, limit)
    //
    PAGE_DEFAULT_LIST_QUERY.limit = limit
  }
  if (sorter && Object.keys(sorter).length) {
    listQuery = { ...listQuery, order: sorter.order, order_field: sorter.field }
  }
  return listQuery
}

/**
 * 缓存当前的工作空间 ID
 */
export const CACHE_WORKSPACE_ID = 'workspaceId'

/**
 * 升级 重启检查等待次数
 */
export const RESTART_UPGRADE_WAIT_TIME_COUNT = 80

/**
 * 定时 cron 默认提示
 *
 * https://www.npmjs.com/package/cron-parser
 */
export const CRON_DATA_SOURCE = [
  {
    title: '取消定时,不再定时执行（支持 ! 前缀禁用定时执行，如：!0 0/1 * * * ?）',
    options: [
      {
        title: '',
        value: ''
      }
    ]
  },
  {
    title: '分钟级别',
    options: [
      {
        title: '1分钟',
        value: '0 0/1 * * * ?'
      },
      {
        title: '5分钟',
        value: '0 0/5 * * * ?'
      },
      {
        title: '10分钟',
        value: '0 0/10 * * * ?'
      },
      {
        title: '30分钟',
        value: '0 0/30 * * * ?'
      }
    ]
  },
  {
    title: '小时级别',
    options: [
      {
        title: '每小时',
        value: '0 0 0/1 * * ?'
      }
    ]
  },
  {
    title: '天级别',
    options: [
      {
        title: '凌晨0点和中午12点',
        value: '0 0 0,12 * * ?'
      },
      {
        title: '凌晨0点',
        value: '0 0 0 * * ?'
      }
    ]
  },
  {
    title: '秒级别（默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）',
    options: [
      {
        title: '5秒一次',
        value: '0/5 * * * * ?'
      },
      {
        title: '10秒一次',
        value: '0/10 * * * * ?'
      },
      {
        title: '30秒一次',
        value: '0/30 * * * * ?'
      }
    ]
  }
]

/**
 * 压缩文件格式
 */
export const ZIP_ACCEPT = '.tar,.bz2,.gz,.zip,.tar.bz2,.tar.gz'

/**
 * mfa app 应用举例
 */
export const MFA_APP_TIP_ARRAY = [
  '<strong>【推荐】微信小程序搜索 数盾OTP</strong>',
  '<strong>【推荐】腾讯身份验证码</strong> 简单好用 <a href="https://a.app.qq.com/o/simple.jsp?pkgname=com.tencent.authenticator">Android</a>',
  '<strong>Authy</strong> 功能丰富 专为两步验证码 <a href="https://authy.com/download/">iOS/Android/Windows/Mac/Linux</a> &nbsp; <a href="https://chrome.google.com/webstore/detail/authy/gaedmjdfmmahhbjefcbgaolhhanlaolb?hl=cn">Chrome 扩展</a>',
  '<strong>Google Authenticator</strong> 简单易用，但不支持密钥导出备份 <a href="https://apps.apple.com/us/app/google-authenticator/id388497605">iOS</a> <a href="https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&amp;hl=cn">Android</a>',
  '<strong>Microsoft Authenticator</strong> 使用微软全家桶的推荐 <a href="https://www.microsoft.com/zh-cn/account/authenticator">iOS/Android</a>',
  '<strong>1Password</strong> 强大安全的密码管理付费应用<a href="https://1password.com/zh-cn/downloads/">iOS/Android/Windows/Mac/Linux/ChromeOS</a>'
]

/**
 * 项目 DSL 示例
 */
export const PROJECT_DSL_DEFATUL =
  '# scriptId 可以是项目路径下脚本文件名或者系统中的脚本模版ID\r\n' +
  'description: 测试\r\n' +
  'run:\r\n' +
  '  start:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: start\r\n' +
  '    scriptEnv:\r\n' +
  '      "boot_active": test\r\n' +
  '  status:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: status\r\n' +
  '  stop:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: stop\r\n' +
  '#  restart:\r\n' +
  '##    scriptId: project.sh\r\n' +
  '#    scriptId: \r\n' +
  '#    scriptArgs: restart\r\n' +
  '#    scriptEnv:\r\n' +
  '#      "boot_active": test\r\n' +
  '#  reload:\r\n' +
  '##    scriptId: project.sh\r\n' +
  '#    scriptId: \r\n' +
  '#    scriptArgs: reload\r\n' +
  '#    scriptEnv:\r\n' +
  '#      "boot_active": test\r\n' +
  '#  fileChangeReload: true\r\n' +
  '#  在指定目录执行: ./ 项目目录  /root/ 特定目录 默认在 ${jpom_agent_data_path}/script_run_cache \r\n' +
  '#  execPath: ./\r\n' +
  'file:\r\n' +
  '# 备份文件保留个数\r\n' +
  '#  backupCount: 5\r\n' +
  '# 限制备份指定文件后缀（支持正则）\r\n' +
  "#  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]\r\n" +
  '# 项目文件备份路径\r\n' +
  '#  backupPath: /data/jpom_backup\r\n' +
  'config:\r\n' +
  '# 是否开启日志备份功能\r\n' +
  '#  autoBackToFile: true\r\n' +
  '\r\n'

/**
 * 并发执行
 * @params list {Array} - 要迭代的数组
 * @params limit {Number} - 并发数量控制数,最好小于3
 * @params asyncHandle {Function} - 对`list`的每一个项的处理函数，参数为当前处理项，必须 return 一个Promise来确定是否继续进行迭代
 * @return {Promise} - 返回一个 Promise 值来确认所有数据是否迭代完成
 */
export function concurrentExecution(list, limit, asyncHandle) {
  // 递归执行
  const recursion = (arr) => {
    // 执行方法 arr.shift() 取出并移除第一个数据
    return asyncHandle(arr.shift()).then((res) => {
      // 数组还未迭代完，递归继续进行迭代
      if (arr.length !== 0) {
        return recursion(arr)
      } else {
        return res
      }
    })
  }
  // 创建新的并发数组
  const listCopy = [].concat(list)
  // 正在进行的所有并发异步操作
  const asyncList = []
  limit = limit > listCopy.length ? listCopy.length : limit

  while (limit--) {
    asyncList.push(recursion(listCopy))
  }
  // 所有并发异步操作都完成后，本次并发控制迭代完成
  return Promise.all(asyncList)
}

/**
 * 并发执行任务
 * @param list 任务列表
 * @param limit 并发控制
 * @param asyncHandle 任务处理函数
 */
export async function concurrentJobs(list, limit, asyncHandle) {
  const arr = [...list]
  const result = []
  for (let i = 0; i < arr.length; i += limit) {
    result.push(...(await Promise.allSettled(arr.slice(i, i + limit).map(asyncHandle))))
  }
  return result
}

export function readJsonStrField(json, key) {
  try {
    const data = JSON.parse(json)[key] || ''
    if (Object.prototype.toString.call(data) === '[object Object]') {
      return JSON.stringify(data)
    }
    return data
  } catch (e) {
    //
  }
  return ''
}

export function randomStr(len = 2) {
  const $chars = 'ABCDEFGHJKMNPQRSTWXYZ0123456789'
  /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
  const maxPos = $chars.length
  let repliccaId = ''
  for (let i = 0; i < len; i++) {
    repliccaId += $chars.charAt(Math.floor(Math.random() * maxPos))
  }
  return repliccaId
}

/**
 * 转换时间函数
 * @param {*} time
 * @param {*} cFormat
 */
export function parseTime(time: any, cFormat = 'YYYY-MM-DD HH:mm:ss') {
  if (arguments.length === 0) {
    return '-'
  }
  if (!time) {
    return '-'
  }
  // 处理 time 参数
  if (isNaN(Number(time)) === false) {
    time = Number(time)
  }
  const format = cFormat || 'YYYY-MM-DD HH:mm:ss'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if (('' + time).length === 10) time = parseInt(time) * 1000
    date = new Date(time)
  }

  return dayjs(date).format(format)
}

/**
 * 格式化文件大小
 * @param {*} value
 * @param defaultValue
 * @returns
 */
export function renderSize(value, defaultValue = '-') {
  return formatUnits(value, 1024, ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'], defaultValue)
}

/**
 * 格式化网络大小
 * @param {*} value
 * @param defaultValue
 * @returns
 */
export function renderBpsSize(value, defaultValue = '-') {
  return formatUnits(value, 1024, ['bps', 'Kbps', 'Mbps', 'Gbps', 'Tbps', 'Pbps', 'Ebps', 'Zbps', 'Ybps'], defaultValue)
}

/**
 * 格式化文件大小
 * @param {*} value
 * @param defaultValue
 * @returns
 */
export function formatUnits(value, base, unitArr, defaultValue = '-') {
  if (null == value || value === '') {
    return defaultValue
  }

  let index = 0
  const srcsize = parseFloat(value)
  if (srcsize <= 0) {
    return defaultValue
  }
  // console.log(value, srcsize);
  index = Math.floor(Math.log(srcsize) / Math.log(base))
  let size = srcsize / Math.pow(base, index)
  size = size.toFixed(2) //保留的小数位数
  return size + unitArr[index]
}

/**
 * 数组分组  [{id:1,value:1},{id:2,value:3}] => {1:{id:1,value:1},2:{id:2,value:3}}
 * @param {function} group
 * @returns Object
 */
Array.prototype.groupBy = function (group) {
  return group && typeof group === 'function'
    ? Array.prototype.reduce.call(
        this,
        function (c, v) {
          const k = group(v)
          c[k] = v
          return c
        },
        {}
      )
    : this
}
//
export function itemGroupBy(arr, groupKey, key, dataKey) {
  key = key || 'type'
  dataKey = dataKey || 'data'

  const newArr = [],
    types = {}
  let i, j, cur
  for (i = 0, j = arr.length; i < j; i++) {
    cur = arr[i]
    if (!(cur[groupKey] in types)) {
      types[cur[groupKey]] = { [key]: cur[groupKey], [dataKey]: [] }
      newArr.push(types[cur[groupKey]])
    }
    types[cur[groupKey]][dataKey].push(cur)
  }
  return newArr
}

/**
 * 格式化时长(毫秒)
 * @param {String} ms
 * @param {String} seg 分割符
 * @param {String} levelCount 格式化个数
 * @returns
 */
export function formatDuration(ms: any, seg: string = '', levelCount: number = 5) {
  let msNum = Number(ms)
  if (isNaN(msNum)) {
    return ms
  }
  if (msNum === 0) {
    return '-'
  }

  seg = seg || ''
  levelCount = levelCount || 5
  if (msNum < 0) msNum = -msNum
  const time = {
    天: Math.floor(msNum / 86400000),
    小时: Math.floor(msNum / 3600000) % 24,
    分钟: Math.floor(msNum / 60000) % 60,
    秒: Math.floor(msNum / 1000) % 60,
    毫秒: Math.floor(msNum) % 1000
  }
  return Object.entries(time)
    .filter((val) => val[1] !== 0)
    .map(([key, val]) => `${val}${key}`)
    .splice(0, levelCount)
    .join(seg)
}

//小数转换为分数(小数先转换成number类型，再乘以100，并且保留2位小数)
export function formatPercent(point, keep = 2) {
  if (!point) {
    return '-'
  }
  return formatPercent2(Number(point) * 100, keep)
}

//小数转换为分数(小数先转换成number类型，并且保留2位小数)
export function formatPercent2(point, keep = 2) {
  if (null == point) {
    return '-'
  }
  let percent = Number(Number(point).toFixed(keep))
  percent += '%'
  return percent
}

//小数转换为分数(小数先转换成number类型，再乘以100，并且保留2位小数)
export function formatPercent2Number(point: any, keep = 2) {
  if (null == point) {
    return 0
  }
  return Number(Number(point).toFixed(keep))
}

export function compareVersion(version1, version2) {
  if (version1 == null && version2 == null) {
    return 0
  } else if (version1 == null) {
    // null视为最小版本，排在前
    return -1
  } else if (version2 == null) {
    return 1
  }

  if (version1 === version2) {
    return 0
  }

  const v1s = version1.split('.')
  const v2s = version2.split('.')

  let diff = 0
  const minLength = Math.min(v1s.length, v2s.length) // 取最小长度值

  for (let i = 0; i < minLength; i++) {
    const v1 = v1s[i]
    const v2 = v2s[i]
    // 先比较长度
    diff = v1.length - v2.length
    if (0 === diff) {
      diff = v1.localeCompare(v2)
    }
    if (diff !== 0) {
      //已有结果，结束
      break
    }
  }

  // 如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
  return diff !== 0 ? diff : v1s.length - v2s.length
}

// 当前页面构建信息
export function pageBuildInfo() {
  const htmlVersion = document.head.querySelector('[name~=jpom-version][content]').content
  const buildTime = document.head.querySelector('[name~=build-time][content]').content
  const buildEnv = document.head.querySelector('[name~=build-env][content]').content
  return {
    v: htmlVersion,
    t: buildTime,
    e: buildEnv,
    df: (document.title || '').toLowerCase().includes('jpom'),
    t2: Date.now()
  }
}

/**
 * 拖拽数据处理 - vue3-smooth-dnd
 * @param arr 原数组
 * @param dragResult onDrop 结果集
 * @returns
 */
export const dropApplyDrag = <T = any>(
  arr: T[],
  dragResult: { removedIndex: number; addedIndex: number; payload: T }
) => {
  const { removedIndex, addedIndex, payload } = dragResult
  if (removedIndex === null && addedIndex === null) return arr
  const result = [...arr]
  let itemToAdd = payload
  if (removedIndex !== null) {
    itemToAdd = result.splice(removedIndex, 1)[0]
  }
  if (addedIndex !== null) {
    result.splice(addedIndex, 0, itemToAdd)
  }
  return result
}
