# Chat App

A simple Android chat application built using Firebase services.

## Features

- User authentication (login/register) with Firebase Authentication
- Real-time chat messaging with Firebase Realtime Database
- Push notifications for new messages with Firebase Cloud Messaging
- Create chats with other users by email

## Setup

1. Clone this repository
2. Create a new project in the [Firebase Console](https://console.firebase.google.com/)
3. Add an Android app to your Firebase project:
   - Package name: `com.example.myfirstapp`
   - Download the `google-services.json` file and place it in the `app/` directory
4. Enable the following Firebase services:
   - Authentication (Email/Password)
   - Realtime Database
   - Cloud Messaging
5. Set up Firebase Authentication:
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication
6. Set up Firebase Realtime Database:
   - Go to Realtime Database > Create database
   - Start in test mode (allow read/write access)
7. Open the project in Android Studio
8. Build and run the app

## Usage

1. Register a new account using your email and password
2. Create a new chat by clicking the + button
3. Enter the email of another registered user
4. Start sending messages!

## Dependencies

- Firebase Authentication
- Firebase Realtime Database
- Firebase Cloud Messaging
- RecyclerView
- CardView
- Circle ImageView
- Material Components

## License

This project is licensed under the MIT License - see the LICENSE file for details. 