import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, ShoppingBag, Zap, Shield, Truck, Star } from 'lucide-react';
import api from '../../lib/api';
import type { Product, Category, ApiResponse, PagedResponse } from '../../types';
import { formatPrice } from '../../lib/utils';

export default function HomePage() {
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [productsRes, categoriesRes] = await Promise.all([
        api.get<ApiResponse<PagedResponse<Product>>>('/api/products?page=0&size=8&sortBy=createdAt&sortDir=desc'),
        api.get<ApiResponse<Category[]>>('/api/categories'),
      ]);
      setFeaturedProducts(productsRes.data.data.content);
      setCategories(categoriesRes.data.data);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
    }
  };

  const features = [
    { icon: Truck, title: 'Free Shipping', desc: 'On orders over PKR 5000' },
    { icon: Shield, title: 'Secure Payment', desc: '100% secure transactions' },
    { icon: Zap, title: 'Fast Delivery', desc: '2-5 business days' },
    { icon: ShoppingBag, title: 'Easy Returns', desc: '7-day return policy' },
  ];

  return (
    <div className="min-h-screen">
      <section className="relative overflow-hidden bg-gradient-to-br from-emerald-50 via-white to-teal-50 dark:from-gray-900 dark:via-gray-950 dark:to-gray-900">
        <div className="absolute inset-0">
          <div className="absolute top-20 left-20 w-72 h-72 bg-emerald-400/20 rounded-full blur-3xl" />
          <div className="absolute bottom-20 right-20 w-96 h-96 bg-teal-400/20 rounded-full blur-3xl" />
        </div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 lg:py-32">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -50 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5 }}
            >
              <span className="inline-block px-4 py-1 bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 rounded-full text-sm font-medium mb-6">
                Pakistani Multi-Vendor Marketplace
              </span>
              <h1 className="text-4xl lg:text-6xl font-bold text-gray-900 dark:text-white leading-tight mb-6">
                Discover Amazing Products from{' '}
                <span className="text-emerald-600">Trusted Sellers</span>
              </h1>
              <p className="text-lg text-gray-600 dark:text-gray-400 mb-8 max-w-lg">
                Shop from hundreds of verified sellers. Quality products, great prices, and fast delivery across Pakistan.
              </p>
              <div className="flex flex-col sm:flex-row gap-4">
                <Link
                  to="/shop/products"
                  className="inline-flex items-center justify-center px-8 py-4 bg-emerald-600 text-white rounded-xl font-semibold hover:bg-emerald-700 transition-all shadow-lg hover:shadow-xl group"
                >
                  Shop Now
                  <ArrowRight className="ml-2 w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </Link>
                <Link
                  to="/register?role=SELLER"
                  className="inline-flex items-center justify-center px-8 py-4 border-2 border-gray-200 dark:border-gray-700 text-gray-900 dark:text-white rounded-xl font-semibold hover:bg-gray-50 dark:hover:bg-gray-800 transition-all"
                >
                  Become a Seller
                </Link>
              </div>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.5, delay: 0.2 }}
              className="relative hidden lg:block"
            >
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-4">
                  <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-xl">
                    <div className="w-12 h-12 bg-emerald-100 dark:bg-emerald-900/30 rounded-xl flex items-center justify-center mb-4">
                      <ShoppingBag className="w-6 h-6 text-emerald-600" />
                    </div>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">10K+</p>
                    <p className="text-gray-500 dark:text-gray-400">Products</p>
                  </div>
                  <div className="bg-gradient-to-br from-emerald-500 to-teal-600 rounded-2xl p-6 shadow-xl text-white">
                    <p className="text-lg font-semibold">Up to 50% OFF</p>
                    <p className="text-sm opacity-90">On selected items</p>
                  </div>
                </div>
                <div className="space-y-4 pt-8">
                  <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-xl">
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">500+</p>
                    <p className="text-gray-500 dark:text-gray-400">Sellers</p>
                  </div>
                  <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-xl">
                    <div className="flex items-center gap-1 mb-2">
                      {[...Array(5)].map((_, i) => (
                        <Star key={i} className="w-4 h-4 fill-amber-400 text-amber-400" />
                      ))}
                    </div>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">4.8</p>
                    <p className="text-gray-500 dark:text-gray-400">Avg Rating</p>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      <section className="py-6 bg-gray-50 dark:bg-gray-900 border-y border-gray-200 dark:border-gray-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.1 }}
                className="flex items-center gap-4"
              >
                <div className="w-12 h-12 bg-emerald-100 dark:bg-emerald-900/30 rounded-xl flex items-center justify-center flex-shrink-0">
                  <feature.icon className="w-6 h-6 text-emerald-600" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900 dark:text-white">{feature.title}</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{feature.desc}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-16 lg:py-24 bg-white dark:bg-gray-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white">
                Shop by Category
              </h2>
              <p className="text-gray-500 dark:text-gray-400 mt-1">
                Browse our wide range of categories
              </p>
            </div>
            <Link
              to="/shop/categories"
              className="text-emerald-600 hover:text-emerald-700 font-medium flex items-center gap-1"
            >
              View All
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
            {loading
              ? [...Array(6)].map((_, i) => (
                  <div key={i} className="animate-pulse">
                    <div className="h-24 bg-gray-200 dark:bg-gray-700 rounded-xl mb-2" />
                    <div className="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded mx-auto" />
                  </div>
                ))
              : categories.slice(0, 6).map((category, i) => (
                  <motion.div
                    key={category.id}
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: i * 0.05 }}
                  >
                    <Link
                      to={`/shop/products?categoryId=${category.id}`}
                      className="block text-center group"
                    >
                      <div className="h-24 bg-gray-100 dark:bg-gray-800 rounded-xl flex items-center justify-center mb-3 group-hover:bg-emerald-50 dark:group-hover:bg-emerald-900/20 transition-colors overflow-hidden">
                        {category.image ? (
                          <img src={category.image} alt={category.name} className="w-full h-full object-cover" />
                        ) : (
                          <ShoppingBag className="w-10 h-10 text-gray-400 group-hover:text-emerald-600 transition-colors" />
                        )}
                      </div>
                      <p className="font-medium text-gray-900 dark:text-white group-hover:text-emerald-600 transition-colors">
                        {category.name}
                      </p>
                    </Link>
                  </motion.div>
                ))}
          </div>
        </div>
      </section>

      <section className="py-16 lg:py-24 bg-gray-50 dark:bg-gray-900">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white">
                Featured Products
              </h2>
              <p className="text-gray-500 dark:text-gray-400 mt-1">
                Top picks for you
              </p>
            </div>
            <Link
              to="/shop/products"
              className="text-emerald-600 hover:text-emerald-700 font-medium flex items-center gap-1"
            >
              View All
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 lg:gap-6">
            {loading
              ? [...Array(8)].map((_, i) => (
                  <div key={i} className="bg-white dark:bg-gray-800 rounded-xl overflow-hidden animate-pulse">
                    <div className="h-48 bg-gray-200 dark:bg-gray-700" />
                    <div className="p-4 space-y-2">
                      <div className="h-4 w-3/4 bg-gray-200 dark:bg-gray-700 rounded" />
                      <div className="h-4 w-1/2 bg-gray-200 dark:bg-gray-700 rounded" />
                    </div>
                  </div>
                ))
              : featuredProducts.map((product, i) => (
                  <motion.div
                    key={product.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.05 }}
                  >
                    <Link
                      to={`/shop/product/${product.slug}`}
                      className="block bg-white dark:bg-gray-800 rounded-xl overflow-hidden shadow-sm hover:shadow-lg transition-all group"
                    >
                      <div className="h-48 bg-gray-100 dark:bg-gray-700 relative overflow-hidden">
                        {product.images[0] ? (
                          <img
                            src={product.images[0]}
                            alt={product.name}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center">
                            <ShoppingBag className="w-12 h-12 text-gray-400" />
                          </div>
                        )}
                        {product.discountPrice && (
                          <span className="absolute top-2 left-2 px-2 py-1 bg-red-500 text-white text-xsfont-bold rounded">
                            SALE
                          </span>
                        )}
                      </div>
                      <div className="p-4">
                        <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">
                          {product.category?.name}
                        </p>
                        <h3 className="font-semibold text-gray-900 dark:text-white truncate group-hover:text-emerald-600 transition-colors">
                          {product.name}
                        </h3>
                        <div className="flex items-center gap-1 mt-2">
                          {[...Array(5)].map((_, j) => (
                            <Star
                              key={j}
                              className={`w-3 h-3 ${
                                j < Math.round(product.averageRating)
                                  ? 'fill-amber-400 text-amber-400'
                                  : 'text-gray-300'
                              }`}
                            />
                          ))}
                          <span className="text-xs text-gray-500 ml-1">({product.totalReviews})</span>
                        </div>
                        <div className="mt-3">
                          <p className="text-lg font-bold text-emerald-600">
                            {formatPrice(product.discountPrice || product.price)}
                          </p>
                          {product.discountPrice && (
                            <p className="text-sm text-gray-400 line-through">
                              {formatPrice(product.price)}
                            </p>
                          )}
                        </div>
                      </div>
                    </Link>
                  </motion.div>
                ))}
          </div>
        </div>
      </section>

      <section className="py-16 lg:py-24 bg-gradient-to-r from-emerald-600 to-teal-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <h2 className="text-3xl lg:text-4xl font-bold text-white mb-4">
              Ready to Start Selling?
            </h2>
            <p className="text-lg text-white/80 mb-8 max-w-xl mx-auto">
              Join thousands of sellers on MultiVend and reach millions of customers across Pakistan.
            </p>
            <Link
              to="/register?role=SELLER"
              className="inline-flex items-center justify-center px-8 py-4 bg-white text-emerald-600 rounded-xl font-semibold hover:bg-gray-100 transition-all shadow-lg"
            >
              Open Your Shop Today
              <ArrowRight className="ml-2 w-5 h-5" />
            </Link>
          </motion.div>
        </div>
      </section>
    </div>
  );
}
