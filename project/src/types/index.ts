export type UserRole = 'ADMIN' | 'SELLER' | 'CUSTOMER';
export type OrderStatus = 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type SellerStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';
export type PaymentMethod = 'COD' | 'JAZZCASH';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  verified: boolean;
  contactNumber?: string;
  profileImage?: string;
  createdAt: string;
}

export interface Seller extends User {
  shopName?: string;
  shopDescription?: string;
  shopLogo?: string;
  shopBanner?: string;
  gstNumber?: string;
  panNumber?: string;
  sellerStatus: SellerStatus;
  rejectionReason?: string;
}

export interface Address {
  id: number;
  label: string;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  phone: string;
  isDefault: boolean;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  image?: string;
  parentId?: number;
  children?: Category[];
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  discountPrice?: number;
  stock: number;
  sku?: string;
  brand?: string;
  images: string[];
  category: Category;
  seller: Seller;
  averageRating: number;
  totalReviews: number;
  active: boolean;
  createdAt: string;
}

export interface CartItem {
  id: number;
  product: Product;
  quantity: number;
  price: number;
}

export interface Cart {
  id: number;
  items: CartItem[];
  subtotal: number;
  discount: number;
  total: number;
  couponCode?: string;
}

export interface OrderItem {
  id: number;
  product: Product;
  quantity: number;
  price: number;
  total: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  shipping: number;
  total: number;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  shippingAddress: Address;
  trackingNumber?: string;
  notes?: string;
  customer: User;
  seller: Seller;
  createdAt: string;
  updatedAt: string;
}

export interface Review {
  id: number;
  rating: number;
  comment: string;
  user: User;
  product: Product;
  createdAt: string;
}

export interface Coupon {
  id: number;
  code: string;
  type: 'PERCENTAGE' | 'FIXED';
  value: number;
  minOrderValue: number;
  maxUses: number;
  usedCount: number;
  validFrom: string;
  validTo: string;
  active: boolean;
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: 'ORDER' | 'PAYMENT' | 'SYSTEM' | 'PROMO';
  read: boolean;
  createdAt: string;
}

export interface DashboardStats {
  totalSellers: number;
  totalCustomers: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  todayRevenue: number;
  monthRevenue: number;
  pendingSellers: number;
}

export interface SellerDashboardStats {
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  lowStockCount: number;
  pendingOrders: number;
  weeklySales: { date: string; amount: number }[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface JazzCashPaymentResponse {
  hostedPageUrl: string;
  formParams: Record<string, string>;
  orderNumber: string;
}
