/**
 * 认证相关 API
 * 模拟后端接口
 */

/**
 * 登录接口
 * @param {string} account - 账号（手机号/昵称）
 * @param {string} password - 密码
 * @returns {Promise} 返回登录结果
 */
export const login = async (account, password) => {
  // 模拟网络延迟
  await new Promise((resolve) => setTimeout(resolve, 600))

  // 模拟后端校验
  if (!account || !password) {
    return {
      code: 400,
      message: '账号和密码不能为空',
    }
  }

  // 模拟登录成功（任何账号密码都能登录）
  return {
    code: 200,
    data: {
      token_name: 'token',
      token_value: 'mock_token_' + Date.now(),
      user_info: {
        phone: account,
        nickname: account,
      },
    },
  }
}

/**
 * 注册接口
 * @param {string} phone - 手机号
 * @param {string} password - 密码
 * @param {string} nickname - 昵称（选填）
 * @returns {Promise} 返回注册结果
 */
export const register = async (phone, password, nickname = '') => {
  await new Promise((resolve) => setTimeout(resolve, 600))

  if (!phone) {
    return {
      code: 400,
      message: '手机号不能为空',
    }
  }

  if (password.length < 6) {
    return {
      code: 400,
      message: '密码至少6位',
    }
  }

  return {
    code: 200,
    data: {
      token_name: 'token',
      token_value: 'mock_token_' + Date.now(),
      user_info: {
        phone: phone,
        nickname: nickname || '银龄用户',
      },
    },
  }
}

/**
 * 退出登录接口
 * @returns {Promise} 返回退出结果
 */
export const logout = async () => {
  await new Promise((resolve) => setTimeout(resolve, 300))
  return {
    code: 200,
    message: '退出成功',
  }
}

/**
 * 获取用户信息接口
 * @returns {Promise} 返回用户信息
 */
export const getUserInfo = async () => {
  await new Promise((resolve) => setTimeout(resolve, 300))
  return {
    code: 200,
    data: {
      phone: '138****8888',
      nickname: '银龄用户',
    },
  }
}