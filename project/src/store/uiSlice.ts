import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UIState {
  sidebarOpen: boolean;
  theme: 'light' | 'dark';
  maintenanceMode: boolean;
}

const initialState: UIState = {
  sidebarOpen: false,
  theme: 'light',
  maintenanceMode: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    toggleTheme: (state) => {
      state.theme = state.theme === 'light' ? 'dark' : 'light';
    },
    setTheme: (state, action: PayloadAction<'light' | 'dark'>) => {
      state.theme = action.payload;
    },
    setMaintenanceMode: (state, action: PayloadAction<boolean>) => {
      state.maintenanceMode = action.payload;
    },
  },
});

export const { toggleSidebar, setSidebarOpen, toggleTheme, setTheme, setMaintenanceMode } =
  uiSlice.actions;
export default uiSlice.reducer;
