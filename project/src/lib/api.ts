import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { storage } from './utils';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = storage.get('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = storage.get('refreshToken');
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/api/auth/refresh-token`, null, {
            headers: { Authorization: `Bearer ${refreshToken}` },
          });

          const { accessToken, refreshToken: newRefreshToken } = response.data.data;
          storage.set('accessToken', accessToken);
          storage.set('refreshToken', newRefreshToken);

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return api(originalRequest);
        }
      } catch {
        storage.remove('accessToken');
        storage.remove('refreshToken');
        storage.remove('user');
        window.location.href = '/';
      }
    }

    return Promise.reject(error);
  }
);

export default api;
