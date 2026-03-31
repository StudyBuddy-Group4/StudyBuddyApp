StudyBuddy - Android Application (Frontend)
=============================================
Group 4

Members:
  Jayson Leander (2155036)
  Wenjie Li (2090937)
  Xiangjie Dong (2072645)
  Yanhang Luo (2123061)
  Yingyao Feng (2094770)
  Yue Peng (2104431)


Project Overview
----------------
StudyBuddy is a mobile application that helps students maintain focus
during study sessions through virtual study meetings, structured focus
timers, task management, and a moderation engine. The Android client
communicates with a Spring Boot REST backend and uses Agora RTC SDK
for real-time video/audio communication.


Build & Run Instructions
------------------------
1. Open this project in Android Studio (Ladybug or later recommended).
2. Ensure Java 21 is configured as the project JDK.
3. Sync Gradle when prompted.
4. The app connects to a remote backend by default (see ApiConfig.java).
   For local development, switch BASE_URL to http://10.0.2.2:8080/
   and run the backend server separately.
5. Build and run on an emulator (API 28+) or a physical Android device.

Minimum SDK: 28 (Android 9.0)
Target SDK: 36
Language: Java


Third-Party Java Sources
------------------------
This project does NOT contain any third-party Java source files used
unchanged. All external libraries are consumed exclusively through
Gradle dependencies (declared in app/build.gradle.kts) and are not
included as source files in this repository.

External libraries used (via Gradle dependencies, not as source):
  - io.agora.rtc:full-sdk:4.5.0           (Agora Video SDK)
  - com.squareup.retrofit2:retrofit:2.11.0 (HTTP client)
  - com.squareup.retrofit2:converter-scalars:2.11.0
  - com.squareup.retrofit2:converter-gson:2.11.0
  - com.google.code.gson:gson:2.11.0       (JSON serialization)
  - androidx.appcompat, material, constraintlayout (Android UI)


Test Environment
----------------
Unit tests are located in: app/src/test/java/
Instrumented tests are located in: app/src/androidTest/java/

Testing frameworks used:
  - JUnit 4.13.2
  - Robolectric 4.14.1
  - Mockito 5.12.0 / mockito-inline 5.2.0
  - AndroidX Test Core 1.5.0
  - Espresso 3.5.1

Code coverage is measured using JaCoCo (version 0.8.14) and results
are uploaded to SonarQube for analysis.

To run tests and generate coverage:
  ./gradlew clean testDebugUnitTest jacocoTestReport

To run SonarQube analysis:
  ./gradlew sonar

SonarQube project: 2IS70-Android-App (NOT SURE)


Project Structure
-----------------
app/src/main/java/com/example/studybuddyapp/
  ├── api/               Retrofit API interfaces and client
  │   ├── dto/           Data transfer objects
  │   ├── AuthApi.java
  │   ├── UserApi.java
  │   ├── MatchingApi.java
  │   ├── SessionApi.java
  │   ├── TaskApi.java
  │   ├── ModerationApi.java
  │   └── ApiClient.java
  ├── HomeFragment.java
  ├── MeetingRoomActivity.java
  ├── MeetingRoomVideoCallManager.java
  ├── MeetingRoomSessionCoordinator.java
  ├── StatisticsFragment.java
  ├── TaskListFragment.java
  ├── AdminProfileFragment.java
  ├── AdminMeetingRoomActivity.java
  ├── SessionManager.java
  └── ... (other activities and fragments)

app/src/main/res/
  ├── layout/            Portrait layouts
  ├── layout-land/       Landscape layouts
  ├── drawable/          Vector icons and shape drawables
  ├── values/            Colors, strings, styles
  └── menu/              Bottom navigation menus
