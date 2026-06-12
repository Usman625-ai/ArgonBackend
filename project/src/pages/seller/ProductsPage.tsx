import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Plus, Search, Edit, Trash2, ChevronLeft, ChevronRight, Package } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../../lib/api';
import type { PagedResponse, Product, ApiResponse } from '../../types';
import { Card, Button, Badge, Modal, Input } from '../../components/ui';
import { formatPrice } from '../../lib/utils';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const productSchema = z.object({
  name: z.string().min(2, 'Name is required'),
  description: z.string().min(10, 'Description is required'),
  price: z.number().min(1, 'Price must be positive'),
  discountPrice: z.number().optional(),
  stock: z.number().min(0, 'Stock cannot be negative'),
  categoryId: z.number().min(1, 'Category is required'),
  brand: z.string().optional(),
});

type ProductFormData = z.infer<typeof productSchema>;

export default function SellerProductsPage() {
  const [products, setProducts] = useState<PagedResponse<Product> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
  });

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async (page = 0) => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<PagedResponse<Product>>>(
        `/api/seller/products?page=${page}&size=12`
      );
      setProducts(response.data.data);
    } catch (error) {
      console.error('Failed to fetch products:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingProduct(null);
    reset({
      name: '',
      description: '',
      price: 0,
      stock: 0,
      categoryId: 0,
      brand: '',
    });
    setIsModalOpen(true);
  };

  const handleEdit = async (product: Product) => {
    setEditingProduct(product);
    reset({
      name: product.name,
      description: product.description,
      price: product.price,
      discountPrice: product.discountPrice,
      stock: product.stock,
      categoryId: product.category?.id || 0,
      brand: product.brand,
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (productId: number) => {
    if (!confirm('Are you sure you want to delete this product?')) return;

    try {
      await api.delete(`/api/seller/products/${productId}`);
      toast.success('Product deleted');
      fetchProducts(products?.number || 0);
    } catch (error) {
      toast.error('Failed to delete product');
    }
  };

  const onSubmit = async (data: ProductFormData) => {
    try {
      if (editingProduct) {
        await api.put(`/api/seller/products/${editingProduct.id}`, data);
        toast.success('Product updated');
      } else {
        await api.post('/api/seller/products', data);
        toast.success('Product created');
      }
      setIsModalOpen(false);
      fetchProducts(products?.number || 0);
    } catch (error) {
      toast.error('Failed to save product');
    }
  };

  const filteredProducts = products?.content.filter(
    (product) =>
      product.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      product.category?.name?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Products</h1>
          <p className="text-gray-500 dark:text-gray-400">Manage your product inventory</p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="w-4 h-4 mr-2" />
          Add Product
        </Button>
      </div>

      <Card>
        <div className="mb-6">
          <div className="relative max-w-md">
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

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {loading ? (
            [...Array(8)].map((_, i) => (
              <div key={i} className="bg-gray-50 dark:bg-gray-800 rounded-xl overflow-hidden animate-pulse">
                <div className="h-48 bg-gray-200 dark:bg-gray-700" />
                <div className="p-4 space-y-2">
                  <div className="h-4 w-3/4 bg-gray-200 dark:bg-gray-700 rounded" />
                  <div className="h-4 w-1/2 bg-gray-200 dark:bg-gray-700 rounded" />
                  <div className="h-6 w-1/3 bg-gray-200 dark:bg-gray-700 rounded" />
                </div>
              </div>
            ))
          ) : filteredProducts?.length === 0 ? (
            <div className="col-span-full text-center py-12">
              <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500 dark:text-gray-400">No products found</p>
              <Button onClick={handleCreate} className="mt-4">
                <Plus className="w-4 h-4 mr-2" />
                Add your first product
              </Button>
            </div>
          ) : (
            filteredProducts?.map((product) => (
              <motion.div
                key={product.id}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="bg-gray-50 dark:bg-gray-800 rounded-xl overflow-hidden hover:shadow-lg transition-shadow group"
              >
                <div className="h-48 bg-gray-200 dark:bg-gray-700 relative overflow-hidden">
                  {product.images[0] ? (
                    <img
                      src={product.images[0]}
                      alt={product.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <Package className="w-12 h-12 text-gray-400" />
                    </div>
                  )}
                  {product.stock < 10 && (
                    <div className="absolute top-2 right-2">
                      <Badge variant="warning">Low Stock</Badge>
                    </div>
                  )}
                </div>
                <div className="p-4">
                  <h3 className="font-semibold text-gray-900 dark:text-white truncate">
                    {product.name}
                  </h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                    {product.category?.name}
                  </p>
                  <div className="flex items-center justify-between mt-3">
                    <div>
                      <p className="font-bold text-gray-900 dark:text-white">
                        {formatPrice(product.discountPrice || product.price)}
                      </p>
                      {product.discountPrice && (
                        <p className="text-sm text-gray-400 line-through">
                          {formatPrice(product.price)}
                        </p>
                      )}
                    </div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {product.stock} in stock
                    </p>
                  </div>
                  <div className="flex items-center gap-2 mt-4">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1"
                      onClick={() => handleEdit(product)}
                    >
                      <Edit className="w-4 h-4 mr-1" />
                      Edit
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(product.id)}
                      className="text-red-600 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </motion.div>
            ))
          )}
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

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingProduct ? 'Edit Product' : 'Add Product'}
        size="lg"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            {...register('name')}
            label="Name"
            placeholder="Product name"
            error={errors.name?.message}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Description
            </label>
            <textarea
              {...register('description')}
              rows={3}
              placeholder="Product description"
              className="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            {errors.description && (
              <p className="text-red-500 text-sm mt-1">{errors.description.message}</p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              {...register('price', { valueAsNumber: true })}
              label="Price"
              type="number"
              placeholder="0"
              error={errors.price?.message}
            />
            <Input
              {...register('discountPrice', { valueAsNumber: true })}
              label="Discount Price (optional)"
              type="number"
              placeholder="0"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              {...register('stock', { valueAsNumber: true })}
              label="Stock"
              type="number"
              placeholder="0"
              error={errors.stock?.message}
            />
            <Input
              {...register('categoryId', { valueAsNumber: true })}
              label="Category ID"
              type="number"
              placeholder="1"
              error={errors.categoryId?.message}
            />
          </div>
          <Input
            {...register('brand')}
            label="Brand (optional)"
            placeholder="Brand name"
          />
          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">
              {editingProduct ? 'Update Product' : 'Create Product'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
