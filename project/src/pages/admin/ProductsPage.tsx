import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Search, ChevronLeft, ChevronRight, ToggleLeft, ToggleRight } from 'lucide-react';
import api from '../../lib/api';
import type { PagedResponse, Product, ApiResponse } from '../../types';
import { Card, Button, Badge } from '../../components/ui';
import { formatPrice } from '../../lib/utils';
import { toast } from 'sonner';

export default function ProductsPage() {
  const [products, setProducts] = useState<PagedResponse<Product> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async (page = 0) => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<PagedResponse<Product>>>(
        `/api/products?page=${page}&size=12`
      );
      setProducts(response.data.data);
    } catch (error) {
      console.error('Failed to fetch products:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleActive = async (product: Product) => {
    try {
      await api.put(`/api/admin/products/${product.id}/status?active=${!product.active}`);
      toast.success(`Product ${!product.active ? 'activated' : 'deactivated'}`);
      fetchProducts(products?.number || 0);
    } catch (error) {
      toast.error('Failed to update product status');
    }
  };

  const filteredProducts = products?.content.filter(
    (product) =>
      product.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      product.seller?.name?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Product Management</h1>
        <p className="text-gray-500 dark:text-gray-400">
          View and manage all products on the platform
        </p>
      </div>

      <Card>
        <div className="mb-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products..."
                className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Product
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Category
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Seller
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Price
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Stock
                </th>
                <th className="text-left py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Status
                </th>
                <th className="text-right py-4 px-4 text-sm font-semibold text-gray-700 dark:text-gray-300">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="border-b border-gray-100 dark:border-gray-800">
                    {[...Array(7)].map((_, j) => (
                      <td key={j} className="py-4 px-4">
                        <div className="animate-pulse h-4 w-20 bg-gray-200 dark:bg-gray-700 rounded" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : filteredProducts?.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-gray-500 dark:text-gray-400">
                    No products found
                  </td>
                </tr>
              ) : (
                filteredProducts?.map((product) => (
                  <motion.tr
                    key={product.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center overflow-hidden">
                          {product.images[0] ? (
                            <img
                              src={product.images[0]}
                              alt={product.name}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <span className="text-gray-400">IMG</span>
                          )}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900 dark:text-white">
                            {product.name}
                          </p>
                          <p className="text-sm text-gray-500 dark:text-gray-400">{product.brand}</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <p className="text-gray-900 dark:text-white">{product.category?.name}</p>
                    </td>
                    <td className="py-4 px-4">
                      <p className="text-gray-900 dark:text-white">{product.seller?.name}</p>
                    </td>
                    <td className="py-4 px-4">
                      <p className="font-medium text-gray-900 dark:text-white">
                        {formatPrice(product.discountPrice || product.price)}
                      </p>
                      {product.discountPrice && (
                        <p className="text-sm text-gray-400 line-through">
                          {formatPrice(product.price)}
                        </p>
                      )}
                    </td>
                    <td className="py-4 px-4">
                      <Badge variant={product.stock < 10 ? 'warning' : product.stock > 0 ? 'success' : 'error'}>
                        {product.stock} in stock
                      </Badge>
                    </td>
                    <td className="py-4 px-4">
                      <Badge variant={product.active ? 'success' : 'default'}>
                        {product.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex justify-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleToggleActive(product)}
                        >
                          {product.active ? (
                            <ToggleRight className="w-5 h-5 text-emerald-500" />
                          ) : (
                            <ToggleLeft className="w-5 h-5 text-gray-400" />
                          )}
                        </Button>
                      </div>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {products && products.totalPages > 1 && (
          <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200 dark:border-gray-700">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Showing {products.number * products.size + 1} to{' '}
              {Math.min((products.number + 1) * products.size, products.totalElements)} of{' '}
              {products.totalElements} products
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchProducts(products.number - 1)}
                disabled={products.first}
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchProducts(products.number + 1)}
                disabled={products.last}
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
