# Buffer Safari Extension

A browser extension that allows users to easily share content to Buffer from across the web. This extension supports multiple browsers including Chrome, Firefox, and Safari.

## Overview

The Buffer Safari Extension enhances your web browsing experience by providing seamless integration with Buffer's publishing platform. It allows you to quickly share web content, images, and selected text to your Buffer queue for scheduled posting to social media platforms.

## Features

- **One-Click Sharing**: Add any webpage to your Buffer queue with a single click
- **Context Menu Integration**: Right-click on pages, images, or selected text to share to Buffer
- **Social Media Integration**: Special integrations for popular platforms:
  - Twitter/X.com
  - Reddit
  - Pinterest
  - Hacker News
  - TweetDeck
- **Keyboard Shortcuts**: Configurable hotkeys for quick Buffer actions
- **Hover Buttons**: Easily share images by hovering over them
- **Light/Dark Mode Support**: Adapts to your browser's theme
- **Customizable Options**: Configure the extension behavior through the options page

## Browser Compatibility

The extension is designed to work on multiple browsers:

- **Chrome**: Full support
- **Firefox**: Full support with Firefox-specific adaptations
- **Safari**: Full support with Safari-specific adaptations

## Development

### Prerequisites

- macOS (for Safari extension development)
- Bash shell
- Basic knowledge of web technologies (HTML, CSS, JavaScript)

### Project Structure

```
Resources/
├── _locales/            # Localization files
├── dist/                # Build output directory (created during build)
├── manifest-chrome.json # Chrome-specific manifest
├── manifest-firefox.json # Firefox-specific manifest
├── manifest-safari.json # Safari-specific manifest
├── scripts/             # Build and utility scripts
│   └── build.sh         # Main build script
├── version.txt          # Current version of the extension
└── [JavaScript files]   # Extension functionality
```

### Building the Extension

The project includes a comprehensive build script (`scripts/build.sh`) that handles the build process for all supported browsers.

#### Basic Commands

```bash
# Build the extension for all browsers (Chrome, Firefox, Safari)
./scripts/build.sh build

# Build with a specific version number
./scripts/build.sh build 6.1.0

# Clean the build directory
./scripts/build.sh clean

# Build and watch for changes (development mode)
./scripts/build.sh dev chrome  # For Chrome
./scripts/build.sh dev firefox # For Firefox
./scripts/build.sh dev safari  # For Safari
```

#### Version Management

The build script automatically manages versioning:

- Reads the current version from `version.txt`
- Increments the patch version by default if no version is specified
- Prevents accidental version downgrades
- Updates all manifest files with the correct version
- Updates `version.txt` with the new version after building

### Development Workflow

1. Make changes to the source files in the `Resources` directory
2. Run the build script in development mode for your target browser:
   ```bash
   ./scripts/build.sh dev chrome
   ```
3. The script will watch for changes and automatically rebuild when files are modified
4. Test your changes in the browser

## Technologies and Approaches

### Core Technologies

- **JavaScript**: Core extension functionality
- **jQuery**: DOM manipulation and event handling
- **Chrome Extension API**: Browser integration
- **Web Extension API**: Cross-browser compatibility

### Key Approaches

- **Browser Detection**: The extension detects the current browser and adapts its behavior accordingly
- **Content Scripts**: Used to inject Buffer functionality into web pages
- **Background Scripts**: Manage extension state and handle browser events
- **Cross-Browser Compatibility**: Manifest files and code adaptations for different browsers
- **Site-Specific Integrations**: Custom code for popular social media platforms
- **Storage API**: Uses Chrome's storage API for persistent settings

## Extension Components

### Background Script

The `background.js` file serves as the main controller for the extension, handling:
- Initial setup
- Context menu creation
- Browser action configuration
- Message passing between components
- Dark/light mode detection

### Content Scripts

Various content scripts provide site-specific functionality:
- `buffer-twitter.js`: Twitter/X.com integration
- `buffer-reddit.js`: Reddit integration
- `buffer-pinterest.js`: Pinterest integration
- `buffer-hn.js`: Hacker News integration
- `buffer-hover-button.js`: Image sharing functionality

### User Interface

- `popup.js`: Controls the main popup interface
- `options.js`: Manages user preferences
- `toggle-icon.js`: Handles icon changes based on theme

### Utilities

- `get-selection.js`: Extracts selected text from web pages
- `get-title.js`: Retrieves page titles
- `keymaster.js`: Keyboard shortcut handling

## Additional Resources

- [Notion docs](https://www.notion.so/buffer/Extension-2147eb90f0084d628395357612d90c47?pvs=4)
