"use client";

import { useQueryClient } from "@tanstack/react-query";
import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  Bell,
  Building2,
  CalendarDays,
  Check,
  ChevronDown,
  CircleHelp,
  CreditCard,
  Eye,
  EyeOff,
  FileCheck2,
  FileSpreadsheet,
  FileText,
  Languages,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Menu,
  Pencil,
  Plus,
  Search,
  ShieldCheck,
  ShoppingCart,
  Tags,
  Trash2,
  Upload,
  UserRound,
  Users,
  Wallet,
} from "lucide-react";
import Image from "next/image";
import {
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  getMeQueryKey,
  useLogin,
  useLogout,
  useMe,
  useRegister,
} from "@/generated/auth/auth";
import { useList2 } from "@/generated/categories/categories";
import {
  getList1QueryKey,
  useAdd1,
  useChangeRole,
  useList1,
} from "@/generated/members/members";
import type {
  Access,
  Transaction,
  TransactionRequest,
} from "@/generated/model";
import { AddMemberRequestRole } from "@/generated/model/addMemberRequestRole";
import { CategoryTransactionType } from "@/generated/model/categoryTransactionType";
import { ListType } from "@/generated/model/listType";
import { MemberResponseRole } from "@/generated/model/memberResponseRole";
import { RegisterRequestRole } from "@/generated/model/registerRequestRole";
import { RoleRequestRole } from "@/generated/model/roleRequestRole";
import { TransactionRequestTransactionType } from "@/generated/model/transactionRequestTransactionType";
import {
  getGetQueryKey as getProfileQueryKey,
  useGet as useProfile,
  useUpdate1 as useUpdateProfile,
} from "@/generated/profiles/profiles";
import {
  getDashboardQueryKey,
  getMonthlyQueryKey,
  getSummaryQueryKey,
  useDashboard,
  useMonthly,
  useSummary,
} from "@/generated/reports/reports";
import {
  getListQueryKey as getTransactionsQueryKey,
  useAdd,
  useDelete,
  useList,
  useUpdate,
} from "@/generated/transactions/transactions";
import { ApiError } from "@/lib/api-client";
import {
  type AuthLanguage,
  authLanguages,
  getAuthCopy,
  isAuthLanguage,
  translateDashboardText,
} from "@/lib/auth-translations";
import {
  addTransactionWithReceipt,
  extractReceiptDetails,
  type ReceiptDetails,
} from "@/lib/receipt-import";

type Screen = "login" | "register" | "recovery";
type ManagedMemberRole =
  | typeof AddMemberRequestRole.EMPLOYEE
  | typeof AddMemberRequestRole.VIEWER;

function Brand() {
  return (
    <span className="brand-lockup">
      <span className="brand-mark">
        <Wallet size={19} strokeWidth={2.4} />
      </span>
      <span className="brand-copy">
        <strong>BizTrack</strong>
        <small>PROFIT TRACKER</small>
      </span>
    </span>
  );
}

function LanguageSelector({
  language,
  label,
  onLanguageChange,
}: {
  language: AuthLanguage;
  label: string;
  onLanguageChange: (language: AuthLanguage) => void;
}) {
  return (
    <label className="language-control">
      <Languages size={14} aria-hidden="true" />
      <span className="sr-only">{label}</span>
      <select
        aria-label={label}
        value={language}
        onChange={(event) => {
          const nextLanguage = event.currentTarget.value;
          if (isAuthLanguage(nextLanguage)) {
            onLanguageChange(nextLanguage);
          }
        }}
      >
        {authLanguages.map((option) => (
          <option key={option.code} value={option.code}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function SiteHeader({
  onSupport,
  language,
  copy,
  onLanguageChange,
}: {
  onSupport: () => void;
  language: AuthLanguage;
  copy: ReturnType<typeof getAuthCopy>;
  onLanguageChange: (language: AuthLanguage) => void;
}) {
  return (
    <header className="site-header">
      <a className="brand-link" href="/" aria-label="BizTrack home">
        <Brand />
      </a>
      <div className="header-actions">
        <LanguageSelector
          language={language}
          label={copy.language}
          onLanguageChange={onLanguageChange}
        />
        <button
          className="support-link"
          type="button"
          aria-label={copy.helpSupport}
          onClick={onSupport}
        >
          <CircleHelp size={15} />
          <span className="support-label">{copy.helpSupport}</span>
        </button>
        <button
          className="avatar avatar-small"
          type="button"
          aria-label="Your profile"
        >
          S
        </button>
      </div>
    </header>
  );
}

function SiteFooter({ copy }: { copy: ReturnType<typeof getAuthCopy> }) {
  return (
    <footer className="site-footer">
      <span>{copy.footerCopyright}</span>
      <span className="footer-links">
        <a href="#security">{copy.gstCompliant}</a>
        <a href="#security">
          <ShieldCheck size={13} /> 256-bit SSL
        </a>
      </span>
    </footer>
  );
}

function PasswordField({
  label = "Password",
  placeholder,
  showLabel,
  hideLabel,
  isNewPassword = false,
  passwordTitle,
  show,
  onToggle,
}: {
  label?: string;
  placeholder: string;
  showLabel: string;
  hideLabel: string;
  isNewPassword?: boolean;
  passwordTitle?: string;
  show: boolean;
  onToggle: () => void;
}) {
  return (
    <label className="field-label">
      <span>{label}</span>
      <span className="input-wrap">
        <input
          autoComplete={isNewPassword ? "new-password" : "current-password"}
          name="password"
          minLength={8}
          pattern={
            isNewPassword ? "(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}" : undefined
          }
          placeholder={placeholder}
          required
          title={passwordTitle}
          type={show ? "text" : "password"}
        />
        <button
          className="input-icon"
          type="button"
          aria-label={show ? hideLabel : showLabel}
          onClick={onToggle}
        >
          {show ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </span>
    </label>
  );
}

function LoginCard({
  onNavigate,
  onLogin,
  isPending,
  copy,
}: {
  onNavigate: (screen: Screen) => void;
  onLogin: (event: FormEvent<HTMLFormElement>) => void;
  isPending: boolean;
  copy: ReturnType<typeof getAuthCopy>;
}) {
  const [showPassword, setShowPassword] = useState(false);
  const text = copy.login;
  return (
    <section className="auth-card" aria-labelledby="login-title">
      <div className="auth-product">
        <Brand />
        <span className="version-pill">v2.4</span>
      </div>
      <div className="secure-badge">
        <ShieldCheck size={13} /> {text.badge}
      </div>
      <h1 id="login-title">{text.title}</h1>
      <p className="auth-intro">{text.intro}</p>
      <form className="auth-form" onSubmit={onLogin} aria-busy={isPending}>
        <label className="field-label">
          <span>
            {text.email} <span className="field-note">{text.emailNote}</span>
          </span>
          <span className="input-wrap">
            <span className="input-icon static">@</span>
            <input
              autoComplete="username"
              name="email"
              placeholder={text.emailPlaceholder}
              required
              type="email"
            />
          </span>
        </label>
        <div className="password-heading">
          <span>{text.password}</span>
          <button
            className="text-button"
            type="button"
            onClick={() => onNavigate("recovery")}
          >
            {text.forgotPassword}
          </button>
        </div>
        <PasswordField
          label={text.password}
          placeholder={text.passwordPlaceholder}
          showLabel={text.showPassword}
          hideLabel={text.hidePassword}
          show={showPassword}
          onToggle={() => setShowPassword(!showPassword)}
        />
        <button className="primary-button" type="submit" disabled={isPending}>
          {isPending ? text.signingIn : text.signIn}{" "}
          {!isPending && <ArrowRight size={16} />}
        </button>
      </form>
      <div className="divider-label">
        <span>{text.securedAccess}</span>
      </div>
      <div className="trust-grid" id="security">
        <div>
          <ShieldCheck size={15} />
          <strong>{text.bankGrade}</strong>
          <small>{text.bitAes}</small>
        </div>
        <div>
          <FileSpreadsheet size={15} />
          <strong>{text.gstAuto}</strong>
          <small>{text.reconciliation}</small>
        </div>
        <div>
          <CreditCard size={15} />
          <strong>{text.noHidden}</strong>
          <small>{text.fees}</small>
        </div>
      </div>
      <p className="auth-switch">
        {text.noAccount}{" "}
        <button
          className="text-button"
          type="button"
          onClick={() => onNavigate("register")}
        >
          {text.registerLink}
        </button>
      </p>
      <p className="secure-note">
        <ShieldCheck size={13} /> {text.secureNote} <span>·</span>{" "}
        <button
          className="text-button"
          type="button"
          onClick={() => window.alert(copy.contactAlert)}
        >
          {text.contactSupport}
        </button>
      </p>
    </section>
  );
}

function RegisterCard({
  onNavigate,
  onRegister,
  isPending,
  copy,
}: {
  onNavigate: (screen: Screen) => void;
  onRegister: (event: FormEvent<HTMLFormElement>) => void;
  isPending: boolean;
  copy: ReturnType<typeof getAuthCopy>;
}) {
  const [showPassword, setShowPassword] = useState(false);
  const text = copy.register;
  return (
    <section
      className="auth-card register-card"
      aria-labelledby="register-title"
    >
      <div className="auth-product">
        <Brand />
        <span className="version-pill">v2.4</span>
      </div>
      <div className="secure-badge">
        <Building2 size={13} /> {text.badge}
      </div>
      <h1 id="register-title">{text.title}</h1>
      <p className="auth-intro">{text.intro}</p>
      <div className="included-box">
        <strong>{text.included}</strong>
        {text.benefits.map((benefit) => (
          <span key={benefit}>
            <Check size={13} /> {benefit}
          </span>
        ))}
      </div>
      <form className="auth-form" onSubmit={onRegister} aria-busy={isPending}>
        <label className="field-label">
          <span>{text.fullName}</span>
          <span className="input-wrap">
            <input
              autoComplete="name"
              name="fullName"
              placeholder={text.namePlaceholder}
              required
            />
          </span>
        </label>
        <label className="field-label">
          <span>{text.email}</span>
          <span className="input-wrap">
            <input
              autoComplete="email"
              name="email"
              placeholder={text.emailPlaceholder}
              required
              type="email"
            />
          </span>
        </label>
        <label className="field-label">
          <span>{text.businessName}</span>
          <span className="input-wrap">
            <input
              autoComplete="organization"
              name="businessName"
              placeholder={text.businessPlaceholder}
              required
            />
          </span>
        </label>
        <p className="field-hint">{text.businessHint}</p>
        <label className="field-label">
          <span>{text.role}</span>
          <span className="select-wrap">
            <select defaultValue={RegisterRequestRole.OWNER} name="role">
              <option value={RegisterRequestRole.OWNER}>{text.owner}</option>
              <option value={RegisterRequestRole.EMPLOYEE}>
                {text.employee}
              </option>
              <option value={RegisterRequestRole.VIEWER}>{text.viewer}</option>
            </select>
            <ChevronDown size={14} />
          </span>
        </label>
        <div className="field-pair">
          <label className="field-label">
            <span>{text.category}</span>
            <span className="select-wrap">
              <select defaultValue={text.categories[0]}>
                {text.categories.map((category) => (
                  <option key={category}>{category}</option>
                ))}
              </select>
              <ChevronDown size={14} />
            </span>
          </label>
          <label className="field-label">
            <span>{text.currency}</span>
            <span className="select-wrap">
              <select defaultValue={text.currencies[0]}>
                {text.currencies.map((currency) => (
                  <option key={currency}>{currency}</option>
                ))}
              </select>
              <ChevronDown size={14} />
            </span>
          </label>
        </div>
        <PasswordField
          label={text.createPassword}
          placeholder={text.createPassword}
          showLabel={copy.login.showPassword}
          hideLabel={copy.login.hidePassword}
          isNewPassword
          passwordTitle={text.passwordTitle}
          show={showPassword}
          onToggle={() => setShowPassword(!showPassword)}
        />
        <p className="field-hint password-hint">
          <ShieldCheck size={12} /> {text.passwordHint}
        </p>
        <label className="check-row terms-row">
          <input name="termsAccepted" required type="checkbox" />
          <span>
            {text.agree} <a href="#terms">{text.terms}</a>,{" "}
            <a href="#privacy">{text.privacy}</a> {text.gstGuidelines}
          </span>
        </label>
        <button className="primary-button" type="submit" disabled={isPending}>
          {isPending ? text.creatingAccount : text.createAccount}{" "}
          {!isPending && <ArrowRight size={16} />}
        </button>
      </form>
      <p className="auth-switch">
        {text.alreadyAccount}{" "}
        <button
          className="text-button"
          type="button"
          onClick={() => onNavigate("login")}
        >
          {text.signIn}
        </button>
      </p>
      <p className="secure-note">
        <ShieldCheck size={13} /> {text.secureNote} <span>·</span> v2.4.1 (Live
        Ledger)
      </p>
    </section>
  );
}

function RecoveryCard({
  onNavigate,
  copy,
}: {
  onNavigate: (screen: Screen) => void;
  copy: ReturnType<typeof getAuthCopy>;
}) {
  const text = copy.recovery;
  return (
    <section
      className="auth-card recovery-card"
      aria-labelledby="recovery-title"
    >
      <div className="auth-product">
        <Brand />
        <span className="version-pill">v2.4</span>
      </div>
      <div className="recovery-icon">
        <ShieldCheck size={24} />
      </div>
      <h1 id="recovery-title">{text.title}</h1>
      <p className="auth-intro">{text.intro}</p>
      <a className="primary-button" href="mailto:support@biztrack.in">
        {text.contactSupport} <ArrowRight size={16} />
      </a>
      <p className="auth-switch">
        <button
          className="text-button"
          type="button"
          onClick={() => onNavigate("login")}
        >
          {text.backToSignIn}
        </button>
      </p>
    </section>
  );
}

function MetricCard({
  label,
  value,
  change,
  direction,
  icon: Icon,
  tone,
  comparison = "vs last month",
}: {
  label: string;
  value: string;
  change: string;
  direction: "up" | "down";
  icon: typeof Wallet;
  tone: string;
  comparison?: string;
}) {
  return (
    <article className="metric-card">
      <div className="metric-top">
        <span>{label}</span>
        <span className={`metric-icon ${tone}`}>
          <Icon size={17} />
        </span>
      </div>
      <strong className="metric-value">{value}</strong>
      <div className="metric-foot">
        <span
          className={direction === "up" ? "change-positive" : "change-neutral"}
        >
          {direction === "up" ? (
            <ArrowUpRight size={14} />
          ) : (
            <ArrowDownRight size={14} />
          )}
          {change}
        </span>
        <span>{comparison}</span>
      </div>
    </article>
  );
}

function MembersPanel({
  canManageMembers,
  language,
}: {
  canManageMembers: boolean;
  language: AuthLanguage;
}) {
  const t = (text: string) => translateDashboardText(language, text);
  const queryClient = useQueryClient();
  const membersQuery = useList1({
    query: { enabled: canManageMembers, retry: false },
  });
  const addMemberMutation = useAdd1();
  const changeRoleMutation = useChangeRole();
  const [isAdding, setIsAdding] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<ManagedMemberRole>(
    AddMemberRequestRole.EMPLOYEE,
  );
  const [notice, setNotice] = useState("");
  const [noticeTone, setNoticeTone] = useState<"success" | "error">("success");

  function getMemberError(error: unknown, fallback: string) {
    if (!(error instanceof ApiError)) return fallback;
    if (error.status === 400)
      return t("That role cannot be assigned. Choose Employee or Viewer.");
    if (error.status === 403)
      return t("Only the business owner can manage members.");
    if (error.status === 404)
      return t("No account found for that email. Ask them to register first.");
    if (error.status === 409) return t("This person is already a member.");
    return fallback;
  }

  async function handleAddMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");

    try {
      await addMemberMutation.mutateAsync({
        data: { email: email.trim(), role },
      });
      await queryClient.invalidateQueries({ queryKey: getList1QueryKey() });
      setEmail("");
      setRole(AddMemberRequestRole.EMPLOYEE);
      setIsAdding(false);
      setNotice(t("Member added successfully."));
      setNoticeTone("success");
    } catch (error) {
      setNotice(
        getMemberError(error, t("Unable to add member. Please try again.")),
      );
      setNoticeTone("error");
    }
  }

  async function handleRoleChange(
    memberId: number | undefined,
    nextRole: ManagedMemberRole,
  ) {
    if (memberId === undefined) return;
    setNotice("");

    try {
      await changeRoleMutation.mutateAsync({
        id: memberId,
        data: { role: nextRole },
      });
      await queryClient.invalidateQueries({ queryKey: getList1QueryKey() });
      setNotice(t("Member role updated."));
      setNoticeTone("success");
    } catch (error) {
      await queryClient.invalidateQueries({ queryKey: getList1QueryKey() });
      setNotice(
        getMemberError(
          error,
          t("Unable to update member role. Please try again."),
        ),
      );
      setNoticeTone("error");
    }
  }

  return (
    <section className="panel members-panel">
      <div className="panel-heading">
        <div>
          <h2>{t("Business members")}</h2>
          <p>{t("Manage the people who can access this business")}</p>
        </div>
        {canManageMembers && (
          <button
            className="add-button"
            onClick={() => {
              setNotice("");
              setIsAdding(!isAdding);
            }}
            type="button"
          >
            <Plus size={15} />
            {isAdding ? t("Cancel") : t("Add member")}
          </button>
        )}
      </div>
      {notice && (
        <output
          className="member-notice"
          data-tone={noticeTone}
          role={noticeTone === "error" ? "alert" : "status"}
        >
          {notice}
        </output>
      )}
      {!canManageMembers ? (
        <div className="member-state">
          {t("Member management is available to the business owner only.")}
        </div>
      ) : (
        <>
          {isAdding && (
            <form className="member-add-form" onSubmit={handleAddMember}>
              <label className="field-label">
                <span>{t("Registered account email")}</span>
                <span className="input-wrap">
                  <input
                    autoComplete="email"
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="member@business.com"
                    required
                    type="email"
                    value={email}
                  />
                </span>
              </label>
              <label className="field-label">
                <span>{t("Role")}</span>
                <span className="select-wrap">
                  <select
                    onChange={(event) =>
                      setRole(event.target.value as ManagedMemberRole)
                    }
                    value={role}
                  >
                    <option value={AddMemberRequestRole.EMPLOYEE}>
                      {t("Employee")}
                    </option>
                    <option value={AddMemberRequestRole.VIEWER}>
                      {t("Viewer")}
                    </option>
                  </select>
                  <ChevronDown size={14} />
                </span>
              </label>
              <button
                className="primary-button"
                disabled={addMemberMutation.isPending}
                type="submit"
              >
                {addMemberMutation.isPending ? t("Adding…") : t("Add member")}
              </button>
              <p className="member-form-hint">
                {t(
                  "Members must have registered for BizTrack before you can add them.",
                )}
              </p>
            </form>
          )}
          {membersQuery.isPending ? (
            <output aria-live="polite">{t("Loading members…")}</output>
          ) : membersQuery.isError ? (
            <div className="member-state" role="alert">
              <p>{t("Unable to load members. Please try again.")}</p>
              <button
                className="outline-button"
                onClick={() => void membersQuery.refetch()}
                type="button"
              >
                {t("Retry")}
              </button>
            </div>
          ) : membersQuery.data.length === 0 ? (
            <div className="member-state">{t("No members found.")}</div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{t("MEMBER")}</th>
                    <th>{t("EMAIL")}</th>
                    <th>{t("ROLE")}</th>
                  </tr>
                </thead>
                <tbody>
                  {membersQuery.data.map((member) => (
                    <tr key={member.memberId ?? member.userId ?? member.email}>
                      <td>
                        <span className="member-name">
                          <span className="member-avatar">
                            {(member.fullName || member.email || "M")
                              .slice(0, 1)
                              .toUpperCase()}
                          </span>
                          <strong>
                            {member.fullName || t("Unnamed member")}
                          </strong>
                        </span>
                      </td>
                      <td>{member.email}</td>
                      <td>
                        {member.role !== MemberResponseRole.OWNER ? (
                          <span className="select-wrap member-role-select">
                            <select
                              aria-label={`${t("Role")} ${member.fullName || member.email}`}
                              disabled={
                                changeRoleMutation.isPending ||
                                member.memberId === undefined
                              }
                              onChange={(event) =>
                                void handleRoleChange(
                                  member.memberId,
                                  event.target.value as ManagedMemberRole,
                                )
                              }
                              value={member.role ?? MemberResponseRole.VIEWER}
                            >
                              <option value={RoleRequestRole.EMPLOYEE}>
                                {t("Employee")}
                              </option>
                              <option value={RoleRequestRole.VIEWER}>
                                {t("Viewer")}
                              </option>
                            </select>
                            <ChevronDown size={13} />
                          </span>
                        ) : (
                          <span className="member-role owner">
                            {t("OWNER")}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  );
}

function ProfilePanel({
  onProfileUpdated,
  language,
}: {
  onProfileUpdated: (name: string) => void;
  language: AuthLanguage;
}) {
  const t = (text: string) => translateDashboardText(language, text);
  const queryClient = useQueryClient();
  const profileQuery = useProfile({ query: { retry: false } });
  const updateProfileMutation = useUpdateProfile();
  const [notice, setNotice] = useState("");
  const [noticeTone, setNoticeTone] = useState<"success" | "error">("success");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");

    const form = event.currentTarget;
    const formData = new FormData(form);
    const newPassword = String(formData.get("newPassword") ?? "");
    const data = {
      fullName: String(formData.get("fullName") ?? "").trim(),
      email: String(formData.get("email") ?? "").trim(),
      ...(newPassword
        ? {
            currentPassword: String(formData.get("currentPassword") ?? ""),
            newPassword,
          }
        : {}),
    };

    try {
      const profile = await updateProfileMutation.mutateAsync({ data });
      queryClient.setQueryData(getProfileQueryKey(), profile);
      onProfileUpdated(profile.fullName ?? "");
      form.reset();
      setNotice(t("Profile updated successfully."));
      setNoticeTone("success");
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setNotice(
          newPassword
            ? t("Check your current password and new password requirements.")
            : t("Please check the profile details and try again."),
        );
      } else if (error instanceof ApiError && error.status === 409) {
        setNotice(t("That email address is already used by another account."));
      } else {
        setNotice(t("Unable to update your profile. Please try again."));
      }
      setNoticeTone("error");
    }
  }

  if (profileQuery.isPending) {
    return (
      <section className="panel profile-panel">
        <h2>{t("Your profile")}</h2>
        <output aria-live="polite">{t("Loading profile…")}</output>
      </section>
    );
  }

  if (profileQuery.isError || !profileQuery.data) {
    return (
      <section className="panel profile-panel">
        <h2>{t("Your profile")}</h2>
        <div className="profile-state" role="alert">
          <p>{t("Unable to load your profile. Please try again.")}</p>
          <button
            className="outline-button"
            onClick={() => void profileQuery.refetch()}
            type="button"
          >
            {t("Retry")}
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="panel profile-panel">
      <div className="panel-heading">
        <div>
          <h2>{t("Your profile")}</h2>
          <p>{t("Update your account details and password")}</p>
        </div>
      </div>
      {notice && (
        <output
          className="profile-notice"
          data-tone={noticeTone}
          role={noticeTone === "error" ? "alert" : "status"}
        >
          {notice}
        </output>
      )}
      <form className="profile-form" onSubmit={handleSubmit}>
        <label className="field-label">
          <span>{t("Full name")}</span>
          <span className="input-wrap">
            <input
              autoComplete="name"
              defaultValue={profileQuery.data.fullName ?? ""}
              maxLength={120}
              name="fullName"
              required
            />
          </span>
        </label>
        <label className="field-label">
          <span>{t("Email address")}</span>
          <span className="input-wrap">
            <input
              autoComplete="email"
              defaultValue={profileQuery.data.email ?? ""}
              name="email"
              required
              type="email"
            />
          </span>
        </label>
        <div className="profile-password-heading">
          <strong>{t("Change password")}</strong>
          <span>{t("Leave blank to keep your current password")}</span>
        </div>
        <label className="field-label">
          <span>{t("Current password")}</span>
          <span className="input-wrap">
            <input
              autoComplete="current-password"
              name="currentPassword"
              placeholder={t("Required to change password")}
              type="password"
            />
          </span>
        </label>
        <label className="field-label">
          <span>{t("New password")}</span>
          <span className="input-wrap">
            <input
              autoComplete="new-password"
              minLength={8}
              name="newPassword"
              pattern="(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}"
              placeholder={t("At least 8 characters, a number and a symbol")}
              title={t(
                "Use at least 8 characters, including a number and a symbol.",
              )}
              type="password"
            />
          </span>
        </label>
        <button
          className="primary-button profile-save-button"
          disabled={updateProfileMutation.isPending}
          type="submit"
        >
          {updateProfileMutation.isPending ? t("Saving…") : t("Save changes")}
        </button>
      </form>
    </section>
  );
}

function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatCurrency(
  value: number | undefined,
  language: AuthLanguage = "en",
) {
  return new Intl.NumberFormat(`${language}-IN`, {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function formatCompactCurrency(value: number, language: AuthLanguage = "en") {
  return new Intl.NumberFormat(`${language}-IN`, {
    notation: "compact",
    maximumFractionDigits: 0,
  }).format(value);
}

function formatPercentageChange(
  value: number | undefined,
  language: AuthLanguage = "en",
) {
  if (value === undefined) return "—";
  return `${new Intl.NumberFormat(`${language}-IN`, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
    signDisplay: "exceptZero",
  }).format(value)}%`;
}

function formatPercentValue(value: number, language: AuthLanguage) {
  return `${new Intl.NumberFormat(`${language}-IN`, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value)}%`;
}

function buildChartPath(values: number[], maximum: number) {
  if (values.length === 0) return "";
  const left = 8;
  const right = 592;
  const top = 12;
  const bottom = 154;
  const xStep = values.length > 1 ? (right - left) / (values.length - 1) : 0;
  const points = values.map((value, index) => {
    const x = left + index * xStep;
    const y = bottom - (Math.max(0, value) / maximum) * (bottom - top);
    return `${x},${y}`;
  });
  return `M${points.join(" L")}`;
}

function formatMonthLabel(
  value: string | undefined,
  language: AuthLanguage = "en",
) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(`${language}-IN`, { month: "short" }).format(
        date,
      );
}

function formatTransactionDate(
  value: string | undefined,
  language: AuthLanguage = "en",
) {
  if (!value) return "—";
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(`${language}-IN`, {
        day: "numeric",
        month: "short",
        year: "numeric",
      }).format(date);
}

function ReportsPanel({ language }: { language: AuthLanguage }) {
  const t = (text: string) => translateDashboardText(language, text);
  const [to] = useState(() => localDateString(new Date()));
  const [from] = useState(() => {
    const date = new Date();
    date.setMonth(date.getMonth() - 5, 1);
    return localDateString(date);
  });
  const [range, setRange] = useState({ from, to });
  const summaryQuery = useSummary(range, { query: { retry: false } });
  const monthlyQuery = useMonthly(range, { query: { retry: false } });
  const dashboardQuery = useDashboard({ query: { retry: false } });
  const monthlyRows = monthlyQuery.data ?? [];
  const maxMonthlyValue = Math.max(
    1,
    ...monthlyRows.map((month) =>
      Math.max(month.inflow ?? 0, month.outflow ?? 0),
    ),
  );

  function updateRange(field: "from" | "to", value: string) {
    setRange((current) => ({ ...current, [field]: value }));
  }

  return (
    <div className="reports-page">
      <div className="dashboard-heading">
        <div>
          <span className="eyebrow">{t("BUSINESS PERFORMANCE")}</span>
          <h1>{t("Reports")}</h1>
          <p>{t("Review your income, expenses, and monthly profit.")}</p>
        </div>
        <div className="report-date-filters">
          <label>
            <span>{t("From")}</span>
            <input
              aria-label={t("Report start date")}
              max={range.to}
              onChange={(event) => updateRange("from", event.target.value)}
              type="date"
              value={range.from}
            />
          </label>
          <label>
            <span>{t("To")}</span>
            <input
              aria-label={t("Report end date")}
              min={range.from}
              onChange={(event) => updateRange("to", event.target.value)}
              type="date"
              value={range.to}
            />
          </label>
        </div>
      </div>
      {summaryQuery.isError && (
        <div className="report-state" role="alert">
          <span>{t("Unable to load report totals.")}</span>
          <button
            className="outline-button"
            onClick={() => void summaryQuery.refetch()}
            type="button"
          >
            {t("Retry")}
          </button>
        </div>
      )}
      <div className="metrics-grid report-metrics">
        <MetricCard
          label={t("Income")}
          value={
            summaryQuery.isPending
              ? t("Loading…")
              : formatCurrency(summaryQuery.data?.inflow, language)
          }
          change={t("Selected period")}
          direction="up"
          icon={ArrowUpRight}
          tone="mint"
        />
        <MetricCard
          label={t("Expenses")}
          value={
            summaryQuery.isPending
              ? t("Loading…")
              : formatCurrency(summaryQuery.data?.outflow, language)
          }
          change={t("Selected period")}
          direction="down"
          icon={CreditCard}
          tone="peach"
        />
        <MetricCard
          label={t("Net profit")}
          value={
            summaryQuery.isPending
              ? t("Loading…")
              : formatCurrency(summaryQuery.data?.profit, language)
          }
          change={t("Selected period")}
          direction={(summaryQuery.data?.profit ?? 0) >= 0 ? "up" : "down"}
          icon={Wallet}
          tone="lavender"
        />
        <MetricCard
          label={t("Profit margin")}
          value={
            summaryQuery.isPending
              ? t("Loading…")
              : `${(summaryQuery.data?.marginPercent ?? 0).toFixed(1)}%`
          }
          change={t("Selected period")}
          direction="up"
          icon={FileSpreadsheet}
          tone="blue"
        />
      </div>
      <section className="panel report-panel">
        <div className="panel-heading">
          <div>
            <h2>{t("Monthly performance")}</h2>
            <p>{t("Income and expenses during the selected period")}</p>
          </div>
        </div>
        {monthlyQuery.isPending || dashboardQuery.isPending ? (
          <output aria-live="polite">{t("Loading report data…")}</output>
        ) : monthlyQuery.isError || dashboardQuery.isError ? (
          <div className="report-state" role="alert">
            <span>{t("Unable to load monthly report data.")}</span>
            <button
              className="outline-button"
              onClick={() => {
                void monthlyQuery.refetch();
                void dashboardQuery.refetch();
              }}
              type="button"
            >
              {t("Retry")}
            </button>
          </div>
        ) : monthlyRows.length === 0 ? (
          <div className="report-state">
            {t("No report data for this period.")}
          </div>
        ) : (
          <>
            <div
              className="monthly-chart"
              aria-label={t("Monthly income and expenses")}
              role="img"
            >
              {monthlyRows.map((month) => (
                <div className="monthly-chart-column" key={month.month}>
                  <div className="monthly-chart-bars">
                    <span
                      className="monthly-bar income"
                      style={{
                        height: `${Math.max(3, ((month.inflow ?? 0) / maxMonthlyValue) * 100)}%`,
                      }}
                      title={`${t("Income")} ${formatCurrency(month.inflow, language)}`}
                    />
                    <span
                      className="monthly-bar expense"
                      style={{
                        height: `${Math.max(3, ((month.outflow ?? 0) / maxMonthlyValue) * 100)}%`,
                      }}
                      title={`${t("Expenses")} ${formatCurrency(month.outflow, language)}`}
                    />
                  </div>
                  <span className="monthly-chart-label">
                    {month.month ?? "—"}
                  </span>
                </div>
              ))}
            </div>
            <div className="report-legend">
              <span>
                <i className="legend-revenue" /> {t("Income")}
              </span>
              <span>
                <i className="legend-expense" /> {t("Expenses")}
              </span>
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{t("MONTH")}</th>
                    <th>{t("INCOME")}</th>
                    <th>{t("EXPENSES")}</th>
                    <th>{t("PROFIT")}</th>
                    <th>{t("MARGIN")}</th>
                  </tr>
                </thead>
                <tbody>
                  {monthlyRows.map((month) => (
                    <tr key={`${month.month}-summary`}>
                      <td>{month.month ?? "—"}</td>
                      <td>{formatCurrency(month.inflow, language)}</td>
                      <td>{formatCurrency(month.outflow, language)}</td>
                      <td>{formatCurrency(month.profit, language)}</td>
                      <td>
                        {formatPercentValue(month.marginPercent ?? 0, language)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {dashboardQuery.data?.highestSurplusMonth && (
              <p className="report-highlight">
                {t("Highest surplus")}:{" "}
                {dashboardQuery.data.highestSurplusMonth} (
                {formatCurrency(
                  dashboardQuery.data.highestSurplusAmount,
                  language,
                )}
                )
              </p>
            )}
          </>
        )}
      </section>
    </div>
  );
}

function TransactionsPanel({ language }: { language: AuthLanguage }) {
  const t = (text: string) => translateDashboardText(language, text);
  const queryClient = useQueryClient();
  const receiptFileInput = useRef<HTMLInputElement>(null);
  const transactionFormRef = useRef<HTMLFormElement>(null);
  const [type, setType] = useState<"ALL" | "INCOME" | "EXPENSE">("ALL");
  const [categoryId, setCategoryId] = useState("");
  const [search, setSearch] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [editingTransaction, setEditingTransaction] =
    useState<Transaction | null>(null);
  const [formType, setFormType] = useState<
    | typeof TransactionRequestTransactionType.INCOME
    | typeof TransactionRequestTransactionType.EXPENSE
  >(TransactionRequestTransactionType.EXPENSE);
  const [formCategoryId, setFormCategoryId] = useState("");
  const [notice, setNotice] = useState("");
  const [noticeTone, setNoticeTone] = useState<"success" | "error">("success");
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [isDraggingReceipt, setIsDraggingReceipt] = useState(false);
  const [receiptFile, setReceiptFile] = useState<File | null>(null);
  const [receiptPreviewUrl, setReceiptPreviewUrl] = useState("");
  const [receiptProgress, setReceiptProgress] = useState(0);
  const [receiptProgressMessage, setReceiptProgressMessage] = useState("");
  const [receiptError, setReceiptError] = useState("");
  const [isExtractingReceipt, setIsExtractingReceipt] = useState(false);
  const [isSavingReceipt, setIsSavingReceipt] = useState(false);
  const [extractedDrafts, setExtractedDrafts] = useState<ReceiptDetails[]>([]);
  const [activeDraftIndex, setActiveDraftIndex] = useState(0);
  const [savedDraftIndexes, setSavedDraftIndexes] = useState<number[]>([]);
  const [receiptAttached, setReceiptAttached] = useState(false);
  const extractedDraft = extractedDrafts[activeDraftIndex] ?? null;
  const [formInstance, setFormInstance] = useState(0);
  useEffect(
    () => () => {
      if (receiptPreviewUrl) URL.revokeObjectURL(receiptPreviewUrl);
    },
    [receiptPreviewUrl],
  );
  useEffect(() => {
    if (!extractedDraft || !isEditing || isExtractingReceipt) return;
    requestAnimationFrame(() => {
      transactionFormRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
    });
  }, [extractedDraft, isEditing, isExtractingReceipt]);
  const transactionType = type === "ALL" ? undefined : (type as ListType);
  const params = {
    from: from || undefined,
    to: to || undefined,
    type: transactionType,
    categoryId: categoryId ? Number(categoryId) : undefined,
    q: search.trim() || undefined,
  };
  const transactionsQuery = useList(params, {
    query: { retry: false },
  });
  const categoriesQuery = useList2(undefined, {
    query: { retry: false },
  });
  const addMutation = useAdd();
  const updateMutation = useUpdate();
  const deleteMutation = useDelete();
  const categoryById = new Map(
    (categoriesQuery.data ?? []).map((category) => [
      category.id,
      category.categoryName,
    ]),
  );
  const editing = editingTransaction !== null;
  const activeMutation = editing ? updateMutation : addMutation;

  async function refreshRelatedData() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: getTransactionsQueryKey() }),
      queryClient.invalidateQueries({ queryKey: getSummaryQueryKey() }),
      queryClient.invalidateQueries({ queryKey: getMonthlyQueryKey() }),
      queryClient.invalidateQueries({ queryKey: getDashboardQueryKey() }),
    ]);
  }

  function showMutationError(error: unknown, fallback: string) {
    if (error instanceof ApiError && error.status === 400) {
      return t("Check the transaction details and try again.");
    }
    if (error instanceof ApiError && error.status === 403) {
      return t("Your role does not allow this transaction change.");
    }
    if (error instanceof ApiError && error.status === 404) {
      return t("That transaction or category no longer exists.");
    }
    return fallback;
  }

  function resetReceiptImport() {
    setIsImportOpen(false);
    setReceiptFile(null);
    setReceiptPreviewUrl("");
    setReceiptProgress(0);
    setReceiptProgressMessage("");
    setReceiptError("");
    setIsExtractingReceipt(false);
    setExtractedDrafts([]);
    setActiveDraftIndex(0);
    setSavedDraftIndexes([]);
    setReceiptAttached(false);
    if (receiptFileInput.current) receiptFileInput.current.value = "";
  }

  async function handleReceiptFile(file: File | undefined) {
    if (!file) return;
    const extension = file.name.toLowerCase().split(".").pop();
    if (
      !["pdf", "png", "jpg", "jpeg", "csv", "xls", "xlsx"].includes(
        extension ?? "",
      )
    ) {
      setReceiptError(t("Choose a PDF, PNG, JPG, CSV, XLS, or XLSX file."));
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setReceiptError(t("Files must be 10 MB or smaller."));
      return;
    }

    setReceiptFile(file);
    setReceiptPreviewUrl(URL.createObjectURL(file));
    setReceiptError("");
    setExtractedDrafts([]);
    setActiveDraftIndex(0);
    setSavedDraftIndexes([]);
    setReceiptAttached(false);
    setReceiptProgress(4);
    setReceiptProgressMessage(t("Preparing your file…"));
    setIsExtractingReceipt(true);
    try {
      setReceiptProgressMessage(t("Reading receipt details on the server…"));
      const details = await extractReceiptDetails(file);
      if (details.length === 0) {
        throw new Error("No transaction details were found.");
      }
      const drafts = details.map((detail) => {
        const category = (categoriesQuery.data ?? []).find(
          (candidate) =>
            candidate.categoryName?.trim().toLowerCase() ===
              detail.category.trim().toLowerCase() &&
            candidate.transactionType === detail.transactionType,
        );
        return {
          ...detail,
          categoryId: category?.id === undefined ? "" : String(category.id),
        };
      });
      setExtractedDrafts(drafts);
      setActiveDraftIndex(0);
      setSavedDraftIndexes([]);
      setFormType(
        drafts[0].transactionType as TransactionRequestTransactionType,
      );
      setFormCategoryId(drafts[0].categoryId);
      setEditingTransaction(null);
      setFormInstance((value) => value + 1);
      setIsEditing(true);
      setReceiptProgress(100);
      setReceiptProgressMessage(
        translateDashboardText(
          language,
          "{count} transactions extracted. Review each before saving.",
        ).replace("{count}", String(details.length)),
      );
      setNotice("");
    } catch (error) {
      const status = error instanceof ApiError ? error.status : 0;
      setReceiptError(
        status === 413
          ? t("This file exceeds the 10 MB upload limit.")
          : status === 415
            ? t("Choose a PDF, JPG, JPEG, PNG, CSV, XLS, or XLSX file.")
            : status === 401
              ? t("Please log in before importing a receipt.")
              : status === 503
                ? t(
                    "Receipt OCR is unavailable. Please contact the administrator.",
                  )
                : error instanceof Error &&
                    error.message.includes("No transaction details")
                  ? t(
                      "Readable text was found, but no transaction details could be identified.",
                    )
                  : t(
                      "We couldn't read transaction details from this file. Try a clearer image or PDF.",
                    ),
      );
      setReceiptProgressMessage("");
    } finally {
      setIsExtractingReceipt(false);
    }
  }

  function handleReceiptInputChange(event: ChangeEvent<HTMLInputElement>) {
    void handleReceiptFile(event.target.files?.[0]);
  }

  function handleReceiptDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setIsDraggingReceipt(false);
    void handleReceiptFile(event.dataTransfer.files[0]);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    const formData = new FormData(event.currentTarget);
    const transaction: TransactionRequest = {
      transactionType: String(
        formData.get("transactionType"),
      ) as TransactionRequestTransactionType,
      amount: Number(formData.get("amount")),
      transactionDate: String(formData.get("transactionDate") ?? ""),
      transactionTime:
        String(formData.get("transactionTime") ?? "").trim() || undefined,
      categoryId: Number(formData.get("categoryId")),
      description: String(formData.get("description") ?? "").trim(),
      paymentMethod: String(formData.get("paymentMethod") ?? "") || undefined,
    };
    const attachingReceipt =
      !editingTransaction &&
      receiptFile !== null &&
      extractedDrafts.length > 0 &&
      !receiptAttached;

    try {
      if (editingTransaction?.id !== undefined) {
        await updateMutation.mutateAsync({
          id: editingTransaction.id,
          data: transaction,
        });
      } else if (attachingReceipt && receiptFile) {
        setIsSavingReceipt(true);
        try {
          await addTransactionWithReceipt(transaction, receiptFile);
          setReceiptAttached(true);
        } finally {
          setIsSavingReceipt(false);
        }
      } else {
        await addMutation.mutateAsync({ data: transaction });
      }
      await refreshRelatedData();
      setEditingTransaction(null);
      setIsEditing(false);
      const savedCount = savedDraftIndexes.includes(activeDraftIndex)
        ? savedDraftIndexes.length
        : savedDraftIndexes.length + 1;
      const nextSavedIndexes = [
        ...new Set([...savedDraftIndexes, activeDraftIndex]),
      ];
      const nextDraftIndex = extractedDrafts.findIndex(
        (_, index) => !nextSavedIndexes.includes(index),
      );
      if (
        !editingTransaction &&
        extractedDrafts.length > 1 &&
        nextDraftIndex >= 0
      ) {
        setSavedDraftIndexes(nextSavedIndexes);
        setActiveDraftIndex(nextDraftIndex);
        setFormType(
          extractedDrafts[nextDraftIndex]
            .transactionType as TransactionRequestTransactionType,
        );
        setFormCategoryId(extractedDrafts[nextDraftIndex].categoryId);
        setEditingTransaction(null);
        setFormInstance((value) => value + 1);
        setIsEditing(true);
      } else {
        resetReceiptImport();
      }
      setNotice(
        editing
          ? t("Transaction updated.")
          : attachingReceipt && nextDraftIndex >= 0
            ? `${t("Transaction added with its receipt.")} ${translateDashboardText(
                language,
                "Review transaction {current} of {total}.",
              )
                .replace("{current}", String(savedCount + 1))
                .replace("{total}", String(extractedDrafts.length))}`
            : attachingReceipt
              ? t("Transaction added with its receipt.")
              : extractedDrafts.length > 1 && nextDraftIndex >= 0
                ? `${t("Transaction added.")} ${translateDashboardText(
                    language,
                    "Review transaction {current} of {total}.",
                  )
                    .replace("{current}", String(savedCount + 1))
                    .replace("{total}", String(extractedDrafts.length))}`
                : t("Transaction added."),
      );
      setNoticeTone("success");
    } catch (error) {
      setNotice(
        showMutationError(
          error,
          t("Unable to save transaction. Please try again."),
        ),
      );
      setNoticeTone("error");
    }
  }

  function startEditing(transaction: Transaction) {
    resetReceiptImport();
    setNotice("");
    setEditingTransaction(transaction);
    setFormType(
      transaction.transactionType ?? TransactionRequestTransactionType.EXPENSE,
    );
    setFormCategoryId(
      transaction.categoryId === undefined
        ? ""
        : String(transaction.categoryId),
    );
    setIsEditing(true);
  }

  async function handleDelete(transaction: Transaction) {
    if (transaction.id === undefined) return;
    if (
      !window.confirm(
        translateDashboardText(language, 'Delete "{description}"?').replace(
          "{description}",
          transaction.description || t("this transaction"),
        ),
      )
    ) {
      return;
    }
    setNotice("");
    try {
      await deleteMutation.mutateAsync({ id: transaction.id });
      await refreshRelatedData();
      setNotice(t("Transaction deleted."));
      setNoticeTone("success");
    } catch (error) {
      setNotice(
        showMutationError(
          error,
          t("Unable to delete transaction. Please try again."),
        ),
      );
      setNoticeTone("error");
    }
  }

  function closeForm() {
    setIsEditing(false);
    setEditingTransaction(null);
    setFormType(TransactionRequestTransactionType.EXPENSE);
    setFormCategoryId("");
    resetReceiptImport();
    setNotice("");
  }

  const missingReceiptFields = extractedDraft
    ? [
        !extractedDraft.amount && "amount",
        !extractedDraft.transactionDate && "date",
        !extractedDraft.description && "description",
        !extractedDraft.categoryId && "category",
      ].filter((field): field is string => Boolean(field))
    : [];

  return (
    <section className="panel transactions-panel transactions-page">
      <div className="panel-heading transactions-heading">
        <div>
          <h2>{t("Transactions")}</h2>
          <p>{t("Review and manage your business income and expenses")}</p>
        </div>
        <div className="transaction-header-actions">
          <button
            className="outline-button receipt-import-trigger"
            disabled={isExtractingReceipt || isSavingReceipt}
            onClick={() => {
              if (!isImportOpen) {
                setEditingTransaction(null);
                setIsEditing(false);
                setExtractedDrafts([]);
                setActiveDraftIndex(0);
                setSavedDraftIndexes([]);
                setReceiptAttached(false);
                setFormType(TransactionRequestTransactionType.EXPENSE);
                setFormCategoryId("");
              }
              setReceiptError("");
              setIsImportOpen(!isImportOpen);
              setNotice("");
            }}
            type="button"
          >
            <FileText size={15} />
            {t("Import receipt")}
          </button>
          <button
            className="add-button"
            disabled={isExtractingReceipt || isSavingReceipt}
            onClick={() => {
              resetReceiptImport();
              setNotice("");
              setEditingTransaction(null);
              setFormType(TransactionRequestTransactionType.EXPENSE);
              setFormCategoryId("");
              setFormInstance((value) => value + 1);
              setIsEditing(!isEditing);
            }}
            type="button"
          >
            <Plus size={15} />
            {isEditing && !editing ? t("Cancel") : t("Add transaction")}
          </button>
        </div>
      </div>
      {notice && (!isEditing || noticeTone === "success") && (
        <output
          className="transaction-notice"
          data-tone={noticeTone}
          role={noticeTone === "error" ? "alert" : "status"}
        >
          {notice}
        </output>
      )}
      {isImportOpen && (
        <div className="receipt-import-panel" aria-busy={isExtractingReceipt}>
          <div className="receipt-import-title">
            <div>
              <strong>{t("Import from a receipt")}</strong>
              <span>
                {t(
                  "Upload a receipt, CSV, or Excel sheet to extract transaction rows. Confirm each transaction before saving.",
                )}
              </span>
            </div>
            <button
              aria-label={t("Close receipt importer")}
              className="transaction-close"
              disabled={isExtractingReceipt || isSavingReceipt}
              onClick={() => setIsImportOpen(false)}
              type="button"
            >
              ×
            </button>
          </div>
          {!receiptFile ? (
            <section
              className={`receipt-dropzone ${isDraggingReceipt ? "is-dragging" : ""}`}
              aria-label={t("Receipt file drop area")}
              onDragEnter={(event) => {
                event.preventDefault();
                setIsDraggingReceipt(true);
              }}
              onDragLeave={(event) => {
                const nextTarget = event.relatedTarget;
                if (
                  !(nextTarget instanceof Node) ||
                  !event.currentTarget.contains(nextTarget)
                ) {
                  setIsDraggingReceipt(false);
                }
              }}
              onDragOver={(event) => event.preventDefault()}
              onDrop={handleReceiptDrop}
            >
              <span className="receipt-upload-icon">
                <Upload size={19} />
              </span>
              <strong>{t("Drag and drop your receipt here")}</strong>
              <span>{t("PDF, PNG, JPG, CSV, XLS, or XLSX · Up to 10 MB")}</span>
              <button
                className="outline-button"
                disabled={isExtractingReceipt || isSavingReceipt}
                onClick={() => receiptFileInput.current?.click()}
                type="button"
              >
                {t("Browse files")}
              </button>
            </section>
          ) : (
            <div className="receipt-file-preview">
              <div className="receipt-file-row">
                <span className="receipt-file-icon">
                  <FileCheck2 size={16} />
                </span>
                <span className="receipt-file-name">
                  <strong>{receiptFile.name}</strong>
                  <small>
                    {(receiptFile.size / (1024 * 1024)).toFixed(2)} MB
                  </small>
                </span>
                <button
                  aria-label={t("Remove receipt file")}
                  className="transaction-close"
                  disabled={isExtractingReceipt}
                  onClick={() => {
                    resetReceiptImport();
                    setIsImportOpen(true);
                  }}
                  type="button"
                >
                  ×
                </button>
              </div>
              {receiptFile.name.toLowerCase().endsWith(".pdf") ? (
                <iframe
                  className="receipt-preview-pdf"
                  src={receiptPreviewUrl}
                  title={`Preview of ${receiptFile.name}`}
                />
              ) : ["png", "jpg", "jpeg"].includes(
                  receiptFile.name.toLowerCase().split(".").pop() ?? "",
                ) ? (
                <Image
                  className="receipt-preview-image"
                  src={receiptPreviewUrl}
                  alt={`Preview of ${receiptFile.name}`}
                  width={800}
                  height={600}
                  unoptimized
                />
              ) : (
                <div className="receipt-preview-document">
                  <FileSpreadsheet size={22} />
                  <span>
                    {receiptFile.name.toLowerCase().endsWith(".csv")
                      ? t("CSV transaction data")
                      : t("Excel transaction data")}
                  </span>
                  {extractedDrafts.length > 0 && (
                    <small>
                      {extractedDrafts.length} {t("transactions found")}
                    </small>
                  )}
                </div>
              )}
              {receiptProgressMessage && (
                <div className="receipt-progress-copy" aria-live="polite">
                  {isExtractingReceipt && (
                    <LoaderCircle className="receipt-spinner" size={14} />
                  )}
                  {receiptProgressMessage}
                </div>
              )}
              {(isExtractingReceipt || receiptProgress > 0) && (
                <div
                  className="receipt-progress-track"
                  role="progressbar"
                  aria-label={t("Receipt reading progress")}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={receiptProgress}
                >
                  <span style={{ width: `${receiptProgress}%` }} />
                </div>
              )}
              {extractedDraft && !isExtractingReceipt && (
                <output className="receipt-import-success">
                  {extractedDrafts.length}{" "}
                  {t(
                    "transactions extracted. Review each transaction and confirm it before saving.",
                  )}
                </output>
              )}
              {extractedDraft && !isExtractingReceipt && (
                <small className="receipt-import-success">
                  {[
                    extractedDraft.category &&
                      `${t("Category")}: ${extractedDraft.category}`,
                    extractedDraft.transactionId &&
                      `${t("Transaction ID")}: ${extractedDraft.transactionId}`,
                    extractedDraft.taxAmount &&
                      `Tax: ${extractedDraft.currency === "INR" ? "₹" : ""}${extractedDraft.taxAmount}`,
                    extractedDraft.receiptReference &&
                      `${t("Document reference")}: ${extractedDraft.receiptReference}`,
                    extractedDraft.confidence !== null &&
                      `${t("Confidence")}: ${Math.round(extractedDraft.confidence * 100)}%`,
                  ]
                    .filter(Boolean)
                    .join(" · ")}
                </small>
              )}
              {extractedDrafts.length > 1 && !isExtractingReceipt && (
                <label className="receipt-draft-selector">
                  <span>{t("Transaction to review")}</span>
                  <span className="select-wrap">
                    <select
                      onChange={(event) => {
                        const index = Number(event.target.value);
                        const draft = extractedDrafts[index];
                        if (!draft) return;
                        setActiveDraftIndex(index);
                        setFormType(
                          draft.transactionType as TransactionRequestTransactionType,
                        );
                        setFormCategoryId(draft.categoryId);
                        setEditingTransaction(null);
                        setFormInstance((value) => value + 1);
                        setIsEditing(true);
                      }}
                      value={String(activeDraftIndex)}
                    >
                      {extractedDrafts.map((draft, index) => (
                        <option
                          key={`${draft.transactionDate}-${draft.amount}-${index}`}
                          disabled={savedDraftIndexes.includes(index)}
                          value={index}
                        >
                          {savedDraftIndexes.includes(index)
                            ? `${t("Saved")} · `
                            : ""}
                          {draft.merchant ||
                            draft.description ||
                            `${t("Transaction")} ${index + 1}`}
                          {draft.amount ? ` · ₹${draft.amount}` : ""}
                        </option>
                      ))}
                    </select>
                    <ChevronDown size={14} />
                  </span>
                </label>
              )}
            </div>
          )}
          <input
            ref={receiptFileInput}
            accept=".pdf,.png,.jpg,.jpeg,.csv,.xls,.xlsx,application/pdf,image/png,image/jpeg,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            className="receipt-file-input"
            onChange={handleReceiptInputChange}
            type="file"
          />
          {receiptError && (
            <p className="receipt-import-error" role="alert">
              {receiptError}
            </p>
          )}
        </div>
      )}
      {isEditing && (
        <form
          ref={transactionFormRef}
          className="transaction-form"
          key={editingTransaction?.id ?? formInstance}
          onSubmit={handleSubmit}
        >
          <div className="transaction-form-heading">
            <strong>
              {editingTransaction
                ? t("Edit transaction")
                : t("Add transaction")}
            </strong>
            <button
              aria-label={t("Close transaction form")}
              className="transaction-close"
              disabled={isSavingReceipt}
              onClick={closeForm}
              type="button"
            >
              ×
            </button>
          </div>
          {extractedDraft && missingReceiptFields.length > 0 && (
            <output className="receipt-review-hint">
              {t("Not detected")}: {missingReceiptFields.map(t).join(", ")}.{" "}
              {t("Fill these in before saving.")}
            </output>
          )}
          {notice && noticeTone === "error" && (
            <output
              className="transaction-notice transaction-form-notice"
              role="alert"
            >
              {notice}
            </output>
          )}
          <label className="field-label">
            <span>{t("Description")}</span>
            <span className="input-wrap">
              <input
                defaultValue={
                  editingTransaction?.description ??
                  extractedDraft?.description ??
                  ""
                }
                maxLength={500}
                name="description"
                placeholder={t("e.g. Office supplies")}
                required
              />
            </span>
          </label>
          <label className="field-label">
            <span>{t("Type")}</span>
            <span className="select-wrap">
              <select
                onChange={(event) => {
                  setFormType(
                    event.target.value as
                      | typeof TransactionRequestTransactionType.INCOME
                      | typeof TransactionRequestTransactionType.EXPENSE,
                  );
                  setFormCategoryId("");
                }}
                name="transactionType"
                value={formType}
              >
                <option value={TransactionRequestTransactionType.EXPENSE}>
                  {t("Expense")}
                </option>
                <option value={TransactionRequestTransactionType.INCOME}>
                  {t("Income")}
                </option>
              </select>
              <ChevronDown size={14} />
            </span>
          </label>
          <label className="field-label">
            <span>{t("Category")}</span>
            <span className="select-wrap">
              <select
                name="categoryId"
                onChange={(event) => setFormCategoryId(event.target.value)}
                required
                value={formCategoryId}
              >
                <option value="" disabled>
                  {t("Select category")}
                </option>
                {(categoriesQuery.data ?? [])
                  .filter((category) => category.transactionType === formType)
                  .map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.categoryName}
                    </option>
                  ))}
              </select>
              <ChevronDown size={14} />
            </span>
            {!editingTransaction &&
              extractedDraft &&
              !extractedDraft.categoryId && (
                <small className="receipt-category-help">
                  {t("Choose a category to save this transaction.")}
                </small>
              )}
          </label>
          <label className="field-label">
            <span>{t("Amount")} (₹)</span>
            <span className="input-wrap">
              <input
                defaultValue={
                  editingTransaction?.amount ?? extractedDraft?.amount ?? ""
                }
                min="0.01"
                name="amount"
                required
                step="0.01"
                type="number"
              />
            </span>
          </label>
          <label className="field-label">
            <span>{t("Date")}</span>
            <span className="input-wrap">
              <input
                defaultValue={
                  editingTransaction?.transactionDate ??
                  (extractedDraft
                    ? extractedDraft.transactionDate
                    : localDateString(new Date()))
                }
                name="transactionDate"
                required
                type="date"
              />
            </span>
          </label>
          <label className="field-label">
            <span>
              {t("Time")} ({t("optional")})
            </span>
            <span className="input-wrap">
              <input
                defaultValue={
                  editingTransaction?.transactionTime ??
                  extractedDraft?.transactionTime ??
                  ""
                }
                name="transactionTime"
                step="1"
                type="time"
              />
            </span>
          </label>
          <label className="field-label">
            <span>{t("Payment method")}</span>
            <span className="select-wrap">
              <select
                defaultValue={
                  editingTransaction?.paymentMethod ??
                  extractedDraft?.paymentMethod ??
                  ""
                }
                name="paymentMethod"
              >
                <option value="">{t("Not specified")}</option>
                <option value="Cash">{t("Cash")}</option>
                <option value="UPI">UPI</option>
                <option value="Debit Card">{t("Debit Card")}</option>
                <option value="Credit Card">{t("Credit Card")}</option>
                <option value="Bank Transfer">{t("Bank Transfer")}</option>
                <option value="Cheque">{t("Cheque")}</option>
                <option value="Other">{t("Other")}</option>
              </select>
              <ChevronDown size={14} />
            </span>
          </label>
          <div className="transaction-form-actions">
            <button
              className="outline-button"
              disabled={isSavingReceipt}
              onClick={closeForm}
              type="button"
            >
              {t("Cancel")}
            </button>
            <button
              className="add-button"
              disabled={activeMutation.isPending || isSavingReceipt}
              type="submit"
            >
              {activeMutation.isPending || isSavingReceipt
                ? t("Saving…")
                : editing
                  ? t("Save changes")
                  : t("Add transaction")}
            </button>
          </div>
        </form>
      )}
      <div className="transaction-filters">
        <label className="transaction-search">
          <span className="sr-only">{t("Search transactions")}</span>
          <Search size={15} />
          <input
            onChange={(event) => setSearch(event.target.value)}
            placeholder={t("Search description")}
            value={search}
          />
        </label>
        <label>
          <span className="sr-only">{t("Type")}</span>
          <select
            onChange={(event) => {
              setType(event.target.value as "ALL" | "INCOME" | "EXPENSE");
              setCategoryId("");
            }}
            value={type}
          >
            <option value="ALL">{t("All types")}</option>
            <option value={ListType.INCOME}>{t("Income")}</option>
            <option value={ListType.EXPENSE}>{t("Expense")}</option>
          </select>
        </label>
        <label>
          <span className="sr-only">{t("Category")}</span>
          <select
            onChange={(event) => setCategoryId(event.target.value)}
            value={categoryId}
          >
            <option value="">{t("All categories")}</option>
            {(categoriesQuery.data ?? [])
              .filter(
                (category) =>
                  type === "ALL" || category.transactionType === type,
              )
              .map((category) => (
                <option key={category.id} value={category.id}>
                  {category.categoryName}
                </option>
              ))}
          </select>
        </label>
        <label>
          <span className="sr-only">{t("From date")}</span>
          <input
            aria-label={t("Filter transactions from")}
            max={to || undefined}
            onChange={(event) => setFrom(event.target.value)}
            type="date"
            value={from}
          />
        </label>
        <label>
          <span className="sr-only">{t("To date")}</span>
          <input
            aria-label={t("Filter transactions to")}
            min={from || undefined}
            onChange={(event) => setTo(event.target.value)}
            type="date"
            value={to}
          />
        </label>
      </div>
      {transactionsQuery.isPending || categoriesQuery.isPending ? (
        <output aria-live="polite">{t("Loading transactions…")}</output>
      ) : transactionsQuery.isError || categoriesQuery.isError ? (
        <div className="transaction-state" role="alert">
          <p>{t("Unable to load transactions. Please try again.")}</p>
          <button
            className="outline-button"
            onClick={() => {
              void transactionsQuery.refetch();
              void categoriesQuery.refetch();
            }}
            type="button"
          >
            {t("Retry")}
          </button>
        </div>
      ) : transactionsQuery.data.length === 0 ? (
        <div className="transaction-state">
          {t("No transactions match these filters.")}
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{t("TRANSACTION")}</th>
                <th>{t("TYPE")}</th>
                <th>{t("CATEGORY")}</th>
                <th>{t("PAYMENT METHOD")}</th>
                <th>{t("DATE")}</th>
                <th>{t("AMOUNT")}</th>
                <th>{t("ACTIONS")}</th>
              </tr>
            </thead>
            <tbody>
              {transactionsQuery.data.map((transaction) => {
                const isIncome =
                  transaction.transactionType ===
                  TransactionRequestTransactionType.INCOME;
                return (
                  <tr key={transaction.id}>
                    <td>
                      <span className="transaction-description">
                        <span
                          className={`transaction-icon ${isIncome ? "mint" : "peach"}`}
                        >
                          {isIncome ? (
                            <ArrowUpRight size={15} />
                          ) : (
                            <ShoppingCart size={15} />
                          )}
                        </span>
                        <strong>{transaction.description}</strong>
                      </span>
                    </td>
                    <td>{isIncome ? t("Income") : t("Expense")}</td>
                    <td>
                      {transaction.categoryId === undefined
                        ? "—"
                        : (categoryById.get(transaction.categoryId) ??
                          t("Category"))}
                    </td>
                    <td>{transaction.paymentMethod || "—"}</td>
                    <td>
                      {formatTransactionDate(
                        transaction.transactionDate,
                        language,
                      )}
                      {transaction.transactionTime && (
                        <>
                          <br />
                          <small>
                            {transaction.transactionTime.slice(0, 5)}
                          </small>
                        </>
                      )}
                    </td>
                    <td className={isIncome ? "amount-in" : ""}>
                      {isIncome ? "+" : "−"}
                      {formatCurrency(transaction.amount, language)}
                    </td>
                    <td>
                      <span className="transaction-row-actions">
                        <button
                          aria-label={`${t("Edit")} ${transaction.description}`}
                          className="transaction-icon-button"
                          disabled={
                            transaction.id === undefined ||
                            updateMutation.isPending ||
                            deleteMutation.isPending
                          }
                          onClick={() => startEditing(transaction)}
                          type="button"
                        >
                          <Pencil size={14} />
                        </button>
                        <button
                          aria-label={`${t("Delete")} ${transaction.description}`}
                          className="transaction-icon-button danger"
                          disabled={
                            transaction.id === undefined ||
                            deleteMutation.isPending ||
                            updateMutation.isPending
                          }
                          onClick={() => void handleDelete(transaction)}
                          type="button"
                        >
                          <Trash2 size={14} />
                        </button>
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function Dashboard({
  onLogout,
  user,
  displayName,
  isLoggingOut,
  notice,
  onProfileUpdated,
  language,
  onLanguageChange,
}: {
  onLogout: () => void;
  user: Access;
  displayName: string;
  isLoggingOut: boolean;
  notice: string;
  onProfileUpdated: (name: string) => void;
  language: AuthLanguage;
  onLanguageChange: (language: AuthLanguage) => void;
}) {
  const t = (text: string) => translateDashboardText(language, text);
  const [activeNav, setActiveNav] = useState("Overview");
  const [categoryType, setCategoryType] = useState<
    "ALL" | "INCOME" | "EXPENSE"
  >("ALL");
  const profileQuery = useProfile({ query: { retry: false } });
  const userDisplayName =
    profileQuery.data?.fullName?.trim() || displayName.trim();
  const categoriesQuery = useList2(
    categoryType === "ALL" ? undefined : { type: categoryType },
    { query: { enabled: activeNav === "Categories", retry: false } },
  );
  const overviewQuery = useDashboard({
    query: { enabled: activeNav === "Overview", retry: false },
  });
  const overviewTransactionsQuery = useList(undefined, {
    query: { enabled: activeNav === "Overview", retry: false },
  });
  const overviewCategoriesQuery = useList2(undefined, {
    query: { enabled: activeNav === "Overview", retry: false },
  });
  const overviewMonths = overviewQuery.data?.months ?? [];
  const chartValues = overviewMonths.flatMap((month) => [
    month.inflow ?? 0,
    month.outflow ?? 0,
  ]);
  const chartMaximum = Math.max(1, ...chartValues);
  const incomePath = buildChartPath(
    overviewMonths.map((month) => month.inflow ?? 0),
    chartMaximum,
  );
  const expensePath = buildChartPath(
    overviewMonths.map((month) => month.outflow ?? 0),
    chartMaximum,
  );
  const recentTransactions = [...(overviewTransactionsQuery.data ?? [])]
    .sort((a, b) =>
      (b.transactionDate ?? "").localeCompare(a.transactionDate ?? ""),
    )
    .slice(0, 4);
  const overviewCategoryNames = new Map(
    (overviewCategoriesQuery.data ?? []).map((category) => [
      category.id,
      category.categoryName,
    ]),
  );

  return (
    <div className="dashboard-shell">
      <aside className="sidebar">
        <a className="brand-link side-brand" href="/">
          <Brand />
        </a>
        <div className="workspace-label">{t("WORKSPACE")}</div>
        <button
          className={`nav-item ${activeNav === "Overview" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveNav("Overview")}
        >
          <LayoutDashboard size={17} /> {t("Overview")}
        </button>
        <button
          className={`nav-item ${activeNav === "Transactions" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveNav("Transactions")}
        >
          <CreditCard size={17} /> {t("Transactions")}
          {(overviewTransactionsQuery.data?.length ?? 0) > 0 && (
            <span className="nav-count">
              {overviewTransactionsQuery.data?.length}
            </span>
          )}
        </button>
        <a className="nav-item credits-nav-item" href="/credits">
          <Wallet size={17} /> Credits
        </a>
        <button
          className={`nav-item ${activeNav === "Reports" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveNav("Reports")}
        >
          <FileSpreadsheet size={17} /> {t("Reports")}
        </button>
        <button
          className={`nav-item ${activeNav === "Categories" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveNav("Categories")}
        >
          <Tags size={17} /> {t("Categories")}
        </button>
        <button
          className={`nav-item ${activeNav === "Members" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveNav("Members")}
        >
          <Users size={17} /> {t("Members")}
        </button>
        <button
          className={`nav-item ${activeNav === "Profile" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveNav("Profile")}
        >
          <UserRound size={17} /> {t("Profile")}
        </button>
        <div className="sidebar-bottom">
          <div className="plan-card">
            <span className="plan-icon">
              <ShieldCheck size={16} />
            </span>
            <strong>{t("All systems secure")}</strong>
            <small>{t("Your data is protected")}</small>
            <a href="#security">
              {t("View security center")} <ArrowRight size={12} />
            </a>
          </div>
          <button
            className="nav-item logout-item"
            type="button"
            onClick={onLogout}
            disabled={isLoggingOut}
          >
            <LogOut size={17} />{" "}
            {isLoggingOut ? t("Signing out…") : t("Sign out")}
          </button>
        </div>
      </aside>
      <main className="dashboard-main">
        <header className="dashboard-topbar">
          <button
            className="mobile-menu"
            type="button"
            aria-label={t("Open menu")}
          >
            <Menu size={20} />
          </button>
          <div className="breadcrumb">
            {t("Your business")} <span>/</span> <strong>{t(activeNav)}</strong>
          </div>
          <div className="topbar-actions">
            <LanguageSelector
              language={language}
              label={getAuthCopy(language).language}
              onLanguageChange={onLanguageChange}
            />
            <button
              className="search-shortcut"
              type="button"
              aria-label={t("Search")}
            >
              <Search size={17} />
              <span>{t("Search")}</span>
              <kbd>⌘ K</kbd>
            </button>
            <button
              className="icon-button"
              type="button"
              aria-label={t("Notifications")}
            >
              <Bell size={18} />
              <i />
            </button>
            <button
              className="account-chip"
              onClick={() => setActiveNav("Profile")}
              type="button"
            >
              <span className="avatar avatar-small">
                {(userDisplayName || "U").slice(0, 1).toUpperCase()}
              </span>
              <span>
                {userDisplayName || t("Account")}
                <small>{user.role ?? "Member"}</small>
              </span>
              <ChevronDown size={14} />
            </button>
          </div>
        </header>
        <div className="dashboard-content">
          {notice && (
            <output className="notice-message" data-tone="error" role="alert">
              {notice}
            </output>
          )}
          {activeNav === "Members" ? (
            <MembersPanel
              canManageMembers={user.role === "OWNER"}
              language={language}
            />
          ) : activeNav === "Profile" ? (
            <ProfilePanel
              onProfileUpdated={onProfileUpdated}
              language={language}
            />
          ) : activeNav === "Transactions" ? (
            <TransactionsPanel language={language} />
          ) : activeNav === "Reports" ? (
            <ReportsPanel language={language} />
          ) : activeNav === "Categories" ? (
            <section className="panel categories-panel">
              <div className="panel-heading">
                <div>
                  <h2>{t("Transaction categories")}</h2>
                  <p>{t("Categories available for your business")}</p>
                </div>
                <fieldset className="category-filters">
                  <legend className="category-filter-legend">
                    {t("Filter categories")}
                  </legend>
                  {(["ALL", "INCOME", "EXPENSE"] as const).map((type) => (
                    <button
                      className={categoryType === type ? "active" : ""}
                      key={type}
                      onClick={() => setCategoryType(type)}
                      type="button"
                      aria-pressed={categoryType === type}
                    >
                      {type === "ALL"
                        ? t("All")
                        : type === CategoryTransactionType.INCOME
                          ? t("Income")
                          : t("Expense")}
                    </button>
                  ))}
                </fieldset>
              </div>
              {categoriesQuery.isPending ? (
                <output aria-live="polite">{t("Loading…")}</output>
              ) : categoriesQuery.isError ? (
                <div className="category-state" role="alert">
                  <p>{t("Unable to load categories. Please try again.")}</p>
                  <button
                    className="outline-button"
                    onClick={() => void categoriesQuery.refetch()}
                    type="button"
                  >
                    {t("Retry")}
                  </button>
                </div>
              ) : categoriesQuery.data.length === 0 ? (
                <p className="category-state">
                  {t("No categories found.")}
                  {categoryType !== "ALL" && ` (${t(categoryType)})`}
                </p>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>{t("CATEGORY")}</th>
                        <th>{t("TYPE")}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {categoriesQuery.data.map((category) => (
                        <tr key={category.id}>
                          <td>
                            <span className="category-name">
                              <Tags size={15} />
                              <strong>{category.categoryName}</strong>
                            </span>
                          </td>
                          <td>
                            <span
                              className={`category-type ${category.transactionType?.toLowerCase() ?? ""}`}
                            >
                              {category.transactionType ===
                              CategoryTransactionType.INCOME
                                ? t("Income")
                                : t("Expense")}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          ) : (
            <>
              <div className="dashboard-heading">
                <div>
                  <span className="eyebrow">
                    {new Intl.DateTimeFormat(`${language}-IN`, {
                      weekday: "long",
                      month: "long",
                      day: "numeric",
                      year: "numeric",
                    })
                      .format(new Date())
                      .toUpperCase()}
                  </span>
                  <h1>
                    {t("Welcome, {name}").replace(
                      "{name}",
                      userDisplayName || "back",
                    )}{" "}
                    <span>☀</span>
                  </h1>
                  <p>
                    {t(
                      "Here's your business performance for the last six months.",
                    )}
                  </p>
                </div>
                <span className="date-button">
                  <CalendarDays size={15} /> {t("Last 6 months")}
                </span>
              </div>
              {overviewQuery.isError && (
                <div className="report-state" role="alert">
                  <span>{t("Unable to load dashboard totals and chart.")}</span>
                  <button
                    className="outline-button"
                    onClick={() => void overviewQuery.refetch()}
                    type="button"
                  >
                    {t("Retry")}
                  </button>
                </div>
              )}
              <div className="metrics-grid">
                <MetricCard
                  label={t("Total income")}
                  value={
                    overviewQuery.isPending
                      ? t("Loading…")
                      : formatCurrency(
                          overviewQuery.data?.totalInflow,
                          language,
                        )
                  }
                  change={formatPercentageChange(
                    overviewQuery.data?.inflowVsLastMonthPercent,
                    language,
                  )}
                  direction={
                    (overviewQuery.data?.inflowVsLastMonthPercent ?? 0) >= 0
                      ? "up"
                      : "down"
                  }
                  icon={Wallet}
                  tone="mint"
                />
                <MetricCard
                  label={t("Total expenses")}
                  value={
                    overviewQuery.isPending
                      ? t("Loading…")
                      : formatCurrency(
                          overviewQuery.data?.totalOutflow,
                          language,
                        )
                  }
                  change={formatPercentageChange(
                    overviewQuery.data?.outflowVsLastMonthPercent,
                    language,
                  )}
                  direction={
                    (overviewQuery.data?.outflowVsLastMonthPercent ?? 0) <= 0
                      ? "up"
                      : "down"
                  }
                  icon={CreditCard}
                  tone="peach"
                />
                <MetricCard
                  label={t("Net profit")}
                  value={
                    overviewQuery.isPending
                      ? t("Loading…")
                      : formatCurrency(overviewQuery.data?.netProfit, language)
                  }
                  change={t("Last 6 months")}
                  comparison={t("summary period")}
                  direction={
                    (overviewQuery.data?.netProfit ?? 0) >= 0 ? "up" : "down"
                  }
                  icon={ArrowUpRight}
                  tone="lavender"
                />
                <MetricCard
                  label={t("Profit margin")}
                  value={
                    overviewQuery.isPending
                      ? t("Loading…")
                      : formatPercentValue(
                          overviewQuery.data?.marginPercent ?? 0,
                          language,
                        )
                  }
                  change={t("Last 6 months")}
                  comparison={t("summary period")}
                  direction={
                    (overviewQuery.data?.marginPercent ?? 0) >= 0
                      ? "up"
                      : "down"
                  }
                  icon={FileSpreadsheet}
                  tone="blue"
                />
              </div>
              <div className="dashboard-panels">
                <section className="panel revenue-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>{t("Revenue overview")}</h2>
                      <p>{t("Income and expenses over time")}</p>
                    </div>
                    <span className="period-select">{t("Monthly")}</span>
                  </div>
                  <div className="chart-legend">
                    <span>
                      <i className="legend-revenue" /> {t("Revenue")}
                    </span>
                    <span>
                      <i className="legend-expense" /> {t("Expenses")}
                    </span>
                  </div>
                  {overviewQuery.isPending ? (
                    <output className="overview-chart-state" aria-live="polite">
                      {t("Loading monthly performance…")}
                    </output>
                  ) : overviewQuery.isError ? (
                    <div className="overview-chart-state" role="alert">
                      {t("Monthly performance is unavailable.")}
                    </div>
                  ) : overviewMonths.length === 0 ? (
                    <div className="overview-chart-state">
                      {t("No monthly activity to chart yet.")}
                    </div>
                  ) : (
                    <div className="chart-area">
                      <div className="chart-ylabels">
                        {[1, 0.75, 0.5, 0.25, 0].map((fraction) => (
                          <span key={fraction}>
                            {formatCompactCurrency(
                              chartMaximum * fraction,
                              language,
                            )}
                          </span>
                        ))}
                      </div>
                      <div className="chart-plot">
                        <div className="chart-gridlines">
                          <i />
                          <i />
                          <i />
                          <i />
                          <i />
                        </div>
                        <svg
                          className="line-chart"
                          viewBox="0 0 600 175"
                          preserveAspectRatio="none"
                          role="img"
                          aria-label={t(
                            "Monthly income and expenses for the last six months",
                          )}
                        >
                          <defs>
                            <linearGradient
                              id="revenue-fill"
                              x1="0"
                              x2="0"
                              y1="0"
                              y2="1"
                            >
                              <stop
                                offset="0"
                                stopColor="#13877d"
                                stopOpacity=".18"
                              />
                              <stop
                                offset="1"
                                stopColor="#13877d"
                                stopOpacity="0"
                              />
                            </linearGradient>
                          </defs>
                          <path
                            d={`${incomePath} L592,160 L8,160 Z`}
                            fill="url(#revenue-fill)"
                          />
                          <path
                            d={incomePath}
                            fill="none"
                            stroke="#16877d"
                            strokeWidth="3"
                            vectorEffect="non-scaling-stroke"
                            strokeLinecap="round"
                          />
                          <path
                            d={expensePath}
                            fill="none"
                            stroke="#a8b4c8"
                            strokeWidth="2"
                            strokeDasharray="5 5"
                            vectorEffect="non-scaling-stroke"
                          />
                        </svg>
                        <div className="chart-xlabels">
                          {overviewMonths.map((month) => (
                            <span key={month.month}>
                              {formatMonthLabel(month.month, language)}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </section>
                <section className="panel profit-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>{t("Profit margin")}</h2>
                      <p>{t("Healthy growth this month")}</p>
                    </div>
                    <span className="report-period-label">
                      {t("Last 6 months")}
                    </span>
                  </div>
                  <div
                    className="profit-ring"
                    style={{
                      background: `conic-gradient(#16877d 0 ${Math.min(100, Math.max(0, overviewQuery.data?.marginPercent ?? 0))}%, #eaf3f1 ${Math.min(100, Math.max(0, overviewQuery.data?.marginPercent ?? 0))}% 100%)`,
                    }}
                  >
                    <div>
                      <strong>
                        {overviewQuery.isPending
                          ? "…"
                          : formatPercentValue(
                              overviewQuery.data?.marginPercent ?? 0,
                              language,
                            )}
                      </strong>
                      <span>{t("Net margin")}</span>
                    </div>
                  </div>
                  <div className="profit-stat">
                    <span>
                      <i className="legend-revenue" /> Income
                    </span>
                    <strong>
                      {overviewQuery.isPending
                        ? t("Loading…")
                        : formatCurrency(
                            overviewQuery.data?.totalInflow,
                            language,
                          )}
                    </strong>
                  </div>
                  <div className="profit-stat">
                    <span>
                      <i className="legend-expense" /> {t("Expenses")}
                    </span>
                    <strong>
                      {overviewQuery.isPending
                        ? t("Loading…")
                        : formatCurrency(
                            overviewQuery.data?.totalOutflow,
                            language,
                          )}
                    </strong>
                  </div>
                  <div className="profit-summary">
                    <ArrowUpRight size={15} />
                    {overviewQuery.isPending
                      ? t("Loading income trend…")
                      : `${formatPercentageChange(overviewQuery.data?.inflowVsLastMonthPercent, language)} ${t("income vs last month")}`}
                  </div>
                </section>
              </div>
              <section className="panel transactions-panel">
                <div className="panel-heading transactions-heading">
                  <div>
                    <h2>{t("Recent transactions")}</h2>
                    <p>{t("A quick look at your latest activity")}</p>
                  </div>
                  <div className="transaction-actions">
                    <button
                      className="outline-button"
                      onClick={() => setActiveNav("Transactions")}
                      type="button"
                    >
                      <span>{t("View all")}</span>
                      <ArrowRight size={14} />
                    </button>
                    <button
                      className="add-button"
                      onClick={() => setActiveNav("Transactions")}
                      type="button"
                    >
                      <Plus size={15} /> {t("Add transaction")}
                    </button>
                  </div>
                </div>
                <div className="table-wrap">
                  {overviewTransactionsQuery.isPending ||
                  overviewCategoriesQuery.isPending ? (
                    <output
                      className="overview-transactions-state"
                      aria-live="polite"
                    >
                      {t("Loading recent transactions…")}
                    </output>
                  ) : overviewTransactionsQuery.isError ||
                    overviewCategoriesQuery.isError ? (
                    <div className="overview-transactions-state" role="alert">
                      <span>{t("Unable to load recent transactions.")}</span>
                      <button
                        className="outline-button"
                        onClick={() => {
                          void overviewTransactionsQuery.refetch();
                          void overviewCategoriesQuery.refetch();
                        }}
                        type="button"
                      >
                        {t("Retry")}
                      </button>
                    </div>
                  ) : recentTransactions.length === 0 ? (
                    <div className="overview-transactions-state">
                      {t(
                        "No transactions yet. Add a transaction to see it here.",
                      )}
                    </div>
                  ) : (
                    <table>
                      <thead>
                        <tr>
                          <th>{t("TRANSACTION")}</th>
                          <th>{t("CATEGORY")}</th>
                          <th>{t("DATE")}</th>
                          <th>{t("AMOUNT")}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {recentTransactions.map((transaction) => {
                          const isIncome =
                            transaction.transactionType ===
                            TransactionRequestTransactionType.INCOME;
                          return (
                            <tr key={transaction.id}>
                              <td>
                                <span
                                  className={`transaction-icon ${isIncome ? "mint" : "peach"}`}
                                >
                                  {isIncome ? (
                                    <ArrowUpRight size={15} />
                                  ) : (
                                    <ShoppingCart size={15} />
                                  )}
                                </span>
                                <strong>
                                  {transaction.description || t("Transaction")}
                                </strong>
                              </td>
                              <td>
                                {transaction.categoryId === undefined
                                  ? "—"
                                  : (overviewCategoryNames.get(
                                      transaction.categoryId,
                                    ) ?? t("Category"))}
                              </td>
                              <td>
                                {formatTransactionDate(
                                  transaction.transactionDate,
                                  language,
                                )}
                              </td>
                              <td className={isIncome ? "amount-in" : ""}>
                                {isIncome ? "+" : "−"}
                                {formatCurrency(transaction.amount, language)}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
              </section>
              <div className="dashboard-footnote">
                <ShieldCheck size={13} />{" "}
                {t("Your financial data is encrypted and securely synced")}{" "}
                <span>·</span> <a href="#security">{t("Security center")}</a>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  );
}

export default function Home() {
  const [screen, setScreen] = useState<Screen>("login");
  const [language, setLanguage] = useState<AuthLanguage>("en");
  const [notice, setNotice] = useState("");
  const [noticeTone, setNoticeTone] = useState<"error" | "success">("error");
  const [displayName, setDisplayName] = useState("");
  const copy = getAuthCopy(language);
  const queryClient = useQueryClient();
  const meQuery = useMe({ query: { retry: false } });
  const loginMutation = useLogin();
  const registerMutation = useRegister();
  const logoutMutation = useLogout();

  useEffect(() => {
    const storedLanguage = window.localStorage.getItem("biztrack-language");
    if (storedLanguage && isAuthLanguage(storedLanguage)) {
      setLanguage(storedLanguage);
      document.documentElement.lang = storedLanguage;
    }
  }, []);

  function handleLanguageChange(nextLanguage: AuthLanguage) {
    setLanguage(nextLanguage);
    window.localStorage.setItem("biztrack-language", nextLanguage);
    document.documentElement.lang = nextLanguage;
  }

  function errorMessage(error: unknown, fallback: string) {
    if (error instanceof ApiError && error.status === 401) {
      return copy.messages.incorrectCredentials;
    }
    if (error instanceof ApiError && error.status === 403) {
      return copy.messages.unauthorized;
    }
    return fallback;
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    const formData = new FormData(event.currentTarget);

    try {
      const result = await loginMutation.mutateAsync({
        data: {
          email: String(formData.get("email") ?? ""),
          password: String(formData.get("password") ?? ""),
        },
      });

      queryClient.setQueryData<Access>(getMeQueryKey(), {
        userId: result.userId,
        businessId: result.businessId,
        role: result.role,
      });
      setDisplayName(result.fullName ?? "");
    } catch (error) {
      setNotice(errorMessage(error, copy.messages.signInFailed));
      setNoticeTone("error");
    }
  }

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    const formData = new FormData(event.currentTarget);
    const role = formData.get("role");
    if (
      role !== RegisterRequestRole.OWNER &&
      role !== RegisterRequestRole.EMPLOYEE &&
      role !== RegisterRequestRole.VIEWER
    ) {
      setNotice(copy.messages.invalidRole);
      return;
    }

    try {
      await registerMutation.mutateAsync({
        data: {
          fullName: String(formData.get("fullName") ?? ""),
          email: String(formData.get("email") ?? ""),
          businessName: String(formData.get("businessName") ?? ""),
          role,
          password: String(formData.get("password") ?? ""),
          termsAccepted: formData.get("termsAccepted") === "on",
        },
      });
      setScreen("login");
      setNotice(copy.messages.accountCreated);
      setNoticeTone("success");
    } catch (error) {
      setNotice(
        error instanceof ApiError
          ? error.status < 500
            ? error.message
            : `${copy.messages.createFailed} (HTTP ${error.status})`
          : copy.messages.createFailed,
      );
      setNoticeTone("error");
    }
  }

  async function handleLogout() {
    setNotice("");
    try {
      await logoutMutation.mutateAsync();
      setDisplayName("");
      setScreen("login");
      await queryClient.cancelQueries({ queryKey: getMeQueryKey() });
      queryClient.removeQueries({ queryKey: getMeQueryKey(), exact: true });
      queryClient.removeQueries({ queryKey: ["/api/profile"], exact: true });
    } catch {
      setNotice("Unable to sign out. Please try again.");
      setNoticeTone("error");
    }
  }

  if (meQuery.isPending)
    return (
      <div className="auth-page">
        <main className="auth-main">
          <output aria-live="polite">{copy.checkingSession}</output>
        </main>
      </div>
    );

  if (meQuery.data)
    return (
      <Dashboard
        onLogout={handleLogout}
        user={meQuery.data}
        displayName={displayName}
        isLoggingOut={logoutMutation.isPending}
        notice={notice}
        onProfileUpdated={setDisplayName}
        language={language}
        onLanguageChange={handleLanguageChange}
      />
    );

  const sessionNotice =
    meQuery.error &&
    !(meQuery.error instanceof ApiError && meQuery.error.status === 401)
      ? copy.messages.verifySession
      : "";
  const visibleNotice = notice || sessionNotice;
  const visibleNoticeTone = notice ? noticeTone : "error";

  return (
    <div className="auth-page">
      <SiteHeader
        copy={copy}
        language={language}
        onLanguageChange={handleLanguageChange}
        onSupport={() => window.alert(copy.contactAlert)}
      />
      <main className="auth-main">
        {screen === "login" && (
          <LoginCard
            onNavigate={(nextScreen) => {
              setScreen(nextScreen);
              setNotice("");
              setNoticeTone("error");
            }}
            onLogin={handleLogin}
            isPending={loginMutation.isPending}
            copy={copy}
          />
        )}
        {screen === "register" && (
          <RegisterCard
            onNavigate={(nextScreen) => {
              setScreen(nextScreen);
              setNotice("");
              setNoticeTone("error");
            }}
            onRegister={handleRegister}
            isPending={registerMutation.isPending}
            copy={copy}
          />
        )}
        {screen === "recovery" && (
          <RecoveryCard
            onNavigate={(nextScreen) => {
              setScreen(nextScreen);
              setNotice("");
              setNoticeTone("error");
            }}
            copy={copy}
          />
        )}
        {visibleNotice && (
          <output
            className="notice-message"
            data-tone={visibleNoticeTone}
            role={visibleNoticeTone === "error" ? "alert" : "status"}
          >
            {visibleNotice}
          </output>
        )}
      </main>
      <SiteFooter copy={copy} />
    </div>
  );
}
