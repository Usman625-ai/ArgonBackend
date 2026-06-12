import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Search,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  Star,
  LayoutGrid,
  List,
  ShoppingBag,
} from 'lucide-react';
import api from '../../lib/api';
import type { Product, Category, ApiResponse, PagedResponse } from '../../types';
import { formatPrice } from '../../lib/utils';
import { useAppDispatch } from '../../store';
import { addToCart } from '../../store/cartSlice';
import { toast } from 'sonner';
import { Button } from '../../components/ui';

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState<PagedResponse<Product> | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  const dispatch = useAppDispatch();

  const q = searchParams.get('q') || '';
  const categoryId = searchParams.get('categoryId') || '';
  const sortBy = searchParams.get('sortBy') || 'createdAt';
  const sortDir = searchParams.get('sortDir') || 'desc';
  const page = parseInt(searchParams.get('page') || '0', 10);

  useEffect(() => {
    fetchData();
  }, [searchParams]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (q) params.append('q', q);
      if (categoryId) params.append('categoryId', categoryId);
      params.append('sortBy', sortBy);
      params.append('sortDir', sortDir);
      params.append('page', page.toString());
      params.append('size', '12');

      const [productsRes, categoriesRes] = await Promise.all([
        api.get<ApiResponse<PagedResponse<Product>>>(`/api/products?${params.toString()}`),
        api.get<ApiResponse<Category[]>>('/api/categories'),
      ]);
      setProducts(productsRes.data.data);
      setCategories(categoriesRes.data.data);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setLoading(false);
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

  const updateFilter = (key: string, value: string | number) => {
    const newParams = new URLSearchParams(searchParams);
    if (value) {
      newParams.set(key, value.toString());
    } else {
      newParams.delete(key);
    }
    if (key !== 'page') newParams.set('page', '0');
    setSearchParams(newParams);
  };

  const ProductCard = ({ product }: { product: Product }) => (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-white dark:bg-gray-800 rounded-xl overflow-hidden shadow-sm hover:shadow-lg transition-all group"
    >
      <Link to={`/shop/product/${product.slug}`} className="block relative">
        <div className="h-48 bg-gray-100 dark:bg-gray-700 overflow-hidden">
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
        </div>
        {product.discountPrice && (
          <span className="absolute top-2 left-2 px-2 py-1 bg-red-500 text-white text-xs font-bold rounded">
            -{Math.round((1 - product.discountPrice / product.price) * 100)}%
          </span>
        )}
      </Link>
      <div className="p-4">
        <Link to={`/shop/product/${product.slug}`}>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{product.category?.name}</p>
          <h3 className="font-semibold text-gray-900 dark:text-white truncate group-hover:text-emerald-600 transition-colors">
            {product.name}
          </h3>
        </Link>
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
        <div className="flex items-center justify-between mt-3">
          <div>
            <p className="text-lg font-bold text-emerald-600">
              {formatPrice(product.discountPrice || product.price)}
            </p>
            {product.discountPrice && (
              <p className="text-sm text-gray-400 line-through">{formatPrice(product.price)}</p>
            )}
          </div>
          <Button size="sm" variant="outline" onClick={() => handleAddToCart(product)}>
            Add
          </Button>
        </div>
      </div>
    </motion.div>
  );

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col lg:flex-row gap-8">
          <aside className="hidden lg:block w-64 flex-shrink-0">
            <div className="bg-white dark:bg-gray-800 rounded-xl p-6 sticky top-24">
              <h3 className="font-semibold text-gray-900 dark:text-white mb-4">Categories</h3>
              <div className="space-y-2">
                <button
                  onClick={() => updateFilter('categoryId', '')}
                  className={`w-full text-left px-3 py-2 rounded-lg transition-colors ${
                    !categoryId
                      ? 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600'
                      : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  All Categories
                </button>
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => updateFilter('categoryId', cat.id)}
                    className={`w-full text-left px-3 py-2 rounded-lg transition-colors ${
                      categoryId === cat.id.toString()
                        ? 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600'
                        : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                    }`}
                  >
                    {cat.name}
                  </button>
                ))}
              </div>

              <hr className="my-6 border-gray-200 dark:border-gray-700" />

              <h3 className="font-semibold text-gray-900 dark:text-white mb-4">Sort By</h3>
              <select
                value={`${sortBy}-${sortDir}`}
                onChange={(e) => {
                  const [newSort, newDir] = e.target.value.split('-');
                  updateFilter('sortBy', newSort);
                  updateFilter('sortDir', newDir);
                }}
                className="w-full px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white"
              >
                <option value="createdAt-desc">Newest First</option>
                <option value="createdAt-asc">Oldest First</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
                <option value="averageRating-desc">Highest Rated</option>
              </select>
            </div>
          </aside>

          <div className="flex-1">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
              <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                  {q ? `Search: "${q}"` : 'All Products'}
                </h1>
                <p className="text-gray-500 dark:text-gray-400">
                  {products ? `${products.totalElements} products found` : 'Loading...'}
                </p>
              </div>
              <div className="flex items-center gap-3 w-full sm:w-auto">
                <div className="relative flex-1 sm:w-64">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="text"
                    defaultValue={q}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        updateFilter('q', (e.target as HTMLInputElement).value);
                      }
                    }}
                    placeholder="Search products..."
                    className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                  />
                </div>
                <div className="flex items-center gap-1 bg-white dark:bg-gray-800 p-1 rounded-lg border border-gray-200 dark:border-gray-700">
                  <button
                    onClick={() => setViewMode('grid')}
                    className={`p-2 rounded ${viewMode === 'grid' ? 'bg-gray-100 dark:bg-gray-700' : ''}`}
                  >
                    <LayoutGrid className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setViewMode('list')}
                    className={`p-2 rounded ${viewMode === 'list' ? 'bg-gray-100 dark:bg-gray-700' : ''}`}
                  >
                    <List className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>

            <div className="lg:hidden mb-6">
              <div className="flex gap-2 overflow-x-auto pb-2">
                <button
                  onClick={() => updateFilter('categoryId', '')}
                  className={`px-4 py-2 rounded-full text-sm whitespace-nowrap ${
                    !categoryId
                      ? 'bg-emerald-600 text-white'
                      : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400'
                  }`}
                >
                  All
                </button>
                {categories.slice(0, 6).map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => updateFilter('categoryId', cat.id)}
                    className={`px-4 py-2 rounded-full text-sm whitespace-nowrap ${
                      categoryId === cat.id.toString()
                        ? 'bg-emerald-600 text-white'
                        : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    {cat.name}
                  </button>
                ))}
              </div>
            </div>

            {loading ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-3 gap-4 lg:gap-6">
                {[...Array(12)].map((_, i) => (
                  <div key={i} className="bg-white dark:bg-gray-800 rounded-xl overflow-hidden animate-pulse">
                    <div className="h-48 bg-gray-200 dark:bg-gray-700" />
                    <div className="p-4 space-y-2">
                      <div className="h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
                      <div className="h-4 w-3/4 bg-gray-200 dark:bg-gray-700 rounded" />
                      <div className="h-6 w-1/3 bg-gray-200 dark:bg-gray-700 rounded" />
                    </div>
                  </div>
                ))}
              </div>
            ) : products?.content.length === 0 ? (
              <div className="text-center py-12">
                <ShoppingBag className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500 dark:text-gray-400">No products found</p>
              </div>
            ) : (
              <div
                className={
                  viewMode === 'grid'
                    ? 'grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-3 gap-4 lg:gap-6'
                    : 'space-y-4'
                }
              >
                {products?.content.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            )}

            {products && products.totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-8">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => updateFilter('page', page - 1)}
                  disabled={products.first}
                >
                  <ChevronLeft className="w-4 h-4" />
                </Button>
                <span className="text-sm text-gray-600 dark:text-gray-400">
                  Page {products.number + 1} of {products.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => updateFilter('page', page + 1)}
                  disabled={products.last}
                >
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
