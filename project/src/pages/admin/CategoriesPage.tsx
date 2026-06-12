import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Plus, Edit, Trash2, ChevronRight, ChevronDown, Folder } from 'lucide-react';
import api from '../../lib/api';
import type { Category, ApiResponse } from '../../types';
import { Card, Button, Modal, Input } from '../../components/ui';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const categorySchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  slug: z.string().min(2, 'Slug is required'),
  description: z.string().optional(),
  parentId: z.number().optional(),
});

type CategoryFormData = z.infer<typeof categorySchema>;

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [expandedCategories, setExpandedCategories] = useState<Set<number>>(new Set());

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
  });

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const response = await api.get<ApiResponse<Category[]>>('/api/categories');
      setCategories(response.data.data);
    } catch (error) {
      console.error('Failed to fetch categories:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingCategory(null);
    reset({ name: '', slug: '', description: '' });
    setIsModalOpen(true);
  };

  const handleEdit = (category: Category) => {
    setEditingCategory(category);
    reset({
      name: category.name,
      slug: category.slug,
      description: category.description || '',
      parentId: category.parentId,
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (categoryId: number) => {
    if (!confirm('Are you sure you want to delete this category?')) return;

    try {
      await api.delete(`/api/admin/categories/${categoryId}`);
      toast.success('Category deleted');
      fetchCategories();
    } catch (error) {
      toast.error('Failed to delete category');
    }
  };

  const onSubmit = async (data: CategoryFormData) => {
    try {
      if (editingCategory) {
        await api.put(`/api/admin/categories/${editingCategory.id}`, data);
        toast.success('Category updated');
      } else {
        await api.post('/api/admin/categories', data);
        toast.success('Category created');
      }
      setIsModalOpen(false);
      fetchCategories();
    } catch (error) {
      toast.error('Failed to save category');
    }
  };

  const toggleExpand = (categoryId: number) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(categoryId)) {
        next.delete(categoryId);
      } else {
        next.add(categoryId);
      }
      return next;
    });
  };

  const buildTree = (categories: Category[], parentId?: number): Category[] => {
    return categories
      .filter((cat) => cat.parentId === parentId)
      .map((cat) => ({
        ...cat,
        children: buildTree(categories, cat.id),
      }));
  };

  const renderCategory = (category: Category, level = 0) => (
    <div key={category.id} className="select-none">
      <motion.div
        initial={{ opacity: 0, x: -10 }}
        animate={{ opacity: 1, x: 0 }}
        className={`flex items-center justify-between p-4 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors ${
          level > 0 ? 'ml-8' : ''
        }`}
      >
        <div className="flex items-center gap-3">
          {category.children && category.children.length > 0 ? (
            <button
              onClick={() => toggleExpand(category.id)}
              className="p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
            >
              {expandedCategories.has(category.id) ? (
                <ChevronDown className="w-4 h-4 text-gray-500" />
              ) : (
                <ChevronRight className="w-4 h-4 text-gray-500" />
              )}
            </button>
          ) : (
            <div className="w-6" />
          )}
          <div className="w-10 h-10 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center">
            {category.image ? (
              <img src={category.image} alt={category.name} className="w-full h-full object-cover rounded-lg" />
            ) : (
              <Folder className="w-5 h-5 text-gray-400" />
            )}
          </div>
          <div>
            <p className="font-medium text-gray-900 dark:text-white">{category.name}</p>
            <p className="text-sm text-gray-500 dark:text-gray-400">{category.slug}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={() => handleEdit(category)}>
            <Edit className="w-4 h-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleDelete(category.id)}
            className="text-red-600 hover:bg-red-50"
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      </motion.div>
      {category.children && category.children.length > 0 && expandedCategories.has(category.id) && (
        <div className="border-l-2 border-gray-200 dark:border-gray-700 ml-5">
          {category.children.map((child) => renderCategory(child, level + 1))}
        </div>
      )}
    </div>
  );

  const categoryTree = buildTree(categories);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Category Management</h1>
          <p className="text-gray-500 dark:text-gray-400">
            Organize your product categories hierarchically
          </p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="w-4 h-4 mr-2" />
          Add Category
        </Button>
      </div>

      <Card>
        {loading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="animate-pulse flex items-center gap-4 p-4">
                <div className="w-10 h-10 bg-gray-200 dark:bg-gray-700 rounded-lg" />
                <div className="flex-1">
                  <div className="h-4 w-32 bg-gray-200 dark:bg-gray-700 rounded mb-2" />
                  <div className="h-3 w-24 bg-gray-200 dark:bg-gray-700 rounded" />
                </div>
              </div>
            ))}
          </div>
        ) : categories.length === 0 ? (
          <div className="text-center py-12">
            <Folder className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 dark:text-gray-400">No categories found</p>
            <Button onClick={handleCreate} className="mt-4">
              <Plus className="w-4 h-4 mr-2" />
              Add your first category
            </Button>
          </div>
        ) : (
          <div className="space-y-2">{categoryTree.map((cat) => renderCategory(cat))}</div>
        )}
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingCategory ? 'Edit Category' : 'Create Category'}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            {...register('name')}
            label="Name"
            placeholder="Category name"
            error={errors.name?.message}
          />
          <Input
            {...register('slug')}
            label="Slug"
            placeholder="category-slug"
            error={errors.slug?.message}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Description
            </label>
            <textarea
              {...register('description')}
              rows={3}
              placeholder="Category description (optional)"
              className="w-full px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">
              {editingCategory ? 'Update Category' : 'Create Category'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
