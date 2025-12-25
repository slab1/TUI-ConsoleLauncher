# T-UI Linux CLI Launcher

<a href="https://play.google.com/store/apps/details?id=ohi.andre.consolelauncher"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="60"></a>    <a href="https://f-droid.org/packages/ohi.andre.consolelauncher">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="60">
</a>


## Useful links

**@tui_launcher**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[Twitter.com](https://twitter.com/tui_launcher)**<br>
**Official community**&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[Reddit](https://www.reddit.com/r/tui_launcher/)**<br>
**Official Group**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[Telegram](https://t.me/tuilauncher)**<br>
**Wiki**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[GitHub.com](https://github.com/Andre1299/TUI-ConsoleLauncher/wiki)**<br>
**FAQ**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[GitHub.com](https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/FAQ)**


## Some mentions

- **[XDA](https://www.xda-developers.com/linux-cli-launcher-transforms-your-home-screen-into-a-terminal/)**
- **[Android Authority](http://www.androidauthority.com/linux-cli-launcher-turns-homepage-linux-command-line-interface-767431/)**
- **[Gadget Hacks](https://android.gadgethacks.com/how-to/linux-style-launcher-turns-your-home-screen-into-command-prompt-0177326/)**

## New Features

This fork of T-UI ConsoleLauncher includes significant enhancements that transform the launcher into a comprehensive smart IDE development environment. The following major features have been implemented to extend the functionality far beyond the original command-line launcher.

### Monaco Editor Integration

The integration of Monaco Editor, the same editor that powers VS Code, brings full-featured code editing capabilities directly to the Android launcher. This integration provides developers with a familiar and powerful editing environment on mobile devices, enabling code editing, syntax highlighting for multiple languages, and seamless interaction with other development tools within the T-UI ecosystem. The Monaco Editor runs within a WebView component and communicates with the native Android layer through a carefully designed JavaScript bridge that enables bidirectional communication for file operations, settings management, and command execution. This architecture allows the editor to leverage web technologies for rendering while maintaining access to native Android capabilities for file system access and system integration.

The editor integration includes a dedicated controller class that manages the lifecycle of the WebView, handles communication between the JavaScript runtime and native Android code, and provides a settings management system that persists editor preferences across sessions. Users can configure themes, font sizes, keyboard mappings, and numerous other editor options through an intuitive settings interface accessible directly from the editor panel. The integration also supports common editor shortcuts and gestures, making the mobile coding experience more efficient and productive.

### Unified Settings Architecture

A comprehensive unified settings architecture has been implemented to provide consistent, modular, and secure configuration management across all application components. This architecture follows a registry pattern where each application module manages its own settings through a standardized interface while a central manager coordinates all configuration operations. The design promotes separation of concerns, where individual modules are responsible for their own settings logic while the central manager provides cross-cutting functionality such as persistence, encryption for sensitive data, and change notifications.

The settings system is built around the ISettingsModule interface, which defines the contract that all settings modules must implement. This interface includes methods for loading settings, saving settings, registering change listeners, and accessing module-specific configuration values. The BaseSettingsModule abstract class provides common implementations for these methods, reducing boilerplate code and ensuring consistency across all module implementations. Individual modules such as GitSettings, FileManagerSettings, TerminalSettings, BuildSettings, and UiThemeSettings extend this base class to provide module-specific configuration management.

The GlobalSettingsManager serves as the central registry for all settings modules, providing a single point of access for configuration operations while maintaining module isolation. This manager handles the initialization of all settings modules, coordinates persistence operations, and provides mechanisms for modules to subscribe to changes in other modules' configurations. The architecture also supports runtime configuration changes, allowing users to modify settings and see the effects immediately without restarting the application.

### Security Enhancements

Security has been a primary concern in the development of these enhancements, with particular attention paid to protecting sensitive user data such as API tokens, credentials, and personal information. The implementation follows OWASP best practices for Android application security, incorporating multiple layers of protection to safeguard user data both at rest and in transit. The security architecture leverages Android's built-in security features while adding additional protections for particularly sensitive information.

EncryptedSharedPreferences from the AndroidX Security library are used to store sensitive configuration values such as API tokens and Git credentials. This encryption layer ensures that even if the device is compromised, sensitive credentials remain protected by military-grade encryption backed by the Android Keystore system. The encryption keys themselves are protected by hardware-backed security on supported devices, providing an additional layer of protection against extraction attacks. Non-sensitive configuration values continue to use standard SharedPreferences for performance while sensitive values receive full encryption protection.

The implementation also includes secure coding practices throughout, with proper input validation, secure storage of cryptographic keys, and careful handling of credentials throughout the application lifecycle. All network communications use HTTPS with certificate validation to prevent man-in-the-middle attacks, and sensitive operations require appropriate user authentication where supported by the device. The security implementation is regularly reviewed and updated to address emerging threats and vulnerabilities.

## Contributing

**Pull requests** are welcome. But **before** you decide to make a major change you should contact me (**[e-mail](mailto:andreuzzi.francesco@gmail.com)**) in order to check if I'm going to include your change in t-ui, so you don't waste your time.

## How to format a bug report

1. Set "**Bug report**" as subject
2. Describe the issue, when it happens, how to reproduce it
3. **English**! (or Italian, at least)
4. Include any **screenshot** that you think could help (*outputs*, *UI*, etc..)
5. Include any **file** that you think could help (*behavior.xml*, *ui.xml*, etc..)
6. Send it to **andreuzzi.francesco@gmail.com**

## Open source libraries

* [**CompareString2**](https://github.com/fAndreuzzi/CompareString2)
* [**OkHttp**](https://github.com/square/okhttp)
* [**HTML cleaner**](http://htmlcleaner.sourceforge.net/)
* [**JsonPath**](https://github.com/json-path/JsonPath)
* [**jsoup**](https://github.com/jhy/jsoup/)
* [**Monaco Editor**](https://microsoft.github.io/monaco-editor/) - The code editor that powers VS Code, integrated for in-app code editing
* [**AndroidX Security Crypto**](https://developer.android.com/jetpack/androidx/releases/security-crypto) - For encrypted shared preferences
