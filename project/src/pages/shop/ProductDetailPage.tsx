import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Star, Minus, Plus, ShoppingCart, Heart, Share2, ChevronRight, Truck, Shield, RotateCcw } from 'lucide-react';
import api from '../../lib/api';
import type { Product, Review, ApiResponse, PagedResponse } from '../../types';
import { formatPrice } from '../../lib/utils';
import { Button, Card } from '../../components/ui';
import { useAppDispatch, useAppSelector } from '../../store';
import { addToCart } from '../../store/cartSlice';
import { toast } from 'sonner';

export default function ProductDetailPage() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [product, setProduct] = useState<Product | null>(null);
  const [reviews, setReviews] = useState<PagedResponse<Review> | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [activeTab, setActiveTab] = useState<'description' | 'reviews'>('description');

  const { isAuthenticated } = useAppSelector((state) => state.auth);
  const cart = useAppSelector((state) => state.cart.cart);
  const isInCart = cart?.items.some(item => item.product.id === product?.id);

  useEffect(() => {
    fetchProduct();
  }, [slug]);

  const fetchProduct = async () => {
    setLoading(true);
    try {
      const [productRes, reviewsRes] = await Promise.all([
        api.get<ApiResponse<Product>>(`/api/products/slug/${slug}`),
        api.get<ApiResponse<PagedResponse<Review>>>(`/api/products/${slug}/reviews?page=0&size=10`),
      ]);
      setProduct(productRes.data.data);
      setReviews(reviewsRes.data.data);
    } catch (error) {
      console.error('Failed to fetch product:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!product) return;
    try {
      await dispatch(addToCart({ productId: product.id, quantity })).unwrap();
      toast.success('Added to cart');
    } catch (error) {
      toast.error('Failed to add to cart');
    }
  };

  const handleBuyNow = async () => {
    await handleAddToCart();
    navigate('/shop/cart');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="animate-pulse flex flex-col lg:flex-row gap-8">
            <div className="lg:w-1/2 space-y-4">
              <div className="h-96 bg-gray-200 dark:bg-gray-800 rounded-xl" />
              <div className="flex gap-2">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="w-20 h-20 bg-gray-200 dark:bg-gray-800 rounded-lg" />
                ))}
              </div>
            </div>
            <div className="lg:w-1/2 space-y-4">
              <div className="h-8 w-3/4 bg-gray-200 dark:bg-gray-800 rounded" />
              <div className="h-4 w-1/2 bg-gray-200 dark:bg-gray-800 rounded" />
              <div className="h-6 w-1/3 bg-gray-200 dark:bg-gray-800 rounded" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500">Product not found</p>
          <Link to="/shop/products" className="text-emerald-600 hover:underline">
            Browse all products
          </Link>
        </div>
      </div>
    );
  }

  const discount = product.discountPrice
    ? Math.round((1 - product.discountPrice / product.price) * 100)
    : 0;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <nav className="flex items-center gap-2 text-sm mb-8">
          <Link to="/shop" className="text-gray-500 hover:text-emerald-600">
            Home
          </Link>
          <ChevronRight className="w-4 h-4 text-gray-400" />
          <Link to="/shop/products" className="text-gray-500 hover:text-emerald-600">
            Products
          </Link>
          <ChevronRight className="w-4 h-4 text-gray-400" />
          <Link
            to={`/shop/products?categoryId=${product.category?.id}`}
            className="text-gray-500 hover:text-emerald-600"
          >
            {product.category?.name}
          </Link>
          <ChevronRight className="w-4 h-4 text-gray-400" />
          <span className="text-gray-900 dark:text-white truncate max-w-32">{product.name}</span>
        </nav>

        <div className="flex flex-col lg:flex-row gap-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="lg:w-1/2"
          >
            <div className="relative">
              <div className="h-96 lg:h-[500px] bg-white dark:bg-gray-800 rounded-2xl overflow-hidden">
                {product.images[selectedImage] ? (
                  <motion.img
                    key={selectedImage}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    src={product.images[selectedImage]}
                    alt={product.name}
                    className="w-full h-full object-contain"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-400">
                    No Image
                  </div>
                )}
              </div>
              {discount > 0 && (
                <span className="absolute top-4 left-4 px-3 py-1 bg-red-500 text-white font-bold rounded-full">
                  -{discount}% OFF
                </span>
              )}
            </div>

            {product.images.length > 1 && (
              <div className="mt-4 flex gap-2 overflow-x-auto pb-2">
                {product.images.map((img, i) => (
                  <button
                    key={i}
                    onClick={() => setSelectedImage(i)}
                    className={`w-20 h-20 flex-shrink-0 rounded-lg overflow-hidden border-2 transition-colors ${
                      selectedImage === i
                        ? 'border-emerald-600'
                        : 'border-transparent hover:border-gray-300'
                    }`}
                  >
                    <img src={img} alt="" className="w-full h-full object-cover" />
                  </button>
                ))}
              </div>
            )}
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="lg:w-1/2"
          >
            <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 lg:p-8">
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400 mb-2">
                <Truck className="w-4 h-4" />
                <span>Sold by: {product.seller?.name}</span>
              </div>

              <h1 className="text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white mb-2">
                {product.name}
              </h1>

              <div className="flex items-center gap-4 mb-6">
                <div className="flex items-center gap-1">
                  {[...Array(5)].map((_, i) => (
                    <Star
                      key={i}
                      className={`w-5 h-5 ${
                        i < Math.round(product.averageRating)
                          ? 'fill-amber-400 text-amber-400'
                          : 'text-gray-300'
                      }`}
                    />
                  ))}
                  <span className="ml-2 text-gray-600 dark:text-gray-400">
                    {product.averageRating.toFixed(1)} ({product.totalReviews} reviews)
                  </span>
                </div>
              </div>

              <div className="flex items-baseline gap-3 mb-6">
                <span className="text-3xl font-bold text-emerald-600">
                  {formatPrice(product.discountPrice || product.price)}
                </span>
                {product.discountPrice && (
                  <span className="text-xl text-gray-400 line-through">
                    {formatPrice(product.price)}
                  </span>
                )}
              </div>

              <p className="text-gray-600 dark:text-gray-400 mb-6 line-clamp-3">
                {product.description}
              </p>

              <div className="flex items-center gap-4 mb-6">
                <span className="text-sm text-gray-600 dark:text-gray-400">Quantity:</span>
                <div className="flex items-center border border-gray-200 dark:border-gray-700 rounded-lg">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
                  >
                    <Minus className="w-4 h-4" />
                  </button>
                  <span className="px-4 py-2 font-medium">{quantity}</span>
                  <button
                    onClick={() => setQuantity(Math.min(product.stock, quantity + 1))}
                    className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>
                <span className="text-sm text-gray-500">
                  {product.stock} items left
                </span>
              </div>

              <div className="flex flex-col sm:flex-row gap-3 mb-6">
                <Button
                  onClick={handleAddToCart}
                  variant="outline"
                  size="lg"
                  className="flex-1"
                  disabled={product.stock === 0 || isInCart}
                >
                  <ShoppingCart className="w-5 h-5 mr-2" />
                  {isInCart ? 'In Cart' : 'Add to Cart'}
                </Button>
                <Button
                  onClick={handleBuyNow}
                  size="lg"
                  className="flex-1"
                  disabled={product.stock === 0}
                >
                  Buy Now
                </Button>
              </div>

              <div className="flex items-center gap-3">
                <Button variant="ghost" size="sm">
                  <Heart className="w-4 h-4 mr-2" />
                  Add to Wishlist
                </Button>
                <Button variant="ghost" size="sm">
                  <Share2 className="w-4 h-4 mr-2" />
                  Share
                </Button>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-3 gap-4">
              <div className="bg-white dark:bg-gray-800 rounded-xl p-4 text-center">
                <Truck className="w-6 h-6 mx-auto text-emerald-600 mb-2" />
                <p className="text-sm font-medium text-gray-900 dark:text-white">Free Delivery</p>
                <p className="text-xs text-gray-500">On orders over PKR 5000</p>
              </div>
              <div className="bg-white dark:bg-gray-800 rounded-xl p-4 text-center">
                <RotateCcw className="w-6 h-6 mx-auto text-emerald-600 mb-2" />
                <p className="text-sm font-medium text-gray-900 dark:text-white">7 Day Returns</p>
                <p className="text-xs text-gray-500">Easy return policy</p>
              </div>
              <div className="bg-white dark:bg-gray-800 rounded-xl p-4 text-center">
                <Shield className="w-6 h-6 mx-auto text-emerald-600 mb-2" />
                <p className="text-sm font-medium text-gray-900 dark:text-white">Secure Payment</p>
                <p className="text-xs text-gray-500">100% secure</p>
              </div>
            </div>
          </motion.div>
        </div>

        <div className="mt-12">
          <Card>
            <div className="flex border-b border-gray-200 dark:border-gray-700 mb-6">
              <button
                onClick={() => setActiveTab('description')}
                className={`px-6 py-3 font-medium transition-colors ${
                  activeTab === 'description'
                    ? 'text-emerald-600 border-b-2 border-emerald-600'
                    : 'text-gray-500'
                }`}
              >
                Description
              </button>
              <button
                onClick={() => setActiveTab('reviews')}
                className={`px-6 py-3 font-medium transition-colors ${
                  activeTab === 'reviews'
                    ? 'text-emerald-600 border-b-2 border-emerald-600'
                    : 'text-gray-500'
                }`}
              >
                Reviews ({product.totalReviews})
              </button>
            </div>

            {activeTab === 'description' ? (
              <div className="prose dark:prose-invert max-w-none">
                <p className="text-gray-600 dark:text-gray-400 whitespace-pre-line">
                  {product.description}
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {reviews?.content.length === 0 ? (
                  <p className="text-gray-500 dark:text-gray-400 text-center py-8">
                    No reviews yet. Be the first to review this product!
                  </p>
                ) : (
                  reviews?.content.map((review) => (
                    <div key={review.id} className="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center">
                            <span className="text-emerald-600 font-medium">
                              {review.user?.name?.charAt(0) || 'U'}
                            </span>
                          </div>
                          <div>
                            <p className="font-medium text-gray-900 dark:text-white">
                              {review.user?.name || 'Anonymous'}
                            </p>
                            <div className="flex items-center gap-1">
                              {[...Array(5)].map((_, i) => (
                                <Star
                                  key={i}
                                  className={`w-3 h-3 ${
                                    i < review.rating
                                      ? 'fill-amber-400 text-amber-400'
                                      : 'text-gray-300'
                                  }`}
                                />
                              ))}
                            </div>
                          </div>
                        </div>
                      </div>
                      <p className="text-gray-600 dark:text-gray-400">{review.comment}</p>
                    </div>
                  ))
                )}
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
