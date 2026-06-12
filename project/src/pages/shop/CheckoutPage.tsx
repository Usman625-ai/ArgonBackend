import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { MapPin, CreditCard, Truck, Check, AlertCircle } from 'lucide-react';
import { useAppSelector, useAppDispatch } from '../../store';
import { formatPrice } from '../../lib/utils';
import { Card, Button, Badge } from '../../components/ui';
import type { Address, AddressResponse, ApiResponse } from '../../types';
import api from '../../lib/api';
import { toast } from 'sonner';
import { clearCart } from '../../store/cartSlice';

export default function CheckoutPage() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { cart } = useAppSelector((state) => state.cart);
  const { user } = useAppSelector((state) => state.auth);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<number | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<'COD' | 'JAZZCASH'>('COD');
  const [loading, setLoading] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);
  const [orderNumber, setOrderNumber] = useState('');

  useEffect(() => {
    fetchAddresses();
  }, []);

  const fetchAddresses = async () => {
    try {
      const response = await api.get<ApiResponse<Address[]>>('/api/customer/addresses');
      setAddresses(response.data.data);
      if (response.data.data.length > 0) {
        const defaultAddr = response.data.data.find((a) => a.isDefault);
        setSelectedAddress(defaultAddr?.id || response.data.data[0].id);
      }
    } catch (error) {
      console.error('Failed to fetch addresses:', error);
    }
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddress) {
      toast.error('Please select a delivery address');
      return;
    }
    setLoading(true);
    try {
      const response = await api.post('/api/customer/orders/checkout', {
        addressId: selectedAddress,
        paymentMethod,
      });
      const orders = response.data.data;
      setOrderNumber(orders[0]?.orderNumber || 'ORD-' + Date.now());
      setOrderSuccess(true);
      dispatch(clearCart());
      toast.success('Order placed successfully!');
    } catch (error) {
      toast.error('Failed to place order. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (!cart || cart.items.length === 0) {
    navigate('/shop/cart');
    return null;
  }

  if (orderSuccess) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center p-8">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center max-w-md"
        >
          <div className="w-20 h-20 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center mx-auto mb-6">
            <Check className="w-10 h-10 text-emerald-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Order Placed Successfully!
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mb-2">
            Thank you for your order. We've received your order and will process it shortly.
          </p>
          <p className="font-mono text-lg mb-6">
            Order Number: <span className="text-emerald-600">{orderNumber}</span>
          </p>
          <div className="space-y-3">
            <Button onClick={() => navigate('/shop/orders')} className="w-full">
              Track Order
            </Button>
            <Button variant="outline" onClick={() => navigate('/shop')} className="w-full">
              Continue Shopping
            </Button>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-8">Checkout</h1>

        <div className="grid gap-8 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-8 h-8 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center">
                  <MapPin className="w-4 h-4 text-emerald-600" />
                </div>
                <h2 className="font-semibold text-gray-900 dark:text-white">
                  Delivery Address
                </h2>
              </div>

              {addresses.length === 0 ? (
                <div className="text-center py-6">
                  <AlertCircle className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                  <p className="text-gray-500 dark:text-gray-400 mb-4">
                    You haven't added any addresses yet
                  </p>
                  <Button onClick={() => navigate('/shop/profile?tab=addresses')}>
                    Add Address
                  </Button>
                </div>
              ) : (
                <div className="space-y-3">
                  {addresses.map((address) => (
                    <label
                      key={address.id}
                      className={`block p-4 rounded-xl border-2 cursor-pointer transition-all ${
                        selectedAddress === address.id
                          ? 'border-emerald-600 bg-emerald-50 dark:bg-emerald-900/20'
                          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <input
                          type="radio"
                          name="address"
                          checked={selectedAddress === address.id}
                          onChange={() => setSelectedAddress(address.id)}
                          className="mt-1"
                        />
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <p className="font-medium text-gray-900 dark:text-white">
                              {address.label}
                            </p>
                            {address.isDefault && (
                              <Badge variant="success">Default</Badge>
                            )}
                          </div>
                          <p className="text-gray-600 dark:text-gray-400 text-sm">
                            {address.street}, {address.city}, {address.state} {address.postalCode}
                          </p>
                          <p className="text-gray-500 dark:text-gray-400 text-sm">
                            Phone: {address.phone}
                          </p>
                        </div>
                      </div>
                    </label>
                  ))}
                </div>
              )}
            </Card>

            <Card>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-8 h-8 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center">
                  <CreditCard className="w-4 h-4 text-emerald-600" />
                </div>
                <h2 className="font-semibold text-gray-900 dark:text-white">
                  Payment Method
                </h2>
              </div>

              <div className="space-y-3">
                <label
                  className={`flex items-center gap-4 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                    paymentMethod === 'COD'
                      ? 'border-emerald-600 bg-emerald-50 dark:bg-emerald-900/20'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment"
                    checked={paymentMethod === 'COD'}
                    onChange={() => setPaymentMethod('COD')}
                  />
                  <div className="flex-1">
                    <p className="font-medium text-gray-900 dark:text-white">
                      Cash on Delivery (COD)
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      Pay when you receive your order
                    </p>
                  </div>
                  <Truck className="w-6 h-6 text-gray-400" />
                </label>

                <label
                  className={`flex items-center gap-4 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                    paymentMethod === 'JAZZCASH'
                      ? 'border-emerald-600 bg-emerald-50 dark:bg-emerald-900/20'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment"
                    checked={paymentMethod === 'JAZZCASH'}
                    onChange={() => setPaymentMethod('JAZZCASH')}
                  />
                  <div className="flex-1">
                    <p className="font-medium text-gray-900 dark:text-white">
                      JazzCash
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      Pay securely with JazzCash
                    </p>
                  </div>
                  <div className="w-12 h-6 bg-red-500 rounded flex items-center justify-center">
                    <span className="text-white text-xs font-bold">J</span>
                  </div>
                </label>
              </div>
            </Card>
          </div>

          <div className="lg:col-span-1">
            <Card className="sticky top-24">
              <h3 className="font-semibold text-gray-900 dark:text-white mb-4">
                Order Summary
              </h3>

              <div className="space-y-3 mb-4">
                {cart.items.map((item) => (
                  <div key={item.id} className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden">
                      {item.product.images[0] && (
                        <img
                          src={item.product.images[0]}
                          alt={item.product.name}
                          className="w-full h-full object-cover"
                        />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                        {item.product.name}
                      </p>
                      <p className="text-xs text-gray-500">Qty: {item.quantity}</p>
                    </div>
                    <p className="text-sm font-medium">{formatPrice(item.quantity * item.price)}</p>
                  </div>
                ))}
              </div>

              <div className="space-y-2 border-t border-gray-200 dark:border-gray-700 pt-4 mb-4">
                <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400">
                  <span>Subtotal</span>
                  <span>{formatPrice(cart.subtotal)}</span>
                </div>
                <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400">
                  <span>Shipping</span>
                  <span>{cart.subtotal >= 5000 ? 'Free' : formatPrice(200)}</span>
                </div>
              </div>

              <div className="border-t border-gray-200 dark:border-gray-700 pt-4 mb-6">
                <div className="flex justify-between font-semibold text-lg text-gray-900 dark:text-white">
                  <span>Total</span>
                  <span>
                    {formatPrice(
                      cart.total + (cart.subtotal >= 5000 ? 0 : 200)
                    )}
                  </span>
                </div>
              </div>

              <Button
                onClick={handlePlaceOrder}
                isLoading={loading}
                disabled={!selectedAddress}
                className="w-full"
                size="lg"
              >
                Place Order
              </Button>

              {!selectedAddress && (
                <p className="text-xs text-red-500 text-center mt-2">
                  Please select a delivery address
                </p>
              )}
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
