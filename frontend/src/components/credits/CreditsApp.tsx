"use client";

import {
  type UseQueryResult,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  ArrowRight,
  Building2,
  CalendarDays,
  ChevronDown,
  CreditCard,
  Download,
  FileSpreadsheet,
  LayoutDashboard,
  Mail,
  Pencil,
  Phone,
  Plus,
  Search,
  Share2,
  ShieldCheck,
  Trash2,
  UserRound,
  Users,
  Wallet,
} from "lucide-react";
import Link from "next/link";
import { type FormEvent, type ReactNode, useEffect, useState } from "react";
import { useMe } from "@/generated/auth/auth";
import { useGet as useProfile } from "@/generated/profiles/profiles";
import { ApiError } from "@/lib/api-client";
import {
  type CreditDaily,
  type CreditEntry,
  type CreditEntryRequest,
  type CreditMonthly,
  type CreditReceipt,
  type CreditReceiptListItem,
  type CreditTakerRequest,
  type CreditTakerType,
  createCreditEntry,
  createCreditTaker,
  deleteCreditEntry,
  deleteCreditTaker,
  downloadCreditDailyCsv,
  downloadCreditReceipt,
  emailCreditReceipt,
  generateCreditReceipt,
  getCreditDaily,
  getCreditDay,
  getCreditMonthly,
  getCreditReceiptPreview,
  getCreditTaker,
  getCreditTakerOverview,
  listCreditReceipts,
  listCreditTakers,
  updateCreditEntry,
  updateCreditTaker,
} from "@/lib/credits-api";
import styles from "./credits.module.css";

type CreditTab = "day" | "daily" | "monthly" | "receipt";

const today = () => {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const part = (type: string) =>
    parts.find((item) => item.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}`;
};

function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function localTimeString(date: Date) {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Kolkata",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const part = (type: string) =>
    parts.find((item) => item.type === type)?.value ?? "00";
  return `${part("hour")}:${part("minute")}`;
}

function dateDaysAgo(days: number) {
  const [year, month, day] = today().split("-").map(Number);
  const date = new Date(Date.UTC(year ?? 0, (month ?? 1) - 1, day ?? 1));
  date.setUTCDate(date.getUTCDate() - days);
  return date.toISOString().slice(0, 10);
}

function formatMoney(value: number | undefined) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  const date = new Date(`${value}T12:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      }).format(date);
}

function formatTime(value: string | null | undefined) {
  if (!value) return "—";
  const [hours = "0", minutes = "0"] = value.split(":");
  const date = new Date(2000, 0, 1, Number(hours), Number(minutes));
  return new Intl.DateTimeFormat("en-IN", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(date);
}

function formatDateTime(value: string) {
  const [date, time] = value.split("T");
  return date && time ? `${formatDate(date)} ${formatTime(time)}` : value;
}

function typeLabel(value: CreditTakerType) {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

function rangeParams(from: string, to: string) {
  return Boolean(from && to && from <= to);
}

function errorText(error: unknown, fallback: string) {
  if (error instanceof ApiError) {
    if (error.status === 401) return "Please log in to continue.";
    if (error.status === 403) return "Your role does not allow this action.";
    if (error.status === 404) return "This credit record could not be found.";
    if (error.status === 503) return "Email is not configured.";
    return error.message;
  }
  return error instanceof Error ? error.message : fallback;
}

function PageFrame({
  children,
  accountPage,
  displayName,
  role,
  businessId,
}: {
  children: ReactNode;
  accountPage: boolean;
  displayName?: string;
  role?: string;
  businessId?: number;
}) {
  const initials = displayName?.trim().slice(0, 1).toUpperCase() || "U";
  const [monthLabel, setMonthLabel] = useState("");

  useEffect(() => {
    setMonthLabel(
      new Intl.DateTimeFormat("en-US", {
        month: "long",
        year: "numeric",
      }).format(new Date()),
    );
  }, []);

  return (
    <main className={styles.page}>
      <aside className={styles.sidebar}>
        <Link className={styles.brand} href="/" aria-label="BizTrack dashboard">
          <span className={styles.brandMark}>
            <Wallet size={19} />
          </span>
          <span>
            <strong>BizTrack</strong>
            <small>Business Profit Tracker</small>
          </span>
        </Link>
        <div className={styles.businessCard}>
          <Building2 size={15} />
          <span>
            <strong>Business</strong>
            <small>ID: #{businessId ?? "—"}</small>
          </span>
          <i />
        </div>
        <nav className={styles.navigation} aria-label="Main navigation">
          <Link className={styles.navItem} href="/">
            <LayoutDashboard size={16} />
            <span>Dashboard</span>
          </Link>
          <Link className={styles.navItem} href="/">
            <CreditCard size={16} />
            <span>Transactions</span>
          </Link>
          <Link className={styles.navItem} href="/credits" aria-current="page">
            <Wallet size={16} />
            <span>Credits</span>
          </Link>
          <Link className={styles.navItem} href="/">
            <FileSpreadsheet size={16} />
            <span>Reports</span>
          </Link>
          <Link className={styles.navItem} href="/">
            <Users size={16} />
            <span>Members</span>
            {role === "OWNER" && (
              <span className={styles.ownerBadge}>OWNER</span>
            )}
          </Link>
          <Link className={styles.navItem} href="/">
            <UserRound size={16} />
            <span>Profile</span>
          </Link>
        </nav>
        <div className={styles.sidebarFooter}>
          <ShieldCheck size={14} /> GST Compliant <span>v2.4</span>
        </div>
      </aside>
      <div className={styles.main}>
        <header className={styles.topbar}>
          <div className={styles.welcome}>
            <strong>
              Welcome back, {displayName?.trim() || "BizTrack user"}
            </strong>
            <small>Business Health Overview</small>
          </div>
          <span className={styles.businessButton}>
            <Building2 size={14} />
            Business #{businessId ?? "—"}
            <ChevronDown size={13} />
          </span>
          <div className={styles.topbarRight}>
            <span className={styles.dateBadge}>
              <CalendarDays size={13} /> {monthLabel}
            </span>
            <span className={styles.currencyBadge}>₹ INR</span>
            <span className={styles.profileChip}>
              <span>
                <strong>{displayName?.trim() || "BizTrack user"}</strong>
                <small>{role ?? "MEMBER"}</small>
              </span>
              <i>{initials}</i>
            </span>
          </div>
        </header>
        <section className={styles.content}>
          <div className={styles.breadcrumb}>
            <Link href="/">Accounts</Link>
            <span>/</span>
            <Link href="/credits">Credits</Link>
            {accountPage && (
              <>
                <span>/</span>
                <span>Account</span>
              </>
            )}
          </div>
          {children}
        </section>
      </div>
    </main>
  );
}

export function CreditsApp({ creditTakerId }: { creditTakerId?: string }) {
  const meQuery = useMe({ query: { retry: false } });
  const profileQuery = useProfile({
    query: { enabled: Boolean(meQuery.data), retry: false },
  });
  const displayName = profileQuery.data?.fullName ?? "";
  const role = meQuery.data?.role ?? "VIEWER";
  const businessId = meQuery.data?.businessId;
  if (meQuery.isPending) {
    return (
      <PageFrame accountPage={Boolean(creditTakerId)}>
        <p className={styles.state} aria-live="polite">
          Loading your session…
        </p>
      </PageFrame>
    );
  }
  if (!meQuery.data) {
    return (
      <PageFrame accountPage={Boolean(creditTakerId)}>
        <div className={styles.state}>
          <h1>Please log in</h1>
          <p>Your BizTrack session is required to view credits.</p>
          <Link className={styles.primaryButton} href="/">
            Go to login
          </Link>
        </div>
      </PageFrame>
    );
  }

  if (creditTakerId !== undefined) {
    const id = Number(creditTakerId);
    if (!Number.isSafeInteger(id) || id < 1) {
      return (
        <PageFrame
          accountPage
          displayName={displayName}
          role={role}
          businessId={businessId}
        >
          <div className={styles.state} role="alert">
            Credit taker not found.
          </div>
        </PageFrame>
      );
    }
    return (
      <PageFrame
        accountPage
        displayName={displayName}
        role={role}
        businessId={businessId}
      >
        <CreditAccount id={id} role={role} />
      </PageFrame>
    );
  }

  return (
    <PageFrame
      accountPage={false}
      displayName={displayName}
      role={role}
      businessId={businessId}
    >
      <CreditTakerList role={role} />
    </PageFrame>
  );
}

function CreditTakerList({ role }: { role: string }) {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState<CreditTakerType | "ALL">("ALL");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const [noticeIsError, setNoticeIsError] = useState(false);
  const canAdd = role === "OWNER" || role === "EMPLOYEE";
  const listQuery = useQuery({
    queryKey: ["credits", "takers", search],
    queryFn: () => listCreditTakers(search),
    retry: false,
  });
  const visibleTakers = (listQuery.data ?? []).filter(
    (taker) => typeFilter === "ALL" || taker.type === typeFilter,
  );
  const overviewQuery = useQuery({
    queryKey: ["credits", "overview"],
    queryFn: getCreditTakerOverview,
    retry: false,
  });
  const addMutation = useMutation({
    mutationFn: createCreditTaker,
    onSuccess: async () => {
      setDialogOpen(false);
      setNotice("Credit taker added.");
      setNoticeIsError(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["credits", "takers"] }),
        queryClient.invalidateQueries({ queryKey: ["credits", "overview"] }),
      ]);
    },
  });

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const request: CreditTakerRequest = {
      name: String(form.get("name") ?? "").trim(),
      type: String(form.get("type") ?? "CUSTOMER") as CreditTakerType,
      phone: String(form.get("phone") ?? "").trim() || undefined,
      email: String(form.get("email") ?? "").trim() || undefined,
    };
    setNotice("");
    try {
      await addMutation.mutateAsync(request);
    } catch (error) {
      setNotice(errorText(error, "Unable to add this credit taker."));
      setNoticeIsError(true);
    }
  }

  return (
    <>
      <div className={styles.pageHeading}>
        <div>
          <h1>Credits</h1>
          <p>Track the credit you give to customers, retailers and others.</p>
        </div>
        {canAdd && (
          <button
            aria-label="Add credit taker"
            className={styles.primaryButton}
            onClick={() => setDialogOpen(true)}
            type="button"
          >
            <Plus size={16} />
            Add credit taker
          </button>
        )}
      </div>
      {notice && (
        <p
          className={noticeIsError ? styles.errorNotice : styles.successNotice}
          role={noticeIsError ? "alert" : "status"}
        >
          {notice}
        </p>
      )}
      <div className={styles.metrics}>
        <article className={styles.metricCard}>
          <span>Total credit given</span>
          <i className={styles.metricIcon}>
            <Wallet size={15} />
          </i>
          <strong>
            {overviewQuery.isPending
              ? "Loading…"
              : formatMoney(overviewQuery.data?.totalGiven)}
          </strong>
        </article>
        <article className={styles.metricCard}>
          <span>Credit takers</span>
          <i className={styles.metricIcon}>
            <Users size={15} />
          </i>
          <strong>
            {overviewQuery.isPending
              ? "Loading…"
              : (overviewQuery.data?.creditTakerCount ?? 0)}
          </strong>
        </article>
      </div>
      {overviewQuery.isError && (
        <p className={styles.errorNotice} role="alert">
          {errorText(overviewQuery.error, "Unable to load credit totals.")}
        </p>
      )}
      <section className={`${styles.panel} ${styles.listPanel}`}>
        <div className={`${styles.panelHeading} ${styles.listPanelHeading}`}>
          <label className={styles.search}>
            <Search size={15} aria-hidden="true" />
            <span className={styles.srOnly}>Search credit takers</span>
            <input
              onChange={(event) => setSearch(event.currentTarget.value)}
              placeholder="Search by name..."
              type="search"
              value={search}
            />
          </label>
          <fieldset className={styles.typeFilters}>
            <legend className={styles.srOnly}>Filter by account type</legend>
            {(["ALL", "CUSTOMER", "RETAILER", "OTHER"] as const).map((type) => (
              <button
                aria-pressed={typeFilter === type}
                className={typeFilter === type ? styles.activeFilter : ""}
                key={type}
                onClick={() => setTypeFilter(type)}
                type="button"
              >
                {type === "ALL" ? "All types" : typeLabel(type)}
              </button>
            ))}
          </fieldset>
        </div>
        {listQuery.isPending ? (
          <p className={styles.state} aria-live="polite">
            Loading credit takers…
          </p>
        ) : listQuery.isError ? (
          <div className={styles.state} role="alert">
            <p>{errorText(listQuery.error, "Unable to load credit takers.")}</p>
            <button
              className={styles.secondaryButton}
              onClick={() => void listQuery.refetch()}
              type="button"
            >
              Retry
            </button>
          </div>
        ) : visibleTakers.length ? (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Credit taker</th>
                  <th>Type</th>
                  <th>Entries</th>
                  <th>Total credit given</th>
                  <th>Last entry</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {visibleTakers.map((taker) => (
                  <tr key={taker.id}>
                    <td className={styles.longName}>
                      <Link
                        className={styles.takerName}
                        href={`/credits/${taker.id}`}
                      >
                        <span className={styles.takerAvatar}>
                          {taker.name.slice(0, 1).toUpperCase()}
                        </span>
                        <span className={styles.takerDetails}>
                          <strong>{taker.name}</strong>
                          <small>
                            {taker.phone && (
                              <>
                                <Phone size={10} /> {taker.phone}
                              </>
                            )}
                            {taker.phone && taker.email && <span> · </span>}
                            {taker.email}
                            {!taker.phone &&
                              !taker.email &&
                              "No contact details"}
                          </small>
                        </span>
                      </Link>
                    </td>
                    <td>
                      <span
                        className={`${styles.typeBadge} ${styles[taker.type.toLowerCase()]}`}
                      >
                        {typeLabel(taker.type)}
                      </span>
                    </td>
                    <td>{taker.entryCount}</td>
                    <td className={styles.amount}>
                      {formatMoney(taker.totalGiven)}
                    </td>
                    <td>{formatDate(taker.lastEntryDate)}</td>
                    <td>
                      <Link
                        className={styles.tableAction}
                        href={`/credits/${taker.id}`}
                      >
                        View account <ArrowRight size={13} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className={styles.empty}>
            <h3>
              {search || typeFilter !== "ALL"
                ? "No matching credit takers"
                : "No credit takers yet"}
            </h3>
            <p>
              {search || typeFilter !== "ALL"
                ? "Try another name."
                : "Add a credit taker to start tracking credit given."}
            </p>
          </div>
        )}
        {!listQuery.isPending &&
        !listQuery.isError &&
        listQuery.data?.length ? (
          <div className={styles.tableFooter}>
            <span>
              Showing {visibleTakers.length} of {listQuery.data.length} credit
              takers
            </span>
            <span>GST &amp; MSME Ledger standard</span>
          </div>
        ) : null}
      </section>
      {dialogOpen && (
        <div className={styles.overlay} role="presentation">
          <section
            aria-labelledby="add-taker-title"
            aria-modal="true"
            className={styles.dialog}
            role="dialog"
          >
            <div className={styles.dialogHeading}>
              <div>
                <h2 id="add-taker-title">Add credit taker</h2>
                <p>Create an account for a customer or business partner.</p>
              </div>
              <button
                className={styles.iconButton}
                onClick={() => setDialogOpen(false)}
                type="button"
                aria-label="Close dialog"
              >
                ×
              </button>
            </div>
            <form
              className={styles.form}
              onSubmit={(event) => void submit(event)}
            >
              <label>
                Name
                <input maxLength={100} minLength={2} name="name" required />
              </label>
              <label>
                Type
                <select defaultValue="CUSTOMER" name="type">
                  <option value="CUSTOMER">Customer</option>
                  <option value="RETAILER">Retailer</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>
              <label>
                Phone (optional)
                <input
                  autoComplete="tel"
                  inputMode="tel"
                  name="phone"
                  placeholder="10 digits or +91 followed by 10 digits"
                />
              </label>
              <label>
                Email (optional)
                <input autoComplete="email" name="email" type="email" />
              </label>
              {addMutation.isError && (
                <p className={styles.errorNotice} role="alert">
                  {errorText(
                    addMutation.error,
                    "Unable to add this credit taker.",
                  )}
                </p>
              )}
              <div className={styles.formActions}>
                <button
                  className={styles.secondaryButton}
                  onClick={() => setDialogOpen(false)}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className={styles.primaryButton}
                  disabled={addMutation.isPending}
                  type="submit"
                >
                  {addMutation.isPending ? "Saving…" : "Add credit taker"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </>
  );
}

function CreditAccount({ id, role }: { id: number; role: string }) {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<CreditTab>("day");
  const [day, setDay] = useState(today);
  const [dailyRange, setDailyRange] = useState(() => ({
    from: dateDaysAgo(29),
    to: today(),
  }));
  const currentYear = Number(today().slice(0, 4));
  const [year, setYear] = useState(currentYear);
  const [selectedMonth, setSelectedMonth] = useState<number | null>(null);
  const [receiptRange, setReceiptRange] = useState(() => {
    const now = new Date(`${today()}T12:00:00`);
    return {
      from: localDateString(new Date(now.getFullYear(), now.getMonth(), 1)),
      to: localDateString(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
    };
  });
  const [selectedReceipt, setSelectedReceipt] = useState<CreditReceipt | null>(
    null,
  );
  const [selectedReceiptId, setSelectedReceiptId] = useState<number | null>(
    null,
  );
  const [entrySheetOpen, setEntrySheetOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<CreditEntry | null>(null);
  const [takerDialogOpen, setTakerDialogOpen] = useState(false);
  const [entryToDelete, setEntryToDelete] = useState<CreditEntry | null>(null);
  const [confirmTakerDelete, setConfirmTakerDelete] = useState(false);
  const [notice, setNotice] = useState("");
  const [noticeIsError, setNoticeIsError] = useState(false);
  const canAdd = role === "OWNER" || role === "EMPLOYEE";
  const canModify = role === "OWNER";
  const canViewReports = role !== "EMPLOYEE";
  const takerQuery = useQuery({
    queryKey: ["credits", "taker", id],
    queryFn: () => getCreditTaker(id),
    retry: false,
  });
  const dayQuery = useQuery({
    queryKey: ["credits", "day", id, day],
    queryFn: () => getCreditDay(id, day),
    enabled: tab === "day",
    retry: false,
  });
  const dailyQuery = useQuery({
    queryKey: ["credits", "daily", id, dailyRange],
    queryFn: () => getCreditDaily(id, dailyRange.from, dailyRange.to),
    enabled:
      canViewReports &&
      tab === "daily" &&
      rangeParams(dailyRange.from, dailyRange.to),
    retry: false,
  });
  const monthlyQuery = useQuery({
    queryKey: ["credits", "monthly", id, year],
    queryFn: () => getCreditMonthly(id, year),
    enabled: canViewReports && tab === "monthly",
    retry: false,
  });
  const monthRange =
    selectedMonth === null
      ? null
      : {
          from: `${year}-${String(selectedMonth).padStart(2, "0")}-01`,
          to: localDateString(new Date(year, selectedMonth, 0)),
        };
  const monthDaysQuery = useQuery({
    queryKey: ["credits", "monthly-days", id, monthRange],
    queryFn: () =>
      getCreditDaily(id, monthRange?.from ?? "", monthRange?.to ?? ""),
    enabled: canViewReports && tab === "monthly" && monthRange !== null,
    retry: false,
  });
  const previewQuery = useQuery({
    queryKey: ["credits", "receipt-preview", id, receiptRange],
    queryFn: () =>
      getCreditReceiptPreview(id, receiptRange.from, receiptRange.to),
    enabled:
      canViewReports &&
      tab === "receipt" &&
      rangeParams(receiptRange.from, receiptRange.to),
    retry: false,
  });
  const receiptsQuery = useQuery({
    queryKey: ["credits", "receipts", id],
    queryFn: () => listCreditReceipts(id),
    enabled: canViewReports && tab === "receipt",
    retry: false,
  });
  const entryMutation = useMutation({
    mutationFn: ({
      entryId,
      data,
    }: {
      entryId?: number;
      data: CreditEntryRequest;
    }) =>
      entryId ? updateCreditEntry(entryId, data) : createCreditEntry(id, data),
    onSuccess: async () => {
      setEntrySheetOpen(false);
      setEditingEntry(null);
      setNotice(editingEntry ? "Credit entry updated." : "Credit added.");
      setNoticeIsError(false);
      await invalidateAccountData(queryClient, id);
    },
  });
  const takerMutation = useMutation({
    mutationFn: (data: CreditTakerRequest) => updateCreditTaker(id, data),
    onSuccess: async () => {
      setTakerDialogOpen(false);
      setNotice("Credit taker updated.");
      setNoticeIsError(false);
      await queryClient.invalidateQueries({
        queryKey: ["credits", "taker", id],
      });
      await queryClient.invalidateQueries({ queryKey: ["credits", "takers"] });
    },
  });
  const deleteEntryMutation = useMutation({
    mutationFn: deleteCreditEntry,
    onSuccess: async () => {
      setEntryToDelete(null);
      setNotice("Credit entry deleted.");
      setNoticeIsError(false);
      await invalidateAccountData(queryClient, id);
    },
  });
  const deleteTakerMutation = useMutation({
    mutationFn: deleteCreditTaker,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["credits", "takers"] });
      await queryClient.invalidateQueries({
        queryKey: ["credits", "overview"],
      });
      window.location.assign("/credits");
    },
  });
  const generateMutation = useMutation({
    mutationFn: () =>
      generateCreditReceipt(id, receiptRange.from, receiptRange.to),
    onSuccess: async (receipt) => {
      setSelectedReceipt(receipt);
      setSelectedReceiptId(receipt.id);
      setNotice(`Receipt ${receipt.receiptNo} generated.`);
      setNoticeIsError(false);
      await queryClient.invalidateQueries({
        queryKey: ["credits", "receipts", id],
      });
    },
  });

  async function submitEntry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const amount = Number(form.get("amount"));
    const entryDate = String(form.get("entryDate") ?? "");
    if (!Number.isFinite(amount) || amount < 0.01 || amount > 99999999.99) {
      setNotice("Amount must be between Rs. 0.01 and Rs. 99,999,999.99.");
      setNoticeIsError(true);
      return;
    }
    if (!entryDate || entryDate > today()) {
      setNotice("Credit date cannot be in the future.");
      setNoticeIsError(true);
      return;
    }
    const request: CreditEntryRequest = {
      amount,
      entryDate,
      entryTime: String(form.get("entryTime") ?? ""),
      note: String(form.get("note") ?? "").trim() || undefined,
    };
    setNotice("");
    try {
      await entryMutation.mutateAsync({
        entryId: editingEntry?.id,
        data: request,
      });
    } catch (error) {
      setNotice(errorText(error, "Unable to save the credit entry."));
      setNoticeIsError(true);
    }
  }

  async function submitTaker(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const request: CreditTakerRequest = {
      name: String(form.get("name") ?? "").trim(),
      type: String(form.get("type") ?? "CUSTOMER") as CreditTakerType,
      phone: String(form.get("phone") ?? "").trim() || undefined,
      email: String(form.get("email") ?? "").trim() || undefined,
    };
    try {
      await takerMutation.mutateAsync(request);
    } catch (error) {
      setNotice(errorText(error, "Unable to update the credit taker."));
      setNoticeIsError(true);
    }
  }

  async function generateAndDownload() {
    try {
      const receipt =
        selectedReceipt?.periodFrom === receiptRange.from &&
        selectedReceipt.periodTo === receiptRange.to
          ? selectedReceipt
          : await generateMutation.mutateAsync();
      if (!receipt.id) return;
      const blob = await downloadCreditReceipt(receipt.id);
      saveBlob(blob, `${receipt.receiptNo}.pdf`);
    } catch (error) {
      setNotice(errorText(error, "Unable to download the receipt PDF."));
      setNoticeIsError(true);
    }
  }

  async function downloadSelectedReceipt() {
    const idToDownload = selectedReceiptId ?? receiptsQuery.data?.[0]?.id;
    if (!idToDownload) {
      if (canModify) {
        await generateAndDownload();
      }
      return;
    }
    try {
      const blob = await downloadCreditReceipt(idToDownload);
      const receipt = receiptsQuery.data?.find(
        (item) => item.id === idToDownload,
      );
      saveBlob(blob, `${receipt?.receiptNo ?? "credit-receipt"}.pdf`);
    } catch (error) {
      setNotice(errorText(error, "Unable to download the receipt PDF."));
      setNoticeIsError(true);
    }
  }

  async function sendReceiptEmail() {
    const toEmail = window.prompt(
      "Send the receipt to which email address?",
      takerQuery.data?.email ?? "",
    );
    if (toEmail === null) return;
    try {
      const receipt =
        selectedReceipt?.periodFrom === receiptRange.from &&
        selectedReceipt.periodTo === receiptRange.to
          ? selectedReceipt
          : await generateMutation.mutateAsync();
      if (!receipt.id) return;
      await emailCreditReceipt(receipt.id, toEmail.trim());
      setNotice("Receipt email sent.");
      setNoticeIsError(false);
    } catch (error) {
      setNotice(errorText(error, "Unable to send the receipt email."));
      setNoticeIsError(true);
    }
  }

  function shareReceipt() {
    const phone = takerQuery.data?.phone?.replace(/\D/g, "") ?? "";
    if (!phone) return;
    const phoneWithCountryCode = phone.length === 10 ? `91${phone}` : phone;
    const receipt = selectedReceipt ?? previewQuery.data;
    const total = receipt?.totalAmount ?? 0;
    const text = `${takerQuery.data?.name ?? "Credit account"}: credit given ${formatMoney(total)} from ${formatDate(receiptRange.from)} to ${formatDate(receiptRange.to)}.`;
    window.open(
      `https://wa.me/${phoneWithCountryCode}?text=${encodeURIComponent(text)}`,
      "_blank",
      "noopener,noreferrer",
    );
  }

  async function copyReceiptText() {
    const receipt = selectedReceipt ?? previewQuery.data;
    if (!receipt) return;
    const text = [
      receipt.businessName,
      `Credit receipt ${receipt.receiptNo}`,
      `Credit taker: ${receipt.creditTakerName}`,
      `Period: ${formatDate(receipt.periodFrom)} to ${formatDate(receipt.periodTo)}`,
      `Entries: ${receipt.entryCount}`,
      `Total credit given: ${formatMoney(receipt.totalAmount)}`,
    ].join("\n");
    try {
      await navigator.clipboard.writeText(text);
      setNotice("Receipt text copied.");
      setNoticeIsError(false);
    } catch {
      setNotice(
        "Unable to copy the receipt text. Check clipboard permissions.",
      );
      setNoticeIsError(true);
    }
  }

  function openEntryForm(entry?: CreditEntry) {
    setEditingEntry(entry ?? null);
    setEntrySheetOpen(true);
  }

  function changeReceiptRange(field: "from" | "to", value: string) {
    setReceiptRange((current) => ({ ...current, [field]: value }));
    setSelectedReceipt(null);
  }

  const selectedTab = canViewReports ? tab : "day";
  const taker = takerQuery.data;
  const tabs: { value: CreditTab; label: string }[] = [
    { value: "day", label: "Per day" },
    ...(canViewReports
      ? [
          { value: "daily" as const, label: "Daily" },
          { value: "monthly" as const, label: "Monthly" },
          { value: "receipt" as const, label: "Receipt" },
        ]
      : []),
  ];

  if (takerQuery.isPending) {
    return (
      <p className={styles.state} aria-live="polite">
        Loading credit account…
      </p>
    );
  }
  if (takerQuery.isError || !taker) {
    return (
      <div className={styles.state} role="alert">
        <h1>Credit taker not found</h1>
        <p>{errorText(takerQuery.error, "This account is unavailable.")}</p>
        <Link className={styles.secondaryButton} href="/credits">
          Back to credits
        </Link>
      </div>
    );
  }

  return (
    <>
      <div className={styles.pageHeading}>
        <div className={styles.accountHeading}>
          <span className={styles.eyebrow}>
            CREDIT ACCOUNT · {typeLabel(taker.type).toUpperCase()}
          </span>
          <h1 className={styles.longName}>{taker.name}</h1>
          <p>
            {[taker.phone, taker.email].filter(Boolean).join(" · ") ||
              "No contact details"}
          </p>
        </div>
        <div className={styles.headingActions}>
          {canModify && (
            <>
              <button
                className={styles.secondaryButton}
                onClick={() => setTakerDialogOpen(true)}
                type="button"
              >
                <Pencil size={15} /> Edit
              </button>
              <button
                className={styles.dangerButton}
                disabled={taker.entryCount > 0}
                onClick={() => setConfirmTakerDelete(true)}
                title={
                  taker.entryCount > 0
                    ? "Delete the entries first."
                    : "Delete this credit taker"
                }
                type="button"
              >
                <Trash2 size={15} /> Delete
              </button>
            </>
          )}
          {canAdd && (
            <button
              className={styles.primaryButton}
              onClick={() => openEntryForm()}
              type="button"
            >
              <Plus size={16} /> Add credit
            </button>
          )}
        </div>
      </div>
      <div className={styles.accountSummary}>
        <span>Total credit given</span>
        <strong>{formatMoney(taker.totalGiven)}</strong>
        <small>
          {taker.entryCount} {taker.entryCount === 1 ? "entry" : "entries"}
        </small>
      </div>
      {notice && (
        <p
          className={noticeIsError ? styles.errorNotice : styles.successNotice}
          role={noticeIsError ? "alert" : "status"}
        >
          {notice}
        </p>
      )}
      <div
        aria-label="Credit account views"
        className={styles.tabs}
        role="tablist"
      >
        {tabs.map((item) => (
          <button
            aria-selected={selectedTab === item.value}
            className={selectedTab === item.value ? styles.activeTab : ""}
            id={`credit-tab-${item.value}`}
            key={item.value}
            onClick={() => setTab(item.value)}
            role="tab"
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>
      {selectedTab === "day" && (
        <section
          aria-labelledby="credit-tab-day"
          className={styles.panel}
          role="tabpanel"
        >
          <div className={styles.panelHeading}>
            <div>
              <h2>Per day</h2>
              <p>Every credit entry on the selected date</p>
            </div>
            <label className={styles.dateField}>
              <span>Date</span>
              <input
                max={today()}
                onChange={(event) => setDay(event.currentTarget.value)}
                type="date"
                value={day}
              />
            </label>
          </div>
          {dayQuery.isPending ? (
            <p className={styles.state}>Loading entries…</p>
          ) : dayQuery.isError ? (
            <ErrorState
              error={dayQuery.error}
              fallback="Unable to load this day's entries."
              onRetry={() => void dayQuery.refetch()}
            />
          ) : (
            <>
              <div className={styles.dayTotal}>
                <span>
                  {formatDate(day)} · {dayQuery.data?.count ?? 0} entries
                </span>
                <strong>{formatMoney(dayQuery.data?.dayTotal)}</strong>
              </div>
              {dayQuery.data?.entries.length ? (
                <div className={styles.tableWrap}>
                  <table className={styles.table}>
                    <thead>
                      <tr>
                        <th>Time</th>
                        <th>Amount</th>
                        <th>Note</th>
                        {canModify && <th>Actions</th>}
                      </tr>
                    </thead>
                    <tbody>
                      {dayQuery.data.entries.map((entry) => (
                        <tr key={entry.id}>
                          <td>{formatTime(entry.entryTime)}</td>
                          <td>{formatMoney(entry.amount)}</td>
                          <td className={styles.longName}>
                            {entry.note || "—"}
                          </td>
                          {canModify && (
                            <td>
                              <div className={styles.rowActions}>
                                <button
                                  className={styles.iconButton}
                                  onClick={() => openEntryForm(entry)}
                                  type="button"
                                  aria-label={`Edit ${formatMoney(entry.amount)} entry`}
                                >
                                  <Pencil size={15} />
                                </button>
                                <button
                                  className={styles.iconButton}
                                  onClick={() => setEntryToDelete(entry)}
                                  type="button"
                                  aria-label={`Delete ${formatMoney(entry.amount)} entry`}
                                >
                                  <Trash2 size={15} />
                                </button>
                              </div>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className={styles.empty}>
                  <h3>No entries for this day</h3>
                  <p>Choose another date or add a credit entry.</p>
                </div>
              )}
            </>
          )}
        </section>
      )}
      {selectedTab === "daily" && canViewReports && (
        <DailyTab
          dailyQuery={dailyQuery}
          range={dailyRange}
          setRange={setDailyRange}
          takerName={taker.name}
        />
      )}
      {selectedTab === "monthly" && canViewReports && (
        <MonthlyTab
          dailyQuery={monthDaysQuery}
          monthlyQuery={monthlyQuery}
          onSelectMonth={(month) => setSelectedMonth(month)}
          selectedMonth={selectedMonth}
          setYear={setYear}
          year={year}
        />
      )}
      {selectedTab === "receipt" && canViewReports && (
        <ReceiptTab
          canModify={canModify}
          generatePending={generateMutation.isPending}
          onChangeRange={changeReceiptRange}
          onCopy={() => void copyReceiptText()}
          onDownload={() => void downloadSelectedReceipt()}
          onEmail={() => void sendReceiptEmail()}
          onGenerate={() => {
            setNotice("");
            generateMutation.mutate();
          }}
          onShare={shareReceipt}
          preview={selectedReceipt ?? previewQuery.data}
          previewError={
            previewQuery.isError
              ? errorText(previewQuery.error, "Unable to preview this receipt.")
              : ""
          }
          range={receiptRange}
          receipts={receiptsQuery.data ?? []}
          receiptsError={
            receiptsQuery.isError
              ? errorText(receiptsQuery.error, "Unable to load receipts.")
              : ""
          }
          selectedReceiptId={selectedReceiptId}
          setSelectedReceiptId={setSelectedReceiptId}
          takerEmail={taker.email}
          takerPhone={taker.phone}
        />
      )}
      {entrySheetOpen && (
        <div className={styles.sheetOverlay} role="presentation">
          <section
            aria-labelledby="entry-sheet-title"
            aria-modal="true"
            className={styles.sheet}
            role="dialog"
          >
            <div className={styles.dialogHeading}>
              <div>
                <h2 id="entry-sheet-title">
                  {editingEntry ? "Edit credit" : "Add credit"}
                </h2>
                <p>Record money given on credit to {taker.name}.</p>
              </div>
              <button
                className={styles.iconButton}
                onClick={() => {
                  setEntrySheetOpen(false);
                  setEditingEntry(null);
                }}
                type="button"
                aria-label="Close sheet"
              >
                ×
              </button>
            </div>
            <form
              className={styles.form}
              onSubmit={(event) => void submitEntry(event)}
            >
              <label>
                Amount (rupees)
                <input
                  defaultValue={editingEntry?.amount ?? ""}
                  max="99999999.99"
                  min="0.01"
                  name="amount"
                  required
                  step="0.01"
                  type="number"
                />
              </label>
              <label>
                Date
                <input
                  defaultValue={editingEntry?.entryDate ?? today()}
                  max={today()}
                  name="entryDate"
                  required
                  type="date"
                />
              </label>
              <label>
                Time
                <input
                  defaultValue={
                    editingEntry?.entryTime?.slice(0, 5) ??
                    localTimeString(new Date())
                  }
                  name="entryTime"
                  required
                  type="time"
                />
              </label>
              <label>
                Note (optional)
                <input
                  defaultValue={editingEntry?.note ?? ""}
                  maxLength={200}
                  name="note"
                />
              </label>
              {entryMutation.isError && (
                <p className={styles.errorNotice} role="alert">
                  {errorText(
                    entryMutation.error,
                    "Unable to save the credit entry.",
                  )}
                </p>
              )}
              <div className={styles.formActions}>
                <button
                  className={styles.secondaryButton}
                  onClick={() => {
                    setEntrySheetOpen(false);
                    setEditingEntry(null);
                  }}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className={styles.primaryButton}
                  disabled={entryMutation.isPending}
                  type="submit"
                >
                  {entryMutation.isPending
                    ? "Saving…"
                    : editingEntry
                      ? "Save changes"
                      : "Add credit"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
      {takerDialogOpen && (
        <div className={styles.overlay} role="presentation">
          <section
            aria-labelledby="edit-taker-title"
            aria-modal="true"
            className={styles.dialog}
            role="dialog"
          >
            <div className={styles.dialogHeading}>
              <div>
                <h2 id="edit-taker-title">Edit credit taker</h2>
                <p>Update this account's details.</p>
              </div>
              <button
                className={styles.iconButton}
                onClick={() => setTakerDialogOpen(false)}
                type="button"
                aria-label="Close dialog"
              >
                ×
              </button>
            </div>
            <form
              className={styles.form}
              onSubmit={(event) => void submitTaker(event)}
            >
              <label>
                Name
                <input
                  defaultValue={taker.name}
                  maxLength={100}
                  minLength={2}
                  name="name"
                  required
                />
              </label>
              <label>
                Type
                <select defaultValue={taker.type} name="type">
                  <option value="CUSTOMER">Customer</option>
                  <option value="RETAILER">Retailer</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>
              <label>
                Phone (optional)
                <input
                  defaultValue={taker.phone ?? ""}
                  inputMode="tel"
                  name="phone"
                />
              </label>
              <label>
                Email (optional)
                <input
                  defaultValue={taker.email ?? ""}
                  name="email"
                  type="email"
                />
              </label>
              {takerMutation.isError && (
                <p className={styles.errorNotice} role="alert">
                  {errorText(
                    takerMutation.error,
                    "Unable to update this credit taker.",
                  )}
                </p>
              )}
              <div className={styles.formActions}>
                <button
                  className={styles.secondaryButton}
                  onClick={() => setTakerDialogOpen(false)}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className={styles.primaryButton}
                  disabled={takerMutation.isPending}
                  type="submit"
                >
                  {takerMutation.isPending ? "Saving…" : "Save changes"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
      {(entryToDelete || confirmTakerDelete) && (
        <div className={styles.overlay} role="presentation">
          <section
            aria-labelledby="confirm-delete-title"
            aria-modal="true"
            className={styles.dialog}
            role="alertdialog"
          >
            <h2 id="confirm-delete-title">
              {entryToDelete ? "Delete credit entry?" : "Delete credit taker?"}
            </h2>
            <p>
              {entryToDelete
                ? "This entry will be permanently removed from the account."
                : "This credit taker will be permanently removed."}
            </p>
            {(deleteEntryMutation.isError || deleteTakerMutation.isError) && (
              <p className={styles.errorNotice} role="alert">
                {errorText(
                  deleteEntryMutation.error ?? deleteTakerMutation.error,
                  "Unable to delete this record.",
                )}
              </p>
            )}
            <div className={styles.formActions}>
              <button
                className={styles.secondaryButton}
                onClick={() => {
                  setEntryToDelete(null);
                  setConfirmTakerDelete(false);
                }}
                type="button"
              >
                Cancel
              </button>
              <button
                className={styles.dangerButton}
                disabled={
                  deleteEntryMutation.isPending || deleteTakerMutation.isPending
                }
                onClick={() => {
                  if (entryToDelete)
                    deleteEntryMutation.mutate(entryToDelete.id);
                  else deleteTakerMutation.mutate(id);
                }}
                type="button"
              >
                {deleteEntryMutation.isPending || deleteTakerMutation.isPending
                  ? "Deleting…"
                  : "Delete"}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}

function DailyTab({
  dailyQuery,
  range,
  setRange,
  takerName,
}: {
  dailyQuery: UseQueryResult<CreditDaily>;
  range: { from: string; to: string };
  setRange: (range: { from: string; to: string }) => void;
  takerName: string;
}) {
  const maxTotal = Math.max(
    0,
    ...(dailyQuery.data?.days ?? []).map((row) => row.total),
  );
  return (
    <section
      aria-labelledby="credit-tab-daily"
      className={styles.panel}
      role="tabpanel"
    >
      <div className={styles.panelHeading}>
        <div>
          <h2>Daily summary</h2>
          <p>Totals by day for {takerName}</p>
        </div>
        <div className={styles.dateRange}>
          <label>
            <span>From</span>
            <input
              max={range.to}
              onChange={(event) =>
                setRange({ ...range, from: event.currentTarget.value })
              }
              type="date"
              value={range.from}
            />
          </label>
          <label>
            <span>To</span>
            <input
              min={range.from}
              onChange={(event) =>
                setRange({ ...range, to: event.currentTarget.value })
              }
              type="date"
              value={range.to}
            />
          </label>
        </div>
      </div>
      <div className={styles.summaryTotal}>
        <span>Grand total</span>
        <strong>
          {dailyQuery.isPending
            ? "Loading…"
            : formatMoney(dailyQuery.data?.grandTotal)}
        </strong>
      </div>
      {dailyQuery.isError ? (
        <ErrorState
          error={dailyQuery.error}
          fallback="Unable to load daily totals."
          onRetry={() => void dailyQuery.refetch()}
        />
      ) : dailyQuery.isPending ? (
        <p className={styles.state}>Loading daily totals…</p>
      ) : (
        <>
          <div
            aria-label="Daily credit amounts"
            className={styles.barChart}
            role="img"
          >
            {dailyQuery.data?.days.map((row) => (
              <div
                className={styles.barColumn}
                key={row.date}
                title={`${formatDate(row.date)}: ${formatMoney(row.total)}`}
              >
                <span
                  className={styles.bar}
                  style={{
                    height: `${maxTotal === 0 ? 0 : (row.total / maxTotal) * 100}%`,
                  }}
                />
                <small>{new Date(`${row.date}T12:00:00`).getDate()}</small>
              </div>
            ))}
          </div>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Entries</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {dailyQuery.data?.days.map((row) => (
                  <tr key={row.date}>
                    <td>{formatDate(row.date)}</td>
                    <td>{row.count}</td>
                    <td>{formatMoney(row.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <button
            className={styles.secondaryButton}
            onClick={() =>
              downloadCreditDailyCsv(takerName, dailyQuery.data?.days ?? [])
            }
            type="button"
          >
            <Download size={15} /> Download CSV
          </button>
        </>
      )}
    </section>
  );
}

function MonthlyTab({
  dailyQuery,
  monthlyQuery,
  onSelectMonth,
  selectedMonth,
  setYear,
  year,
}: {
  dailyQuery: UseQueryResult<CreditDaily>;
  monthlyQuery: UseQueryResult<CreditMonthly>;
  onSelectMonth: (month: number | null) => void;
  selectedMonth: number | null;
  setYear: (year: number) => void;
  year: number;
}) {
  const maxTotal = Math.max(
    0,
    ...(monthlyQuery.data?.months ?? []).map((row) => row.total),
  );
  const months = monthlyQuery.data?.months ?? [];
  return (
    <section
      aria-labelledby="credit-tab-monthly"
      className={styles.panel}
      role="tabpanel"
    >
      <div className={styles.panelHeading}>
        <div>
          <h2>Monthly summary</h2>
          <p>Credit given by month</p>
        </div>
        <label className={styles.dateField}>
          <span>Year</span>
          <select
            onChange={(event) => {
              setYear(Number(event.currentTarget.value));
              onSelectMonth(null);
            }}
            value={year}
          >
            {Array.from(
              { length: 10 },
              (_, index) => new Date().getFullYear() - index,
            ).map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
      </div>
      {monthlyQuery.isPending ? (
        <p className={styles.state}>Loading monthly totals…</p>
      ) : monthlyQuery.isError ? (
        <ErrorState
          error={monthlyQuery.error}
          fallback="Unable to load monthly totals."
          onRetry={() => void monthlyQuery.refetch()}
        />
      ) : (
        <>
          <div className={styles.summaryTotal}>
            <span>Year total</span>
            <strong>{formatMoney(monthlyQuery.data?.yearTotal)}</strong>
          </div>
          <div
            aria-label={`Monthly credit amounts for ${year}`}
            className={styles.barChart}
            role="img"
          >
            {months.map((row) => (
              <button
                aria-label={`${monthName(row.month)}: ${formatMoney(row.total)}`}
                className={`${styles.barColumn} ${selectedMonth === row.month ? styles.selectedBarColumn : ""}`}
                key={row.month}
                onClick={() => onSelectMonth(row.month)}
                title={`${monthName(row.month)}: ${formatMoney(row.total)}`}
                type="button"
              >
                <span
                  className={styles.bar}
                  style={{
                    height: `${maxTotal === 0 ? 0 : (row.total / maxTotal) * 100}%`,
                  }}
                />
                <small>{monthName(row.month).slice(0, 3)}</small>
              </button>
            ))}
          </div>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Month</th>
                  <th>Entries</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {months.map((row) => (
                  <tr key={row.month}>
                    <td>
                      <button
                        className={styles.textButton}
                        onClick={() => onSelectMonth(row.month)}
                        type="button"
                      >
                        {monthName(row.month)}
                      </button>
                    </td>
                    <td>{row.count}</td>
                    <td>{formatMoney(row.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {selectedMonth !== null && (
            <section className={styles.monthDays}>
              <h3>
                {monthName(selectedMonth)} {year} · daily entries
              </h3>
              {dailyQuery.isPending ? (
                <p className={styles.state}>Loading days…</p>
              ) : dailyQuery.isError ? (
                <ErrorState
                  error={dailyQuery.error}
                  fallback="Unable to load this month's days."
                  onRetry={() => void dailyQuery.refetch()}
                />
              ) : (
                <div className={styles.tableWrap}>
                  <table className={styles.table}>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Entries</th>
                        <th>Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(dailyQuery.data?.days ?? []).map((row) => (
                        <tr key={row.date}>
                          <td>{formatDate(row.date)}</td>
                          <td>{row.count}</td>
                          <td>{formatMoney(row.total)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </>
      )}
    </section>
  );
}

function ReceiptTab({
  canModify,
  generatePending,
  onChangeRange,
  onCopy,
  onDownload,
  onEmail,
  onGenerate,
  onShare,
  preview,
  previewError,
  range,
  receipts,
  receiptsError,
  selectedReceiptId,
  setSelectedReceiptId,
  takerEmail,
  takerPhone,
}: {
  canModify: boolean;
  generatePending: boolean;
  onChangeRange: (field: "from" | "to", value: string) => void;
  onCopy: () => void;
  onDownload: () => void;
  onEmail: () => void;
  onGenerate: () => void;
  onShare: () => void;
  preview: CreditReceipt | undefined;
  previewError: string;
  range: { from: string; to: string };
  receipts: CreditReceiptListItem[];
  receiptsError: string;
  selectedReceiptId: number | null;
  setSelectedReceiptId: (id: number) => void;
  takerEmail: string | null;
  takerPhone: string | null;
}) {
  return (
    <section
      aria-labelledby="credit-tab-receipt"
      className={styles.panel}
      role="tabpanel"
    >
      <div className={styles.panelHeading}>
        <div>
          <h2>Credit receipt</h2>
          <p>Preview and generate an account receipt to share</p>
        </div>
        <div className={styles.dateRange}>
          <label>
            <span>From</span>
            <input
              max={range.to}
              onChange={(event) =>
                onChangeRange("from", event.currentTarget.value)
              }
              type="date"
              value={range.from}
            />
          </label>
          <label>
            <span>To</span>
            <input
              min={range.from}
              onChange={(event) =>
                onChangeRange("to", event.currentTarget.value)
              }
              type="date"
              value={range.to}
            />
          </label>
        </div>
      </div>
      {previewError ? (
        <p className={styles.errorNotice} role="alert">
          {previewError}
        </p>
      ) : !preview ? (
        <p className={styles.state}>Loading receipt preview…</p>
      ) : (
        <article className={styles.receiptPreview}>
          <div className={styles.receiptHeader}>
            <div>
              <span className={styles.eyebrow}>CREDIT GIVEN RECEIPT</span>
              <h3>{preview.businessName}</h3>
              {preview.businessGstin && <p>GSTIN: {preview.businessGstin}</p>}
            </div>
            <strong>{preview.receiptNo}</strong>
          </div>
          <div className={styles.receiptDetails}>
            <span>
              <b>Credit taker</b>
              {preview.creditTakerName}
            </span>
            <span>
              <b>Type</b>
              {typeLabel(preview.type)}
            </span>
            {preview.phone && (
              <span>
                <b>Phone</b>
                {preview.phone}
              </span>
            )}
            <span>
              <b>Period</b>
              {formatDate(preview.periodFrom)} – {formatDate(preview.periodTo)}
            </span>
            <span>
              <b>Generated</b>
              {formatDateTime(preview.generatedAt)}
            </span>
            <span>
              <b>Generated by</b>
              {preview.generatedBy}
            </span>
          </div>
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Amount</th>
                  <th>Note</th>
                </tr>
              </thead>
              <tbody>
                {preview.entries.map((entry, index) => (
                  <tr key={`${entry.date}-${entry.time}-${index}`}>
                    <td>{formatDate(entry.date)}</td>
                    <td>{formatTime(entry.time)}</td>
                    <td>{formatMoney(entry.amount)}</td>
                    <td className={styles.longName}>{entry.note || "—"}</td>
                  </tr>
                ))}
                {!preview.entries.length && (
                  <tr>
                    <td colSpan={4}>No entries for this period.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className={styles.receiptTotal}>
            <span>{preview.entryCount} entries</span>
            <strong>
              TOTAL CREDIT GIVEN: {formatMoney(preview.totalAmount)}
            </strong>
          </div>
        </article>
      )}
      <div className={styles.receiptActions}>
        {canModify && (
          <button
            className={styles.primaryButton}
            disabled={generatePending || !preview}
            onClick={onGenerate}
            type="button"
          >
            {generatePending ? "Generating…" : "Generate receipt"}
          </button>
        )}
        <button
          className={styles.secondaryButton}
          disabled={generatePending || (!canModify && receipts.length === 0)}
          onClick={onDownload}
          type="button"
        >
          <Download size={15} /> Download PDF
        </button>
        {canModify && (
          <button
            className={styles.secondaryButton}
            disabled={generatePending}
            onClick={onEmail}
            type="button"
            title={takerEmail || "Enter an email address"}
          >
            <Mail size={15} /> Send by email
          </button>
        )}
        <button
          className={styles.secondaryButton}
          disabled={!takerPhone}
          onClick={onShare}
          type="button"
          title={
            !takerPhone
              ? "Add a phone number to share on WhatsApp"
              : "Share on WhatsApp"
          }
        >
          <Share2 size={15} /> Share on WhatsApp
        </button>
        <button
          className={styles.secondaryButton}
          disabled={!preview}
          onClick={onCopy}
          type="button"
        >
          Copy text
        </button>
      </div>
      <section className={styles.receiptHistory}>
        <h3>Generated receipts</h3>
        {receiptsError ? (
          <p className={styles.errorNotice} role="alert">
            {receiptsError}
          </p>
        ) : receipts.length ? (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Receipt</th>
                  <th>Period</th>
                  <th>Total</th>
                  <th>Generated</th>
                  <th>Download</th>
                </tr>
              </thead>
              <tbody>
                {receipts.map((receipt) => (
                  <tr
                    className={
                      selectedReceiptId === receipt.id
                        ? styles.selectedReceipt
                        : ""
                    }
                    key={receipt.id}
                  >
                    <td>{receipt.receiptNo}</td>
                    <td>
                      {formatDate(receipt.periodFrom)} –{" "}
                      {formatDate(receipt.periodTo)}
                    </td>
                    <td>{formatMoney(receipt.totalAmount)}</td>
                    <td>{formatDateTime(receipt.generatedAt)}</td>
                    <td>
                      <button
                        className={styles.textButton}
                        onClick={() => setSelectedReceiptId(receipt.id)}
                        type="button"
                      >
                        Select PDF
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className={styles.state}>No receipts have been generated yet.</p>
        )}
      </section>
    </section>
  );
}

function ErrorState({
  error,
  fallback,
  onRetry,
}: {
  error: unknown;
  fallback: string;
  onRetry: () => void;
}) {
  return (
    <div className={styles.state} role="alert">
      <p>{errorText(error, fallback)}</p>
      <button
        className={styles.secondaryButton}
        onClick={onRetry}
        type="button"
      >
        Retry
      </button>
    </div>
  );
}

async function invalidateAccountData(
  queryClient: ReturnType<typeof useQueryClient>,
  id: number,
) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ["credits", "taker", id] }),
    queryClient.invalidateQueries({ queryKey: ["credits", "overview"] }),
    queryClient.invalidateQueries({ queryKey: ["credits", "takers"] }),
    queryClient.invalidateQueries({ queryKey: ["credits", "day", id] }),
    queryClient.invalidateQueries({ queryKey: ["credits", "daily", id] }),
    queryClient.invalidateQueries({ queryKey: ["credits", "monthly", id] }),
    queryClient.invalidateQueries({
      queryKey: ["credits", "monthly-days", id],
    }),
    queryClient.invalidateQueries({
      queryKey: ["credits", "receipt-preview", id],
    }),
  ]);
}

function monthName(month: number) {
  return new Intl.DateTimeFormat("en-IN", { month: "long" }).format(
    new Date(2026, month - 1, 1),
  );
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}
