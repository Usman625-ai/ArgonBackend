import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import api from '../lib/api';
import type { User, AuthResponse, ApiResponse } from '../types';
import { storage } from '../lib/utils';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

const initialState: AuthState = {
  user: storage.get('user') || null,
  isAuthenticated: !!storage.get('accessToken'),
  isLoading: false,
  error: null,
};

export const login = createAsyncThunk(
  'auth/login',
  async (credentials: { email: string; password: string }, { rejectWithValue }) => {
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/api/auth/login', credentials);
      const { accessToken, refreshToken, user } = response.data.data;
      storage.set('accessToken', accessToken);
      storage.set('refreshToken', refreshToken);
      storage.set('user', user);
      return user;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Login failed');
    }
  }
);

export const register = createAsyncThunk(
  'auth/register',
  async (data: { name: string; email: string; password: string; role: string }, { rejectWithValue }) => {
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/api/auth/register', data);
      return response.data;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Registration failed');
    }
  }
);

export const verifyEmail = createAsyncThunk(
  'auth/verifyEmail',
  async (data: { email: string; otp: string }, { rejectWithValue }) => {
    try {
      await api.post('/api/auth/verify-email', data);
      return true;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Verification failed');
    }
  }
);

export const logout = createAsyncThunk('auth/logout', async () => {
  try {
    await api.post('/api/auth/logout');
  } catch {
    // ignore
  }
  storage.remove('accessToken');
  storage.remove('refreshToken');
  storage.remove('user');
});

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      state.isAuthenticated = true;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
      })
      .addCase(login.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      .addCase(logout.fulfilled, (state) => {
        state.user = null;
        state.isAuthenticated = false;
      });
  },
});

export const { clearError, setUser } = authSlice.actions;
export default authSlice.reducer;
