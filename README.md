# BizTrack

BizTrack tracks small-business transactions, reports, and credit given to customers and other business partners.

## Credits API requests for Postman

Set `baseUrl` to the backend URL (for example `http://localhost:8080`). Log in with the existing `/api/auth/login` request first; Postman should retain the `JSESSIONID` session cookie. The API derives the business from that session, so do not send a `businessId`.

### Credit takers

**POST** `{{baseUrl}}/api/credit-takers`

```json
{
  "name": "Ravi Traders",
  "type": "RETAILER",
  "phone": "+919876543210",
  "email": "accounts@example.com"
}
```

**GET** `{{baseUrl}}/api/credit-takers?q=Ravi`

**PUT** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}`

```json
{
  "name": "Ravi Traders",
  "type": "RETAILER",
  "phone": "9876543210",
  "email": "accounts@example.com"
}
```

**DELETE** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}`  
Deletion succeeds only when the taker has no credit entries or retained receipts.

**GET** `{{baseUrl}}/api/credit-takers/overview` returns the business-wide total and taker count.

### Credit entries

**POST** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/entries`

```json
{
  "amount": 5000.00,
  "entryDate": "2026-10-01",
  "entryTime": "10:30",
  "note": "Stock supplied"
}
```

For the arithmetic example, repeat the POST with `2500.00` at `2026-10-01T16:45`, `1200.50` at `2026-10-03T11:00`, and `3000.00` at `2026-11-02T09:15`.

**GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/entries?from=2026-10-01&to=2026-11-30`

**PUT** `{{baseUrl}}/api/credit-entries/{{entryId}}`

```json
{
  "amount": 5100.00,
  "entryDate": "2026-10-01",
  "entryTime": "10:30",
  "note": "Corrected amount"
}
```

**DELETE** `{{baseUrl}}/api/credit-entries/{{entryId}}`

### Daily, monthly, and receipt requests

- **GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/day?date=2026-10-01`
- **GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/daily?from=2026-10-01&to=2026-10-31`
- **GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/monthly?year=2026`
- **GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/receipt-preview?from=2026-10-01&to=2026-11-30` previews without saving.
- **GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/receipt?from=2026-10-01&to=2026-11-30` generates and saves a traceable receipt; retain its returned `id`.
- **GET** `{{baseUrl}}/api/credit-takers/{{creditTakerId}}/receipts`
- **GET** `{{baseUrl}}/api/credit-receipts/{{receiptId}}/pdf` downloads the PDF.
- **POST** `{{baseUrl}}/api/credit-receipts/{{receiptId}}/email`

```json
{
  "toEmail": "accounts@example.com"
}
```

Generated receipts are immutable, so there are no receipt `PUT` or `DELETE` endpoints. If SMTP is configured, supply `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD` through environment variables. Do not put SMTP credentials in source control or this README. Without a configured mail host, the email endpoint returns `503 Email is not configured`; PDF downloads remain available.
