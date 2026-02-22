## 🖥 Frontend (React + Vite)

A minimal React frontend for submitting payments locally.

---
### Setup
```bash
cd frontend
npm install
npm run dev
```
Visit `http://localhost:5173`. Requires the Spring Boot backend running on `http://localhost:8080`.

> API calls are proxied via Vite — no CORS configuration needed on the backend.

---

## 📸 Screenshots

### Payment Form
The main payment form where users enter their details.
![Payment Form_Login](docs/screenshots/login.png)
---

### Successful Payment Response
API response panel showing a successful `200 Created` response after submission.
![Payment Form_success](docs/screenshots/success.png)
---

### Validation State
Form validation errors shown when required fields are missing or invalid.
![Payment Form_validation error01](docs/screenshots/validation.png)
![Payment Form_validation error02](docs/screenshots/validation2.png)
---

### Internal service error
Triggered when a dependent service (e.g. database) is unavailable, the API returns a `500 Internal Server Error`.
![Payment Form_internal_service_error](docs/screenshots/service_error.png)

