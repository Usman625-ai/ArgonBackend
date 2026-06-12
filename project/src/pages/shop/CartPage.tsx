import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Trash2, Minus, Plus, ShoppingBag, ArrowRight, Tag } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '../../store';
import {
  fetchCart,
  removeFromCart,
  updateCartItem,
  clearCart,
} from '../../store/cartSlice';
import { formatPrice } from '../../lib/utils';
import { Button, Card } from '../../components/ui';
import { toast } from 'sonner';
import api from '../../lib/api';

export default function CartPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { cart, isLoading } = useAppSelector((state) => state.cart);
  const { isAuthenticated } = useAppSelector((state) => state.auth);
  const [couponCode, setCouponCode] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCart());
    }
  }, [isAuthenticated]);

  const handleUpdateQuantity = async (itemId: number, quantity: number) => {
    if (quantity < 1) return;
    try {
      await dispatch(updateCartItem({ itemId, quantity })).unwrap();
    } catch (error) {
      toast.error('Failed to update quantity');
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    try {
      await dispatch(removeFromCart(itemId)).unwrap();
      toast.success('Item removed from cart');
    } catch (error) {
      toast.error('Failed to remove item');
    }
  };

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) {
      toast.error('Please enter a coupon code');
      return;
    }
    try {
      const response = await api.post('/api/customer/coupons/validate', {
        code: couponCode,
        cartTotal: cart?.total || 0,
      });
      toast.success('Coupon applied successfully');
    } catch (error) {
      toast.error('Invalid coupon code');
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center p-8">
        <div className="text-center">
          <ShoppingBag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Your Cart
          </h2>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            Sign in to view your cart and checkout
          </p>
          <Button onClick={() => navigate('/login')}>
            Sign In
          </Button>
        </div>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center p-8">
        <div className="text-center">
          <ShoppingBag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Your cart is empty
          </h2>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            Looks like you haven't added anything to your cart yet
          </p>
          <Link to="/shop/products">
            <Button>
              Start Shopping
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Shopping Cart ({cart.items.length} items)
          </h1>
          <Button variant="ghost" onClick={() => dispatch(clearCart())}>
            <Trash2 className="w-4 h-4 mr-2" />
            Clear Cart
          </Button>
        </div>

        <div className="flex flex-col lg:flex-row gap-8">
          <div className="flex-1 space-y-4">
            {cart.items.map((item, index) => (
              <motion.div
                key={item.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
              >
                <Card className="flex gap-4">
                  <Link to={`/shop/product/${item.product.slug}`} className="w-24 h-24 flex-shrink-0">
                    <div className="w-full h-full bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden">
                      {item.product.images[0] ? (
                        <img
                          src={item.product.images[0]}
                          alt={item.product.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <ShoppingBag className="w-8 h-8 text-gray-400" />
                        </div>
                      )}
                    </div>
                  </Link>
                  <div className="flex-1 min-w-0">
                    <Link
                      to={`/shop/product/${item.product.slug}`}
                      className="font-medium text-gray-900 dark:text-white hover:text-emerald-600 line-clamp-1"
                    >
                      {item.product.name}
                    </Link>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {item.product.category?.name}
                    </p>
                    <p className="text-emerald-600 font-semibold mt-1">
                      {formatPrice(item.price)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center border border-gray-200 dark:border-gray-700 rounded-lg">
                      <button
                        onClick={() => handleUpdateQuantity(item.id, item.quantity - 1)}
                        className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
                      >
                        <Minus className="w-4 h-4" />
                      </button>
                      <span className="px-3 py-1 font-medium">{item.quantity}</span>
                      <button
                        onClick={() =>
                          handleUpdateQuantity(item.id, Math.min(item.product.stock, item.quantity + 1))
                        }
                        className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
                      >
                        <Plus className="w-4 h-4" />
                      </button>
                    </div>
                    <p className="font-semibold text-gray-900 dark:text-white w-24 text-right">
                      {formatPrice(item.quantity * item.price)}
                    </p>
                    <button
                      onClick={() => handleRemoveItem(item.id)}
                      className="p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>
                </Card>
              </motion.div>
            ))}
          </div>

          <div className="lg:w-80">
            <Card className="sticky top-24">
              <h3 className="font-semibold text-gray-900 dark:text-white mb-4">
                Order Summary
              </h3>

              <div className="space-y-3 mb-4">
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Subtotal</span>
                  <span>{formatPrice(cart.subtotal)}</span>
                </div>
                {cart.discount > 0 && (
                  <div className="flex justify-between text-emerald-600">
                    <span>Discount</span>
                    <span>-{formatPrice(cart.discount)}</span>
                  </div>
                )}
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Shipping</span>
                  <span>{cart.subtotal >= 5000 ? 'Free' : formatPrice(200)}</span>
                </div>
              </div>

              <div className="border-t border-gray-200 dark:border-gray-700 pt-4 mb-4">
                <div className="flex justify-between font-semibold text-lg text-gray-900 dark:text-white">
                  <span>Total</span>
                  <span>
                    {formatPrice(
                      cart.total + (cart.subtotal >= 5000 ? 0 : 200)
                    )}
                  </span>
                </div>
              </div>

              <div className="mb-4">
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Tag className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="text"
                      value={couponCode}
                      onChange={(e) => setCouponCode(e.target.value)}
                      placeholder="Coupon code"
                      className="w-full pl-9 pr-3 py-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white"
                    />
                  </div>
                  <Button variant="outline" onClick={handleApplyCoupon}>
                    Apply
                  </Button>
                </div>
              </div>

              <Button
                onClick={() => navigate('/shop/checkout')}
                className="w-full"
                size="lg"
              >
                Proceed to Checkout
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>

              <p className="text-xs text-gray-500 dark:text-gray-400 text-center mt-4">
                Free shipping on orders over PKR 5,000
              </p>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
