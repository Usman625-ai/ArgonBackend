import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store';
import { Toaster } from 'sonner';

// Layouts
import { CustomerLayout, DashboardLayout, ProtectedRoute } from './components/layout';

// Auth pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import LandingPage from './pages/auth/LandingPage';
import UnauthorizedPage from './pages/auth/UnauthorizedPage';

// Admin pages
import {
  AdminDashboard,
  SellersPage,
  CategoriesPage,
  CouponsPage,
  OrdersPage as AdminOrdersPage,
  ProductsPage as AdminProductsPage,
  ReportsPage,
  SettingsPage,
} from './pages/admin';

// Seller pages
import {
  SellerDashboard,
  ProductsPage as SellerProductsPage,
  OrdersPage as SellerOrdersPage,
  RevenuePage,
  SettingsPage as SellerSettingsPage,
} from './pages/seller';

// Shop pages
import {
  HomePage,
  ProductsPage,
  ProductDetailPage,
  CartPage,
  CheckoutPage,
  OrdersPage,
  ProfilePage,
  WishlistPage,
} from './pages/shop';

function App() {
  return (
    <Provider store={store}>
      <BrowserRouter>
        <Routes>
          {/* Landing */}
          <Route path="/" element={<LandingPage />} />

          {/* Auth */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/unauthorized" element={<UnauthorizedPage />} />

          {/* Admin Routes */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/admin/dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="sellers" element={<SellersPage />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="categories" element={<CategoriesPage />} />
            <Route path="coupons" element={<CouponsPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>

          {/* Seller Routes */}
          <Route
            path="/seller"
            element={
              <ProtectedRoute allowedRoles={['SELLER']}>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/seller/dashboard" replace />} />
            <Route path="dashboard" element={<SellerDashboard />} />
            <Route path="products" element={<SellerProductsPage />} />
            <Route path="orders" element={<SellerOrdersPage />} />
            <Route path="revenue" element={<RevenuePage />} />
            <Route path="settings" element={<SellerSettingsPage />} />
          </Route>

          {/* Customer/Shop Routes */}
          <Route path="/shop" element={<CustomerLayout />}>
            <Route index element={<HomePage />} />
            <Route path="products" element={<ProductsPage />} />
            <Route path="product/:slug" element={<ProductDetailPage />} />
            <Route path="cart" element={<CartPage />} />
            <Route path="checkout" element={<CheckoutPage />} />
            <Route path="orders" element={<OrdersPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="wishlist" element={<WishlistPage />} />
          </Route>

          {/* Catch all - redirect to landing */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
      <Toaster position="top-right" richColors />
    </Provider>
  );
}

export default App;
