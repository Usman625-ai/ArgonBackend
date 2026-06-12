import { Link } from 'react-router-dom';
import { Facebook, Twitter, Instagram, Youtube, Mail, Phone, MapPin } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center space-x-2 mb-4">
              <div className="w-10 h-10 bg-gradient-to-br from-emerald-500 to-teal-600 rounded-xl flex items-center justify-center">
                <span className="text-white font-bold text-xl">M</span>
              </div>
              <span className="text-xl font-bold text-white">MultiVend</span>
            </div>
            <p className="text-gray-400 text-sm mb-4">
              Your one-stop destination for quality products from verified sellers.
            </p>
            <div className="flex space-x-4">
              <a href="#" className="hover:text-emerald-400 transition-colors">
                <Facebook className="w-5 h-5" />
              </a>
              <a href="#" className="hover:text-emerald-400 transition-colors">
                <Twitter className="w-5 h-5" />
              </a>
              <a href="#" className="hover:text-emerald-400 transition-colors">
                <Instagram className="w-5 h-5" />
              </a>
              <a href="#" className="hover:text-emerald-400 transition-colors">
                <Youtube className="w-5 h-5" />
              </a>
            </div>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">Quick Links</h3>
            <ul className="space-y-2">
              <li>
                <Link to="/shop" className="hover:text-emerald-400 transition-colors text-sm">
                  Home
                </Link>
              </li>
              <li>
                <Link to="/shop/products" className="hover:text-emerald-400 transition-colors text-sm">
                  Products
                </Link>
              </li>
              <li>
                <Link to="/shop/categories" className="hover:text-emerald-400 transition-colors text-sm">
                  Categories
                </Link>
              </li>
              <li>
                <Link to="/about" className="hover:text-emerald-400 transition-colors text-sm">
                  About Us
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">Customer Support</h3>
            <ul className="space-y-2">
              <li>
                <Link to="/help" className="hover:text-emerald-400 transition-colors text-sm">
                  Help Center
                </Link>
              </li>
              <li>
                <Link to="/shipping" className="hover:text-emerald-400 transition-colors text-sm">
                  Shipping Info
                </Link>
              </li>
              <li>
                <Link to="/returns" className="hover:text-emerald-400 transition-colors text-sm">
                  Returns & Refunds
                </Link>
              </li>
              <li>
                <Link to="/faq" className="hover:text-emerald-400 transition-colors text-sm">
                  FAQs
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">Contact Us</h3>
            <ul className="space-y-3">
              <li className="flex items-center space-x-3 text-sm">
                <MapPin className="w-4 h-4 text-emerald-400" />
                <span>123 Commerce Street, Karachi, Pakistan</span>
              </li>
              <li className="flex items-center space-x-3 text-sm">
                <Phone className="w-4 h-4 text-emerald-400" />
                <span>+92 21 1234567</span>
              </li>
              <li className="flex items-center space-x-3 text-sm">
                <Mail className="w-4 h-4 text-emerald-400" />
                <span>support@multivend.pk</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-800 mt-8 pt-8 flex flex-col md:flex-row justify-between items-center">
          <p className="text-sm text-gray-400">
            {new Date().getFullYear()} MultiVend. All rights reserved.
          </p>
          <div className="flex space-x-6 mt-4 md:mt-0">
            <Link to="/privacy" className="text-sm hover:text-emerald-400 transition-colors">
              Privacy Policy
            </Link>
            <Link to="/terms" className="text-sm hover:text-emerald-400 transition-colors">
              Terms of Service
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
