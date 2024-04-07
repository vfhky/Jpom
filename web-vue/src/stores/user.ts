///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { TOKEN_KEY, USER_INFO_KEY, LONG_TERM_TOKEN } from '@/utils/const'

import { getUserInfo, loginOut } from '@/api/user/user'
import { useAllMenuStore } from './menu2'

export const useUserStore = defineStore('user', {
  state: () => ({
    //token: localStorage.getItem(TOKEN_KEY) || '',
    // longTermToken: localStorage.getItem(LONG_TERM_TOKEN) || '',
    userInfo: localStorage.getItem(USER_INFO_KEY) ? JSON.parse(localStorage.getItem(USER_INFO_KEY)!) : {},
    reloadUserInfo: false
  }),
  actions: {
    async refreshUserInfo() {
      return await getUserInfo().then((res) => {
        if (res.code === 200) {
          this.userInfo = res.data
          localStorage.setItem(USER_INFO_KEY, JSON.stringify(res.data))
          return res.data
        }
        return Promise.reject()
      })
    },
    // 页面刷新 加载用户信息
    pageReloadRefreshUserInfo() {
      if (this.reloadUserInfo) {
        return
      }
      this.refreshUserInfo().then(() => {
        this.reloadUserInfo = true
      })
    },
    // 登录 data = {token: 'xxx', userName: 'name'}
    async login(data: any) {
      const token = data.token || ''
      const longTermToken = data.longTermToken || ''
      if (token) {
        localStorage.setItem(TOKEN_KEY, data.token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
      if (longTermToken) {
        localStorage.setItem(LONG_TERM_TOKEN, data.longTermToken)
      } else {
        localStorage.removeItem(LONG_TERM_TOKEN)
      }

      return true
    },
    // 退出登录 移除对应的 store
    async logOut() {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(LONG_TERM_TOKEN)
      const menuStore = useAllMenuStore()
      // 调用其他 action
      menuStore.clearTabs('normal', { key: 'all' })
      menuStore.clearTabs('management', { key: 'all' })
      menuStore.clearMenus('normal')
      menuStore.clearMenus('management')
      // 重置，避免信息没有刷新
      this.reloadUserInfo = false
      //
      return loginOut({})
    }
  },
  getters: {
    getToken() {
      return () => {
        return localStorage.getItem(TOKEN_KEY) || ''
      }
    },
    getLongTermToken() {
      return () => {
        return localStorage.getItem(LONG_TERM_TOKEN) || ''
      }
    },
    getUserInfo(state) {
      return state.userInfo
    }
  }
})

// export default useUserStore()
