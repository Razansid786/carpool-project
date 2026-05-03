# Carpool App - Complete Workflow & Technical Documentation

This document provides a comprehensive overview of the Carpool application's architecture, workflow, and key features, designed for project understanding and Viva preparation.

---

## 1. Project Overview
The **Carpool App** is a peer-to-peer transportation platform designed for a community (e.g., Students and Teachers). It allows users to share rides, reducing travel costs and environmental impact.

### Core Tech Stack
*   **Language:** Java (Android SDK)
*   **Backend:** Firebase (Authentication, Firestore Database)
*   **Architecture:** Fragment-based UI with a single main Activity.
*   **External APIs:** Google Maps & Location Services (for route selection).

---

## 2. App Workflow (Step-by-Step)

### Phase 1: Launch & Entry
1.  **Splash Screen (`SplashActivity`):** 
    *   Initializes the app.
    *   Checks `SharedPreferences` to see if the user has completed the onboarding.
    *   Checks `FirebaseAuth` to see if a session exists.
    *   **Logic:** If logged in → `MainActivity`; Else if first time → `OnboardingActivity`; Else → `LoginActivity`.
2.  **Onboarding (`OnboardingActivity`):**
    *   Features a `ViewPager2` with an `OnboardingAdapter` to introduce app benefits.
3.  **Authentication (`LoginActivity` / `SignupActivity`):**
    *   **Signup:** Users register with Name, Email, Phone, City, Country, and Role (Student/Teacher).
    *   **Verification:** Integrates an OTP verification flow (`VerifyOtpActivity`).
    *   **Post-Signup:** A welcome notification is sent, and an email intent is triggered.

### Phase 2: Core Functionality (Main Dashboard)
The `MainActivity` uses a `BottomNavigationView` to host four primary fragments:

1.  **Discover (`DiscoverFragment`):**
    *   The "Home" feed.
    *   Fetches available rides from Firestore.
    *   Users can filter or search for rides.
2.  **Post Ride (`PostRideFragment`):**
    *   Allows users (Drivers) to create a ride.
    *   Inputs: Pickup point, Destination, Date, Time, and available Seats.
3.  **My Rides (`RidesFragment` / `MyRidesFragment`):**
    *   Shows rides the user has joined or created.
    *   Uses a `TabLayout` to switch between "Joined" and "Posted" rides.
4.  **Profile (`ProfileFragment`):**
    *   Displays user information.
    *   Includes "Logout" and "Edit Profile" options.

### Phase 3: Interaction & Real-time Features
*   **Ride Details:** Clicking a ride opens a `RideDetailsBottomSheet` or `BookingOfferBottomSheet` for quick interaction.
*   **Chatting (`ChatActivity`):** Real-time messaging between passengers and drivers using Firestore.
*   **Notifications:** 
    *   A `SnapshotListener` in `MainActivity` listens for new documents in the `notifications` collection.
    *   `NotificationHelper` triggers system-level alerts even if the app is in the foreground.

---

## 3. Database Schema (Firestore)
*   **`users`**: Stores user profiles (UID, name, role, phone).
*   **`rides`**: Stores ride offers (source, destination, driverId, timestamp, seats).
*   **`notifications`**: Stores alerts for users (recipientId, title, message, read status).
*   **`chats`**: Stores message history between users.

---

## 4. Key Technical Concepts (Viva Questions)
1.  **How do you handle data persistence?** 
    *   User sessions and onboarding state are saved in `SharedPreferences`.
    *   All dynamic data (Rides, Users, Messages) is stored in **Firebase Firestore**.
2.  **How does real-time notification work?** 
    *   We use a `SnapshotListener` on the Firestore collection. When a new document matches the user's ID, the app detects it instantly and triggers a `NotificationManager` alert.
3.  **What is the benefit of using Fragments?** 
    *   Fragments allow for a modular UI. We can switch between Discover, Post, and Profile without destroying the `MainActivity`, providing a smoother user experience.
4.  **How is the car animation implemented?** 
    *   Using an XML animation (`res/anim/car_animation.xml`) applied to an `ImageView` in the Signup/Login screens to enhance UX.

---
*Created for: Carpool Project Viva Documentation*
