import { customDownload, customInstance } from "@/lib/api-client";

export type CreditTakerType = "CUSTOMER" | "RETAILER" | "OTHER";

export interface CreditTaker {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  type: CreditTakerType;
  totalGiven: number;
  entryCount: number;
  lastEntryDate: string | null;
}

export interface CreditTakerOverview {
  totalGiven: number;
  creditTakerCount: number;
}

export interface CreditEntry {
  id: number;
  creditTakerId: number;
  amount: number;
  entryDate: string;
  entryTime: string;
  note: string | null;
}

export interface CreditDay {
  date: string;
  entries: CreditEntry[];
  count: number;
  dayTotal: number;
}

export interface CreditDateSummary {
  date: string;
  count: number;
  total: number;
}

export interface CreditDaily {
  from: string;
  to: string;
  days: CreditDateSummary[];
  grandTotal: number;
}

export interface CreditMonthSummary {
  month: number;
  count: number;
  total: number;
}

export interface CreditMonthly {
  year: number;
  months: CreditMonthSummary[];
  yearTotal: number;
}

export interface CreditReceiptEntry {
  date: string;
  time: string;
  amount: number;
  note: string | null;
}

export interface CreditReceipt {
  id: number | null;
  businessName: string;
  businessGstin: string | null;
  receiptNo: string;
  generatedAt: string;
  generatedBy: string;
  creditTakerId: number;
  creditTakerName: string;
  phone: string | null;
  type: CreditTakerType;
  periodFrom: string;
  periodTo: string;
  entries: CreditReceiptEntry[];
  entryCount: number;
  totalAmount: number;
}

export interface CreditReceiptListItem {
  id: number;
  receiptNo: string;
  periodFrom: string;
  periodTo: string;
  totalAmount: number;
  entryCount: number;
  generatedAt: string;
}

export interface CreditTakerRequest {
  name: string;
  phone?: string;
  email?: string;
  type?: CreditTakerType;
}

export interface CreditEntryRequest {
  amount: number;
  entryDate: string;
  entryTime: string;
  note?: string;
}

function queryString(params: Record<string, string | number>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    query.set(key, String(value));
  });
  return query.toString();
}

export function listCreditTakers(search: string) {
  const query = search.trim() ? `?${queryString({ q: search.trim() })}` : "";
  return customInstance<CreditTaker[]>(`/api/credit-takers${query}`);
}

export function getCreditTaker(id: number) {
  return customInstance<CreditTaker>(`/api/credit-takers/${id}`);
}

export function getCreditTakerOverview() {
  return customInstance<CreditTakerOverview>("/api/credit-takers/overview");
}

export function createCreditTaker(data: CreditTakerRequest) {
  return customInstance<CreditTaker>("/api/credit-takers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export function updateCreditTaker(id: number, data: CreditTakerRequest) {
  return customInstance<CreditTaker>(`/api/credit-takers/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export function deleteCreditTaker(id: number) {
  return customInstance<void>(`/api/credit-takers/${id}`, { method: "DELETE" });
}

export function getCreditDay(id: number, date: string) {
  return customInstance<CreditDay>(
    `/api/credit-takers/${id}/day?${queryString({ date })}`,
  );
}

export function getCreditDaily(id: number, from: string, to: string) {
  return customInstance<CreditDaily>(
    `/api/credit-takers/${id}/daily?${queryString({ from, to })}`,
  );
}

export function getCreditMonthly(id: number, year: number) {
  return customInstance<CreditMonthly>(
    `/api/credit-takers/${id}/monthly?${queryString({ year })}`,
  );
}

export function createCreditEntry(id: number, data: CreditEntryRequest) {
  return customInstance<CreditEntry>(`/api/credit-takers/${id}/entries`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export function updateCreditEntry(id: number, data: CreditEntryRequest) {
  return customInstance<CreditEntry>(`/api/credit-entries/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export function deleteCreditEntry(id: number) {
  return customInstance<void>(`/api/credit-entries/${id}`, {
    method: "DELETE",
  });
}

export function getCreditReceiptPreview(id: number, from: string, to: string) {
  return customInstance<CreditReceipt>(
    `/api/credit-takers/${id}/receipt-preview?${queryString({ from, to })}`,
  );
}

export function generateCreditReceipt(id: number, from: string, to: string) {
  return customInstance<CreditReceipt>(
    `/api/credit-takers/${id}/receipt?${queryString({ from, to })}`,
  );
}

export function listCreditReceipts(id: number) {
  return customInstance<CreditReceiptListItem[]>(
    `/api/credit-takers/${id}/receipts`,
  );
}

export function emailCreditReceipt(id: number, toEmail: string) {
  return customInstance<void>(`/api/credit-receipts/${id}/email`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ toEmail }),
  });
}

export function downloadCreditReceipt(id: number) {
  return customDownload(`/api/credit-receipts/${id}/pdf`);
}

export function downloadCreditDailyCsv(
  name: string,
  rows: CreditDateSummary[],
): void {
  const escapeCell = (value: string | number) =>
    `"${String(value).replaceAll('"', '""')}"`;
  const content = [
    ["Date", "Entries", "Total credit given"],
    ...rows.map((row) => [row.date, row.count, row.total]),
  ]
    .map((row) => row.map(escapeCell).join(","))
    .join("\r\n");
  const blob = new Blob(["\ufeff", content], {
    type: "text/csv;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${name
    .trim()
    .replace(/[^a-z0-9]+/gi, "-")
    .toLowerCase()}-credits.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}
