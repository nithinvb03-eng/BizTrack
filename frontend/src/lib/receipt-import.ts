import type { Transaction, TransactionRequest } from "@/generated/model";
import { customInstance } from "@/lib/api-client";

export type ReceiptDetails = {
  amount: string;
  transactionDate: string;
  transactionTime: string;
  description: string;
  categoryId: string;
  transactionType: "INCOME" | "EXPENSE";
  paymentMethod: string;
  merchant: string;
  currency: string;
  category: string;
  transactionId: string;
  taxAmount: string;
  receiptReference: string;
  confidence: number | null;
};

type ExtractedTransaction = {
  merchant: string | null;
  amount: number | null;
  currency: string | null;
  date: string | null;
  time: string | null;
  description: string | null;
  category: string | null;
  paymentMethod: string | null;
  transactionId: string | null;
  taxAmount: number | null;
  receiptReference: string | null;
  transactionType: "INCOME" | "EXPENSE" | null;
  confidence: number | null;
};

type ReceiptExtractionResponse = {
  success: boolean;
  message: string;
  fileName: string;
  extractedText: string;
  transactions: ExtractedTransaction[];
};

export async function extractReceiptDetails(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  const response = await customInstance<ReceiptExtractionResponse>(
    "/api/transactions/upload-and-extract",
    { method: "POST", body: formData },
  );
  return response.transactions.map(
    (transaction): ReceiptDetails => ({
      amount: transaction.amount == null ? "" : transaction.amount.toFixed(2),
      currency: transaction.currency ?? "",
      transactionDate: transaction.date ?? "",
      transactionTime: transaction.time ?? "",
      description: transaction.description ?? transaction.merchant ?? "",
      categoryId: "",
      category: transaction.category ?? "",
      transactionType: transaction.transactionType ?? "EXPENSE",
      paymentMethod: transaction.paymentMethod ?? "",
      merchant: transaction.merchant ?? "",
      transactionId: transaction.transactionId ?? "",
      taxAmount:
        transaction.taxAmount == null ? "" : transaction.taxAmount.toFixed(2),
      receiptReference: transaction.receiptReference ?? "",
      confidence: transaction.confidence,
    }),
  );
}

export async function addTransactionWithReceipt(
  transaction: TransactionRequest,
  file: File,
) {
  const formData = new FormData();
  formData.append(
    "transaction",
    new Blob([JSON.stringify(transaction)], { type: "application/json" }),
    "transaction.json",
  );
  formData.append("receipt", file);
  return customInstance<Transaction>("/api/transactions/with-receipt", {
    method: "POST",
    body: formData,
  });
}
