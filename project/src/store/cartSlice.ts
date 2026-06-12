import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import api from '../lib/api';
import type { Cart, CartItem, ApiResponse } from '../types';
import { storage } from '../lib/utils';

interface CartState {
  cart: Cart | null;
  isLoading: boolean;
  error: string | null;
}

const initialState: CartState = {
  cart: storage.get('cart') || null,
  isLoading: false,
  error: null,
};

export const fetchCart = createAsyncThunk('cart/fetch', async (_, { rejectWithValue }) => {
  try {
    const response = await api.get<ApiResponse<Cart>>('/api/customer/cart');
    storage.set('cart', response.data.data);
    return response.data.data;
  } catch (err: unknown) {
    const error = err as { response?: { data?: { message?: string } } };
    return rejectWithValue(error.response?.data?.message || 'Failed to fetch cart');
  }
});

export const addToCart = createAsyncThunk(
  'cart/add',
  async (data: { productId: number; quantity: number }, { rejectWithValue }) => {
    try {
      const response = await api.post<ApiResponse<Cart>>('/api/customer/cart', data);
      storage.set('cart', response.data.data);
      return response.data.data;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Failed to add to cart');
    }
  }
);

export const updateCartItem = createAsyncThunk(
  'cart/update',
  async ({ itemId, quantity }: { itemId: number; quantity: number }, { rejectWithValue }) => {
    try {
      const response = await api.put<ApiResponse<Cart>>(`/api/customer/cart/${itemId}?quantity=${quantity}`);
      storage.set('cart', response.data.data);
      return response.data.data;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Failed to update cart');
    }
  }
);

export const removeFromCart = createAsyncThunk(
  'cart/remove',
  async (itemId: number, { rejectWithValue }) => {
    try {
      const response = await api.delete<ApiResponse<Cart>>(`/api/customer/cart/${itemId}`);
      storage.set('cart', response.data.data);
      return response.data.data;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Failed to remove item');
    }
  }
);

export const clearCart = createAsyncThunk('cart/clear', async (_, { rejectWithValue }) => {
  try {
    await api.delete('/api/customer/cart/clear');
    storage.remove('cart');
    return null;
  } catch (err: unknown) {
    const error = err as { response?: { data?: { message?: string } } };
    return rejectWithValue(error.response?.data?.message || 'Failed to clear cart');
  }
});

export const applyCoupon = createAsyncThunk(
  'cart/applyCoupon',
  async (code: string, { rejectWithValue }) => {
    try {
      const response = await api.post<ApiResponse<Cart>>('/api/customer/coupons/validate', {
        code,
        cartTotal: 0,
      });
      return response.data.data;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      return rejectWithValue(error.response?.data?.message || 'Invalid coupon');
    }
  }
);

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    setCart: (state, action: PayloadAction<Cart | null>) => {
      state.cart = action.payload;
      if (action.payload) {
        storage.set('cart', action.payload);
      } else {
        storage.remove('cart');
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCart.fulfilled, (state, action) => {
        state.cart = action.payload;
      })
      .addCase(addToCart.fulfilled, (state, action) => {
        state.cart = action.payload;
      })
      .addCase(updateCartItem.fulfilled, (state, action) => {
        state.cart = action.payload;
      })
      .addCase(removeFromCart.fulfilled, (state, action) => {
        state.cart = action.payload;
      })
      .addCase(clearCart.fulfilled, (state) => {
        state.cart = null;
      });
  },
});

export const { setCart } = cartSlice.actions;
export default cartSlice.reducer;
