/**
 * API 封装层
 * - axios 实例，baseURL=/api
 * - 请求拦截：自动附加 JWT token
 * - 响应拦截：401 跳转登录，业务错误抛出 Error
 */

const instance = axios.create({ baseURL: '/api' });

instance.interceptors.request.use(config => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token;
  }
  return config;
});

instance.interceptors.response.use(
  response => {
    const data = response.data;
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败，code=' + data.code));
    }
    return data;
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('jwt_username');
      window.location.href = '/index.html';
    }
    return Promise.reject(error);
  }
);

const api = {
  auth: {
    login(username, password) {
      return instance.post('/auth/login', { username, password });
    }
  },
  accounts: {
    list(current = 1, size = 10) {
      return instance.get('/accounts', { params: { current, size } });
    },
    get(id) {
      return instance.get('/accounts/' + id);
    },
    create(data) {
      return instance.post('/accounts', data);
    },
    delete(id) {
      return instance.delete('/accounts/' + id);
    },
    sendSms(id) {
      return instance.post('/accounts/' + id + '/send-sms');
    },
    loginSms(id, code) {
      return instance.post('/accounts/' + id + '/login-sms', { code });
    }
  },
  tasks: {
    list(current = 1, size = 10) {
      return instance.get('/tasks', { params: { current, size } });
    },
    get(id) {
      return instance.get('/tasks/' + id);
    },
    create(data) {
      return instance.post('/tasks', data);
    },
    update(id, data) {
      return instance.put('/tasks/' + id, data);
    },
    delete(id) {
      return instance.delete('/tasks/' + id);
    },
    schedule(id) {
      return instance.post('/tasks/' + id + '/schedule');
    },
    cancel(id) {
      return instance.post('/tasks/' + id + '/cancel');
    }
  },
  logs: {
    list(params = {}) {
      return instance.get('/logs', { params });
    }
  },
  platforms: {
    list() {
      return instance.get('/platforms');
    }
  }
};
