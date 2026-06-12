import { useState } from 'react';
import { motion } from 'framer-motion';
import { Download, Calendar, FileSpreadsheet } from 'lucide-react';
import { Card, Button, Input } from '../../components/ui';
import { toast } from 'sonner';
import api from '../../lib/api';
import type { ApiResponse } from '../../types';

export default function ReportsPage() {
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    if (!fromDate || !toDate) {
      toast.error('Please select both from and to dates');
      return;
    }

    setDownloading(true);
    try {
      const response = await api.get('/api/admin/reports/sales', {
        params: {
          from: new Date(fromDate).toISOString(),
          to: new Date(toDate).toISOString(),
        },
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `sales_report_${fromDate}_${toDate}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      toast.success('Report downloaded successfully');
    } catch (error) {
      toast.error('Failed to generate report');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Reports</h1>
        <p className="text-gray-500 dark:text-gray-400">
          Download sales reports and analytics
        </p>
      </div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <Card>
          <div className="flex items-center gap-4 mb-6">
            <div className="w-12 h-12 bg-emerald-100 dark:bg-emerald-900/30 rounded-xl flex items-center justify-center">
              <FileSpreadsheet className="w-6 h-6 text-emerald-600" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Sales Report
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Download detailed sales report in Excel format
              </p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  From Date
                </label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type="date"
                    value={fromDate}
                    onChange={(e) => setFromDate(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  To Date
                </label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type="date"
                    value={toDate}
                    onChange={(e) => setToDate(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>
            </div>

            <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                The report will include:
              </p>
              <ul className="mt-2 space-y-1 text-sm text-gray-600 dark:text-gray-400">
                <li>Order details (number, date, status)</li>
                <li>Customer information</li>
                <li>Product breakdown per order</li>
                <li>Revenue calculations</li>
                <li>Seller performance metrics</li>
              </ul>
            </div>

            <Button
              onClick={handleDownload}
              isLoading={downloading}
              className="w-full"
              size="lg"
            >
              <Download className="w-4 h-4 mr-2" />
              Download Sales Report
            </Button>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
