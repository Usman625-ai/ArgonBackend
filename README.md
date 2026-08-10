# Argon – Multi-Vendor E-Commerce Backend

Spring Boot 3.x production-ready backend for a multi‑vendor e‑commerce platform with **JWT authentication**, **JazzCash** payments, **Cloudinary** image storage, and **Excel reporting**.

---

## ✨ Features

- 🔐 **JWT Authentication** (access + refresh tokens)  
- 👥 **Three roles** – Admin, Seller, Customer  
- 🛍️ **Product management** – CRUD, categories, stock, image upload (Cloudinary)  
- 🛒 **Shopping cart** – Add/remove, quantity, persistent  
- 💸 **JazzCash payment gateway** – Sandbox & live support, signature verification  
- 📦 **Order workflow** – Pending → Processing → Shipped → Delivered / Cancelled  
- 🎟️ **Coupon system** – Percentage / fixed, min order, usage limits  
- ⭐ **Product reviews & ratings**  
- ❤️ **Wishlist**  
- 📧 **Email notifications** (Gmail SMTP) – OTP, order updates, seller approval  
- 📊 **Excel reports** (Apache POI) – Sales reports for admin & sellers  
- 🖼️ **Image compression** (Thumbnailator)  
- 🤖 **Scheduled tasks** – Auto‑cancel stale COD orders, clean reset tokens  

---

## 🧱 Tech Stack

| Layer          | Technology                                       |
|----------------|--------------------------------------------------|
| Framework      | Spring Boot 3.2+                                 |
| Language       | Java 17                                          |
| Database       | MySQL 8.x                                        |
| Auth           | Spring Security 6 + JWT (jjwt 0.11.5)            |
| ORM            | Spring Data JPA + Hibernate                      |
| Payments       | **JazzCash** (Pakistan)                          |
| Images         | Cloudinary + Thumbnailator                       |
| Reports        | Apache POI 5.x (Excel .xlsx)                     |
| Email          | JavaMailSender (Gmail SMTP)                      |
| Boilerplate    | Lombok                                           |
| Build          | Maven 3.8+                                       |
| Scheduling     | Spring `@Scheduled`                              |

---

## 🚀 Quick Start

### Prerequisites

- Java 17+ (tested with 21)
- MySQL 8.x
- Maven 3.8+
