# 🚀 Smart Assistant & Disaster Alert App

An **AI-powered, secure, multi-utility Android app** that keeps you safe, informed, and productive.  

---

## 🎥 Demo
- 📱 [APK Download](./app-debug.apk)  
- 🎬 [Demo Video](https://drive.google.com/drive/folders/1E175BOW-2tFS7iHuJ_Itq1i-zOrHx6iE)  

---

## ✨ Features

### 🔐 Authentication & Security
- Multi-login: **Google, Facebook, Email**
- Full **end-to-end data encryption**
- Secure storage of **photos, videos, PDFs, usernames & passwords**

### 📰 Home Fragment
- Fetches **recent news within 2 km** using Google API  
- Disaster alerts with **geo-fencing** → warns if you’re entering a danger zone  

### 🆘 Help Fragment
- Save & access your **favourite links**  
- Track application status with **real-time notifications**  
- **One-click helpline calls**  

### 🤖 Jarvis Chatbot
- Supports **any language** dynamically  
- **Form-filling assistant** for government/other forms  
- Acts as **voice assistant** (commands, responses, actions)  

### 👤 Profile & Subscription
- Edit & view personal data  
- Subscription model: **₹69 for 84 days**  
- Additional support for **ad revenue**  

---

## ⚙️ Tech Stack
- **Android Studio (Kotlin)**
- **Firebase** (Auth, Firestore, Storage, Notifications)
- **Google Maps + GeoFencing API**
- **Google News API**
- **AES Encryption** for secure storage
- **Gemini / LLM Integration** for chatbot  

---

## 🛠️ Setup & Run

1. **Clone repo**
   ```bash
   git clone https://github.com/shlok6097/Srujana_hackathon_sevapath.git
   cd repo
   2. **Open in Android Studio**
   - File > Open > select the `app/` folder  

3. **Add config files**
   - Place your `google-services.json` inside the `app/` folder  
   - Add your API keys in `local.properties`:  
     ```properties
     MAPS_API_KEY=your_google_maps_key
     NEWS_API_KEY=your_google_news_key
     GEMINI_API_KEY=your_gemini_key
     ```

4. **Build & Run**
   - Connect an Android device (USB / emulator)  
   - Click **▶️ Run** in Android Studio  

5. **Login & Explore**
   - Use Google/Facebook/Email login  
   - Try features in Home, Help, Chatbot, and Profile fragments  
---

## 🧪 Test Accounts

To save setup time, you can log in using these demo accounts:

### Google / Facebook
- Use your own Google or Facebook account to sign in (OAuth supported).

### Email Login (Firebase Auth)
 
- **Demo**
  - Email: admin@test.com  
  - Password: 123456  

*(If Firebase Auth is reset, please contact the team.)*

---

## 📸 Screenshots

## 📸 Screenshots

### 🔐 Login & Security
![Login](https://drive.google.com/file/d/1cTCBJ494_-GcspmbsfpdQGs4U-xdoOD2/view?usp=drivesdk)

### 📰 Home Fragment
![News](https://drive.google.com/file/d/18RWv2sidJks-fUZ_06nyrSApgx72reRW/view?usp=drivesdk)

### 🤖 Chatbot (Jarvis)
![Chatbot](https://drive.google.com/file/d/1ie_a4btjxNwqoP1_eiYIme0BNyTau7Tb/view?usp=drivesdk)

---

## 📊 Extra Notes

- 🌍 App supports **multiple languages** dynamically.  
- 🔔 Push notifications used for application status updates.  
- 💾 Encrypted storage tested on Android 10–14.  
- 📱 Minimum SDK: 24 (Android 7.0).  

---

pip install pypandoc
pypandoc.convert_text(open("README.md").read(), 'pdf', format='md', outputfile="README.pdf", extra_args=['--standalone'])


