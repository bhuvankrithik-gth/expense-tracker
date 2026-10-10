package com.ledgertracker.service;

import com.ledgertracker.model.Transaction;
import com.ledgertracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExportService {

    private final TransactionRepository transactionRepository;

    public ExportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public String exportTransactionsToCsv(Long userId, LocalDate startDate, LocalDate endDate, Long categoryId, String type) {
        List<Transaction> transactions = transactionRepository.findAllForExport(userId, startDate, endDate, categoryId, type);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // CSV Header
        pw.println("Date,Type,Category,Amount,Payment Method,Note,Created At");

        for (Transaction tx : transactions) {
            StringBuilder line = new StringBuilder();
            line.append(escapeCsv(tx.getTransactionDate().toString())).append(",");
            line.append(escapeCsv(tx.getType())).append(",");
            line.append(escapeCsv(tx.getCategoryName() != null ? tx.getCategoryName() : "")).append(",");
            line.append(tx.getAmount().toPlainString()).append(",");
            line.append(escapeCsv(tx.getPaymentMethod())).append(",");
            line.append(escapeCsv(tx.getNote() != null ? tx.getNote() : "")).append(",");
            line.append(escapeCsv(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""));
            pw.println(line);
        }

        pw.flush();
        return sw.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
