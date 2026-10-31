/* 校园二手交易平台 - 前端接口封装 */
(function (global) {
  const TOKEN_KEY = 'campus_token';
  const USER_KEY = 'campus_user';

  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || '';
  }

  function getUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    } catch (e) {
      return null;
    }
  }

  function setAuth(loginVO) {
    localStorage.setItem(TOKEN_KEY, loginVO.token);
    localStorage.setItem(USER_KEY, JSON.stringify(loginVO.user));
    localStorage.setItem('campus_expire', String(Date.now() + (loginVO.expireSeconds || 86400) * 1000));
  }

  function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem('campus_expire');
  }

  function isLogin() {
    const expire = Number(localStorage.getItem('campus_expire') || 0);
    if (expire && Date.now() > expire) {
      clearAuth();
      return false;
    }
    return !!getToken();
  }

  function isAdmin() {
    const user = getUser();
    return !!user && user.role === 'ADMIN';
  }

  async function api(path, options) {
    options = options || {};
    const method = options.method || 'GET';
    let url = path;
    if (options.params) {
      const qs = new URLSearchParams();
      Object.keys(options.params).forEach(function (key) {
        const value = options.params[key];
        if (value !== undefined && value !== null && value !== '') {
          qs.append(key, value);
        }
      });
      const s = qs.toString();
      if (s) url += (url.indexOf('?') >= 0 ? '&' : '?') + s;
    }

    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    let body;
    if (options.form) {
      body = options.form;
    } else if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(options.body);
    }

    let resp;
    try {
      resp = await fetch(url, { method: method, headers: headers, body: body });
    } catch (e) {
      throw { code: -1, message: '网络异常，请检查服务是否启动' };
    }

    let data = null;
    try {
      data = await resp.json();
    } catch (e) {
      throw { code: resp.status, message: '服务器返回异常（HTTP ' + resp.status + '）' };
    }

    if (data.code !== 200) {
      if (data.code === 401) {
        clearAuth();
        const next = encodeURIComponent(location.pathname + location.search);
        if (!/login\.html/.test(location.pathname)) {
          setTimeout(function () { location.href = 'login.html?next=' + next; }, 400);
        }
      }
      throw { code: data.code, message: data.message || '请求失败' };
    }
    return data.data;
  }

  global.API = {
    get: function (path, params) { return api(path, { params: params }); },
    post: function (path, body) { return api(path, { method: 'POST', body: body }); },
    put: function (path, body) { return api(path, { method: 'PUT', body: body }); },
    del: function (path) { return api(path, { method: 'DELETE' }); },
    upload: function (file) {
      const form = new FormData();
      form.append('file', file);
      return api('/api/upload/image', { method: 'POST', form: form });
    },
    getToken: getToken,
    getUser: getUser,
    setAuth: setAuth,
    clearAuth: clearAuth,
    isLogin: isLogin,
    isAdmin: isAdmin
  };
})(window);
