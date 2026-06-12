import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard,
  Package,
  ShoppingCart,
  Tags,
  Users,
  Settings,
  FileText,
  LogOut,
  X,
  ChevronLeft,
  Percent,
  DollarSign,
  Bell,
  BarChart3,
} from 'lucide-react';
import { useAppSelector, useAppDispatch } from '../../store';
import { logout } from '../../store/authSlice';
import { cn } from '../../lib/utils';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
}

const adminNavItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/admin/dashboard' },
  { icon: Users, label: 'Sellers', path: '/admin/sellers' },
  { icon: Package, label: 'Products', path: '/admin/products' },
  { icon: ShoppingCart, label: 'Orders', path: '/admin/orders' },
  { icon: Tags, label: 'Categories', path: '/admin/categories' },
  { icon: Percent, label: 'Coupons', path: '/admin/coupons' },
  { icon: BarChart3, label: 'Reports', path: '/admin/reports' },
  { icon: Settings, label: 'Settings', path: '/admin/settings' },
];

const sellerNavItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/seller/dashboard' },
  { icon: Package, label: 'Products', path: '/seller/products' },
  { icon: ShoppingCart, label: 'Orders', path: '/seller/orders' },
  { icon: DollarSign, label: 'Revenue', path: '/seller/revenue' },
  { icon: FileText, label: 'Reports', path: '/seller/reports' },
  { icon: Bell, label: 'Notifications', path: '/seller/notifications' },
  { icon: Settings, label: 'Settings', path: '/seller/settings' },
];

export default function DashboardSidebar({
  isOpen,
  onClose,
  collapsed = false,
  onToggleCollapse,
}: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);

  const navItems = user?.role === 'ADMIN' ? adminNavItems : sellerNavItems;

  const handleLogout = async () => {
    await dispatch(logout());
    navigate('/');
  };

  const isActive = (path: string) => location.pathname === path;

  const sidebarContent = (
    <div className="flex flex-col h-full">
      <div className="p-4 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-gradient-to-br from-emerald-500 to-teal-600 rounded-xl flex items-center justify-center flex-shrink-0">
              <span className="text-white font-bold text-xl">M</span>
            </div>
            {!collapsed && (
              <div>
                <h2 className="font-semibold text-gray-900 dark:text-white">MultiVend</h2>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {user?.role === 'ADMIN' ? 'Admin Panel' : 'Seller Dashboard'}
                </p>
              </div>
            )}
          </div>
          <button
            onClick={onClose}
            className="lg:hidden p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            <X className="w-5 h-5 text-gray-500" />
          </button>
          {onToggleCollapse && (
            <button
              onClick={onToggleCollapse}
              className="hidden lg:flex p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800"
            >
              <ChevronLeft
                className={cn('w-5 h-5 text-gray-500 transition-transform', collapsed && 'rotate-180')}
              />
            </button>
          )}
        </div>
      </div>

      {user?.role === 'SELLER' && !collapsed && (
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-xl flex items-center justify-center">
              {user.shopName?.charAt(0) || 'S'}
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-white">{user.shopName || 'Shop'}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">View Profile</p>
            </div>
          </div>
        </div>
      )}

      <nav className="flex-1 py-4 overflow-y-auto">
        <ul className="space-y-1 px-3">
          {navItems.map((item) => (
            <li key={item.path}>
              <Link
                to={item.path}
                onClick={onClose}
                className={cn(
                  'flex items-center space-x-3 px-3 py-2.5 rounded-xl transition-all duration-200',
                  isActive(item.path)
                    ? 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                )}
              >
                <item.icon
                  className={cn('w-5 h-5 flex-shrink-0', isActive(item.path) && 'text-emerald-600 dark:text-emerald-400')}
                />
                {!collapsed && <span className="font-medium">{item.label}</span>}
              </Link>
            </li>
          ))}
        </ul>
      </nav>

      <div className="p-4 border-t border-gray-200 dark:border-gray-700">
        <button
          onClick={handleLogout}
          className={cn(
            'flex items-center space-x-3 w-full px-3 py-2.5 rounded-xl text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors',
            collapsed && 'justify-center'
          )}
        >
          <LogOut className="w-5 h-5 flex-shrink-0" />
          {!collapsed && <span className="font-medium">Logout</span>}
        </button>
      </div>
    </div>
  );

  return (
    <>
      <motion.div
        initial={{ x: '-100%' }}
        animate={{ x: isOpen ? 0 : '-100%' }}
        className="fixed inset-y-0 left-0 z-50 w-64 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 lg:hidden"
      >
        {sidebarContent}
      </motion.div>

      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          'hidden lg:flex flex-col bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 h-screen sticky top-0',
          collapsed ? 'w-20' : 'w-64'
        )}
      >
        {sidebarContent}
      </aside>
    </>
  );
}
