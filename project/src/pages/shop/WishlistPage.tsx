import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Heart, Trash2, ShoppingCart } from 'lucide-react';
import api from '../../lib/api';
import type { Product, PagedResponse, ApiResponse } from '../../types';
import { formatPrice } from '../../lib/utils';
import { Button, Card } from '../../components/ui';
import { toast } from 'sonner';
import { useAppDispatch } from '../../store';
import { addToCart } from '../../store/cartSlice';

export default function WishlistPage() {
  const [products, setProducts] = useState<PagedResponse<Product> | null>(null);
  const [loading, setLoading] = useState(true);
  const dispatch = useAppDispatch();

  useEffect(() => {
    fetchWishlist();
  }, []);

  const fetchWishlist = async () => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<PagedResponse<Product>>>(
        '/api/customer/wishlist?page=0&size=20'
      );
      setProducts(response.data.data);
    } catch (error) {
      console.error('Failed to fetch wishlist:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (productId: number) => {
    try {
      await api.delete(`/api/customer/wishlist/${productId}`);
      toast.success('Removed from wishlist');
      fetchWishlist();
    } catch (error) {
      toast.error('Failed to remove');
    }
  };

  const handleAddToCart = async (product: Product) => {
    try {
      await dispatch(addToCart({ productId: product.id, quantity: 1 })).unwrap();
      toast.success('Added to cart');
    } catch (error) {
      toast.error('Failed to add to cart');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 p-8">
        <div className="max-w-6xl mx-auto">
          <div className="animate-pulse h-8 w-32 bg-gray-200 dark:bg-gray-700 rounded mb-8" />
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="animate-pulse h-64 bg-gray-200 dark:bg-gray-700 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">My Wishlist</h1>
            <p className="text-gray-500 dark:text-gray-400">
              {products?.totalElements || 0} items saved
            </p>
          </div>
        </div>

        {products?.content.length === 0 ? (
          <div className="text-center py-12">
            <Heart className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              Your wishlist is empty
            </h2>
            <p className="text-gray-500 dark:text-gray-400 mb-6">
              Save items you love to buy them later
            </p>
            <Link to="/shop/products">
              <Button>
                Browse Products
              </Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 lg:gap-6">
            {products?.content.map((product, i) => (
              <motion.div
                key={product.id}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: i * 0.05 }}
              >
                <Card padding="none" className="overflow-hidden">
                  <Link to={`/shop/product/${product.slug}`} className="block relative h-40">
                    <div className="w-full h-full bg-gray-100 dark:bg-gray-700">
                      {product.images[0] ? (
                        <img
                          src={product.images[0]}
                          alt={product.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <Heart className="w-10 h-10 text-gray-400 fill-gray-400" />
                        </div>
                      )}
                    </div>
                  </Link>
                  <div className="p-4">
                    <Link to={`/shop/product/${product.slug}`}>
                      <h3 className="font-medium text-gray-900 dark:text-white truncate hover:text-emerald-600">
                        {product.name}
                      </h3>
                    </Link>
                    <p className="text-emerald-600 font-semibold mt-1">
                      {formatPrice(product.discountPrice || product.price)}
                    </p>
                    <div className="flex items-center gap-2 mt-3">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleAddToCart(product)}
                        className="flex-1"
                      >
                        <ShoppingCart className="w-4 h-4 mr-1" />
                        Add
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => handleRemove(product.id)}
                        className="text-red-600"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </Card>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
