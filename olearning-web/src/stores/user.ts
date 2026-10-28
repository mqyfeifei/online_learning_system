import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export interface UserInfo {
  id: number
  /** 学号 / 职工号，登录账号 */
  userNo: string
  /** 用户名（昵称，可改） */
  username: string
  realName: string
  role: string
  avatar?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(
    localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null
  )

  const isLogin = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')

  /** 登录用学号/职工号，不用用户名 */
  async function login(account: string, password: string) {
    const data: any = await request.post('/api/auth/login', { account, password })
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLogin, role, login, logout }
})
