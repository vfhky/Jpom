///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

/**
 * 应用工作空间相关
 */
import { CACHE_WORKSPACE_ID } from '@/utils/const'
import { getHashQuery } from '@/utils/utils'
import { RouteLocationNormalized } from 'vue-router'
import { executionRequest } from '@/api/external'
import { parseTime, pageBuildInfo } from '@/utils/const'

export const useAppStore = defineStore('app', {
  state: () => ({
    workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
    // 菜单折叠
    isCollapsed: localStorage.getItem('collapsed') === 'true',
    isShowInfo: false,
    loading: 0
  }),

  actions: {
    // 页面加载（路由切换）
    pageLoading(loading: boolean) {
      this.loading = loading ? 1 : 2
    },
    // 切换工作空间
    changeWorkspace(workspaceId: string) {
      return new Promise((resolve) => {
        this.workspaceId = workspaceId
        localStorage.setItem(CACHE_WORKSPACE_ID, workspaceId)
        resolve(true)
      })
    },
    collapsed(isCollapsed: boolean) {
      this.isCollapsed = isCollapsed
      localStorage.setItem('collapsed', String(isCollapsed))
    },
    showInfo(to: RouteLocationNormalized) {
      if (this.isShowInfo) {
        return
      }
      this.isShowInfo = true
      // 控制台输出版本号信息
      const buildInfo = pageBuildInfo()
      executionRequest('https://jpom.top/docs/versions.show', { ...buildInfo, p: to.path })
        .then((data) => {
          console.log(
            '\n %c ' + parseTime(buildInfo.t) + ' %c vs %c ' + buildInfo.v + ' %c vs %c ' + data,
            'color: #ffffff; background: #f1404b; padding:5px 0;',
            'background: #1890ff; padding:5px 0;',
            'color: #ffffff; background: #f1404b; padding:5px 0;',
            'background: #1890ff; padding:5px 0;',
            'color: #ffffff; background: #f1404b; padding:5px 0;'
          )
        })
        .catch(() => {
          // 解锁
          this.isShowInfo = false
        })
    }
  },
  getters: {
    getWorkspaceId: (state) => {
      return () => {
        const query = getHashQuery()
        return query.wid || state.workspaceId
      }
    },
    getCollapsed(state) {
      return state.isCollapsed
    }
  }
})

// export default useAppStore()
