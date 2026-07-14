package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.entity.Order;
import com.ecommerce.multivendor.entity.OrderItem;
import com.ecommerce.multivendor.exception.BadRequestException;
import com.ecommerce.multivendor.repository.OrderItemRepository;
import com.ecommerce.multivendor.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    // ─── Admin: Sales Report ───────────────────────────────────────────────

    /**
     * Generate platform-wide sales report for the given date range.
     * Returns raw bytes of the .xlsx file.
     */

    public byte[] generateAdminSalesReport(LocalDate fromDt, LocalDate toDt) {
        LocalDateTime from = fromDt.atStartOfDay();           // 2026-06-11T00:00:00
        LocalDateTime to = toDt.atTime(23, 59, 59);         // 2026-07-11T23:59:59
        List<Order> orders = orderRepository.findByDateRange(from, to);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Sheet 1: Orders Summary
            Sheet ordersSheet = workbook.createSheet("Orders");
            buildAdminOrdersSheet(workbook, ordersSheet, orders);

            // Sheet 2: Revenue Summary
            Sheet revenueSheet = workbook.createSheet("Revenue Summary");
            buildRevenueSummarySheet(workbook, revenueSheet, orders, from, to);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            log.info("Admin sales report generated: {} orders", orders.size());
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate report: " + e.getMessage());
        }
    }

    // ─── Seller: Sales Report ──────────────────────────────────────────────

    /**
     * Generate seller-specific sales report.
     */
    public byte[] generateSellerSalesReport(Long sellerId, LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findBySellerAndDateRange(sellerId, from, to);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("My Sales");
            buildSellerSalesSheet(workbook, sheet, orders);

            Sheet summarySheet = workbook.createSheet("Summary");
            buildRevenueSummarySheet(workbook, summarySheet, orders, from, to);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            log.info("Seller {} sales report generated: {} orders", sellerId, orders.size());
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate report: " + e.getMessage());
        }
    }

    // ─── Admin Orders Sheet ────────────────────────────────────────────────

    private void buildAdminOrdersSheet(XSSFWorkbook workbook, Sheet sheet, List<Order> orders) {
        // Title row
        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Platform Sales Report");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

        // Header row
        String[] headers = {
            "Order #", "Date", "Customer", "Seller", "Shop",
            "Items", "Subtotal", "Discount", "Shipping", "Total", "Payment Method",
            "Payment Status", "Order Status"
        };
        Row headerRow = sheet.createRow(2);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        CellStyle numStyle = createNumberStyle(workbook);
        CellStyle altStyle = createAlternateRowStyle(workbook);
        int rowNum = 3;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Order order : orders) {
            Row row = sheet.createRow(rowNum++);
            CellStyle rowStyle = (rowNum % 2 == 0) ? altStyle : null;

            setCell(row, 0, order.getOrderNumber(), rowStyle);
            setCell(row, 1, order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_FMT) : "", rowStyle);
            setCell(row, 2, order.getCustomer().getName(), rowStyle);
            setCell(row, 3, order.getSeller().getName(), rowStyle);
            setCell(row, 4, order.getSeller().getShopName(), rowStyle);
            setCell(row, 5, order.getOrderItems().size(), rowStyle);

            setNumericCell(row, 6, order.getSubtotalAmount(), numStyle);
            setNumericCell(row, 7, order.getDiscountAmount(), numStyle);
            setNumericCell(row, 8, order.getShippingAmount(), numStyle);
            setNumericCell(row, 9, order.getFinalAmount(), numStyle);
            setCell(row, 10, order.getPaymentMethod().name(), rowStyle);
            setCell(row, 11, order.getPaymentStatus().name(), rowStyle);
            setCell(row, 12, order.getOrderStatus().name(), rowStyle);

            grandTotal = grandTotal.add(order.getFinalAmount());
        }

        // Totals row
        Row totalRow = sheet.createRow(rowNum + 1);
        CellStyle totalStyle = createTotalStyle(workbook);
        Cell totalLabel = totalRow.createCell(8);
        totalLabel.setCellValue("GRAND TOTAL:");
        totalLabel.setCellStyle(totalStyle);
        Cell totalCell = totalRow.createCell(9);
        totalCell.setCellValue(grandTotal.doubleValue());
        totalCell.setCellStyle(totalStyle);

        // Auto-size columns
        for (int i = 0; i <= 12; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ─── Seller Sales Sheet ────────────────────────────────────────────────

    private void buildSellerSalesSheet(XSSFWorkbook workbook, Sheet sheet, List<Order> orders) {
        String[] headers = {
            "Order #", "Date", "Customer", "Product", "Quantity",
            "Unit Price", "Item Total", "Order Total", "Payment Method",
            "Payment Status", "Order Status"
        };
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle numStyle = createNumberStyle(workbook);
        int rowNum = 1;

        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                Row row = sheet.createRow(rowNum++);
                setCell(row, 0, order.getOrderNumber(), null);
                setCell(row, 1, order.getCreatedAt().format(DATE_FMT), null);
                setCell(row, 2, order.getCustomer().getName(), null);
                setCell(row, 3, item.getProductName(), null);
                setCell(row, 4, item.getQuantity(), null);
                setNumericCell(row, 5, item.getUnitPrice(), numStyle);
                setNumericCell(row, 6, item.getTotalPrice(), numStyle);
                setNumericCell(row, 7, order.getFinalAmount(), numStyle);
                setCell(row, 8, order.getPaymentMethod().name(), null);
                setCell(row, 9, order.getPaymentStatus().name(), null);
                setCell(row, 10, order.getOrderStatus().name(), null);
            }
        }

        for (int i = 0; i <= 10; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ─── Revenue Summary Sheet ────────────────────────────────────────────

    private void buildRevenueSummarySheet(XSSFWorkbook workbook, Sheet sheet,
                                           List<Order> orders, LocalDateTime from, LocalDateTime to) {
        CellStyle labelStyle = createHeaderStyle(workbook);
        CellStyle numStyle   = createNumberStyle(workbook);

        int row = 0;
        setHeaderCell(sheet, row++, 0, "Revenue Summary", labelStyle);
        setHeaderCell(sheet, row++, 0, "Period: " + from.format(DATE_FMT)
            + " to " + to.format(DATE_FMT), null);
        row++;

        long totalOrders   = orders.size();
        long paidOrders    = orders.stream()
            .filter(o -> o.getPaymentStatus().name().equals("PAID")).count();
        BigDecimal revenue = orders.stream()
            .filter(o -> o.getPaymentStatus().name().equals("PAID"))
            .map(Order::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = orders.stream()
            .map(Order::getDiscountAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Object[][] summaryData = {
            {"Total Orders", totalOrders},
            {"Paid Orders", paidOrders},
            {"Total Revenue (₹)", revenue},
            {"Total Discounts Given (₹)", totalDiscount},
            {"Average Order Value (₹)", paidOrders > 0
                ? revenue.divide(BigDecimal.valueOf(paidOrders), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO},
        };

        for (Object[] data : summaryData) {
            Row r = sheet.createRow(row++);
            Cell label = r.createCell(0);
            label.setCellValue((String) data[0]);
            label.setCellStyle(labelStyle);
            Cell value = r.createCell(1);
            if (data[1] instanceof BigDecimal bd) {
                value.setCellValue(bd.doubleValue());
                value.setCellStyle(numStyle);
            } else {
                value.setCellValue(((Number) data[1]).longValue());
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    // ─── Style helpers ─────────────────────────────────────────────────────

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createAlternateRowStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTotalStyle(XSSFWorkbook wb) {
        CellStyle style = createNumberStyle(wb);
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // ─── Cell setters ──────────────────────────────────────────────────────

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof String s) cell.setCellValue(s);
        else if (value instanceof Integer i) cell.setCellValue(i);
        else if (value instanceof Long l) cell.setCellValue(l);
        else if (value != null) cell.setCellValue(value.toString());
        if (style != null) cell.setCellStyle(style);
    }

    private void setNumericCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        if (style != null) cell.setCellStyle(style);
    }

    private void setHeaderCell(Sheet sheet, int rowNum, int col, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    /** Generate filename for download. */
    public String generateReportFilename(String type) {
        return type + "_report_" + LocalDateTime.now().format(FILE_DATE) + ".xlsx";
    }
}
