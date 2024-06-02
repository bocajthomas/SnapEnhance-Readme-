<div align="center">
  <img src="https://raw.githubusercontent.com/bocajthomas/SE-Extended/dev/images/logo/SE%20Extended%20Logo.png" height="250" />

# SE Extended 
SE Extended is a fork from the [SnapEnhance app](https://github.com/rhunk/SnapEnhance) that uses the Xposed Framework to enhance your Snapchat experience.<br/><br/>
Please note that this project is currently in development, so bugs and crashes may occur. If you encounter any issues, we encourage you to report them. To do this simply visit our [issues](https://github.com/bocajthomas/SE-Extended/issues/new?assignees=&labels=bug&projects=&template=bug_report.yml&title=...) page and create an issue, make sure to follow the guidelines.
</div>

## Screenshots
<div align="center">
<div>
<img src="images/screenshots/Screenshot-1.jpg" width="30%" />
<img src="images/screenshots/Screenshot-2.jpg" width="30%" />
<img src="images/screenshots/Screenshot-3.jpg" width="30%" />
<img src="images/screenshots/Screenshot-4.jpg" width="30%" />
<img src="images/screenshots/Screenshot-6.jpg" width="30%" />
<img src="images/screenshots/Screenshot-7.jpg" width="30%" />
</div>
</div>

## Main Features
<details closed>
  <summary>Media Downloader</summary>
   
  - `Auto Download`
  - `Prevent Self Auto Download`
  - `Merge Overlays`
  - `Force Image Format`
  - `Force Voice Note Format`
  - `Download Profile Pictures`
  - `Opera Download Button`
  - `Chat Download Context Menu`
  - `Logging`
  - `Custom Path Format` 
</details>

<details closed>
  <summary>User Interface</summary>
  
  - `Friend Feed Menu Buttons` 
  - `AMOLED Dark Mode`
  - `Friend Feed Message Preview` 
  - `Snap Preview`
  - `Bootstrap Override` (Default Home Tab & Persistent App Appearance)
  - `Enhance Friend Map Nametags`
  - `Prevent Message List Auto Scroll`
  - `Show Streak Expiration Info`
  - `Hide Friend Feed Entry`
  - `Hide Streak Restore`
  - `Hide Quick Add In Friend Feed`
  - `Hide Story Section` 
  - `Hide UI Components` (Voice Record button, Call Buttons, ...)
  - `Opera Media Quick Info`
  - `Old Bitmoji Selfie` 
  - `Disable Spotlight` 
  - `Hide Settings Gear`
  - `Vertical Story Viewer` 
  - `Message Indicators` 
  - `Stealth Mode Indicator` 
  - `Edit Text Override`
</details>  

<details closed>
  <summary>Messaging</summary>
  
  - `Bypass Screenshot Detection` 
  - `Anonymous Story Viewing`
  - `Prevent Story Rewatch Indicator`
  - `Hide Peek-a-Peek`
  - `Hide Bitmoji Presence` 
  - `Hide Typing Notifications` 
  - `Unlimited Snap View Time`
  - `Auto Mark As Read` 
  - `Loop Media PlayBack`
  - `Disable Replay In FF`
  - `Half Swipe Notifier`
  - `Message Preview`
  - `Call Start Confirmation`
  - `Auto Save Messages` 
  - `Prevent Message Sending`
  - `Friend Mutation Notifier`
  - `Better Notifications` 
  - `Notifications Blacklist`
  - `Message Logger`
  - `Gallery Media Send Override`
  - `Strip Media Metadata`
  - `Bypass Message Retention Policy`
  - `Bypass Message Action Restrictions`
  - `Remove Groups Locked Status` 
 </details>

<details closed>
  <summary>Global</summary>
 
  - `Better Location`
  - `Suspend Location Updates`
  - `Snapchat Plus` 
  - `Disable Confirmation Dialogs`
  - `Disable Metrics`
  - `Disable Story Sections`
  - `Block Ads`
  - `Disable Permission Request`
  - `Disable Memories Snap Feed`
  - `Spotlight Comments Username` 
  - `Bypass Video Length Restriction`
  - `Default Video Playback Rate`
  - `Video Playback Rate Slider`
  - `Disable Google Play Services Dialogs`
  - `Force Upload Source Quality`
  - `Default Volume Controls`
  - `Hide Active Music`
  - `Disable Snap Splitting`
</details>

<details closed>
  <summary>Camera</summary>
  
  - `Disable Camera`
  - `Immersive Preview`
  - `Black Photos` 
  - `Custom Frame Rate` (Front & Back)
  - `HEVC Recording`
  - `Force Camera Source Encoding`
  - `Override Resolution` (Front & Back)
</details> 

<details closed>
  <summary>Experimental</summary>
  
  - `Session Events`
  - `Device Spoof`
  - `Convert Message Locally`
  - `New Chat Action Menu`
  - `Media File Picker`
  - `Story Logger`
  - `Call Recorder`
  - `Account Switcher`
  - `Edit Messages` 
  - `App Passcode`
  - `Infinite Story Boost`
  - `My Eyes Only Passcode Bypass`
  - `No Friend Score Delay`
  - `End-to-End Encryption`
  - `Enable Hidden Snapchat Plus Features`
  - `Custom Streaks Expiration Format`
  - `Add Friend Source Spoof`
  - `Prevent Forced Logout`
</details>

## Download 
To Download the latest stable release, please visit the [Releases](https://github.com/bocajthomas/SE-Extended/releases) page.<br/>
You can also download the latest debug build from the [Actions](https://github.com/bocajthomas/SE-Extended/actions/workflows/debug.yml) section.<br/>
We no longer offer official `LSPatch` binaries for obvious reasons. However, you're welcome to patch them yourself, as they should theoretically work without any issues.

## FAQ
You can view the FAQ [here](https://github.com/bocajthomas/SE-Extended/wiki/FAQ)
## Contact 
Join our [Telegram Channel](https://t.me/SE_Extended) for Discussions, announcements, and releases!


## Donate
SE Extended will always be free and open source for everyone, If you like it consider [Donating](https://www.paypal.me/Jhuddz00)

## Privacy
We do not collect any user information. However, please be aware that third-party libraries may collect data as described in their respective privacy policies.
<details>
  <summary>Permissions</summary>
  
  - [android.permission.INTERNET](https://developer.android.com/reference/android/Manifest.permission#INTERNET)
  - [android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS](https://developer.android.com/reference/android/Manifest.permission.html#REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
  - [android.permission.POST_NOTIFICATIONS](https://developer.android.com/reference/android/Manifest.permission.html#POST_NOTIFICATIONS)
  - [android.permission.SYSTEM_ALERT_WINDOW](https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW)
  - [android.permission.USE_BIOMETRIC](https://developer.android.com/reference/android/Manifest.permission#USE_BIOMETRIC)
</details>

## Credits 
SE Extended Uses Some Third-party libraries, These Include 
  
  - [libxposed](https://github.com/libxposed/api)
  - [ffmpeg-kit-full-gpl](https://github.com/arthenica/ffmpeg-kit)
  - [osmdroid](https://github.com/osmdroid/osmdroid)
  - [coil](https://github.com/coil-kt/coil)
  - [Dobby](https://github.com/jmpews/Dobby)
  - [rhino](https://github.com/mozilla/rhino)
  - [rhino-android](https://github.com/F43nd1r/rhino-android)
  - [libsu](https://github.com/topjohnwu/libsu)
  - [colorpicker-compose](https://github.com/skydoves/colorpicker-compose)
