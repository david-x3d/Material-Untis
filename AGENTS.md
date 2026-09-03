# Material-Untis — architecture

Phone (`:app`) is the source of truth. Wear (`:wear`) never talks to WebUntis.

- `:core` owns JSON-RPC (`jsonrpc.do` only), domain models, Room cache, encrypted credentials, WatchPayload codec.
- Dates are Untis ints (`YYYYMMDD`). Times are Untis ints (`720` = 07:20). Device TZ is only used for “now”.
- Session cookie is `JSESSIONID` from `authenticate.sessionId`. Do not use `;jsessionid=` path rewriting.
- Auth retry: one re-login on RPC auth errors / HTTP 401, then fail to UI. No poll loops.
- Data Layer path `/dayline/today` carries gzip JSON: date, periods (subject, room, teacher, start, end, status, info). Never credentials.
- School search: `POST https://mobile.webuntis.com/ms/schoolquery2` method `searchSchool` (community / official mobile clients). Manual host + school is always available.
- Secret login: TOTP (SHA1, 6 digits, 30s) from QR `key` (hex or base32), then the same `authenticate` method with the OTP as password (SchoolUtils/WebUntis SecretAuth).
- This app is not affiliated with Untis.
