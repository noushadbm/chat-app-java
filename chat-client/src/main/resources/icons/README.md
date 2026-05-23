# App Icons

This folder contains the application icons used by the JavaFX Chat Client.

## Required Files

Place the following PNG files in this directory for best results across platforms (Windows, macOS, Linux):

| File Name              | Size   | Recommended Use                  |
|------------------------|--------|----------------------------------|
| `chat-icon-16.png`     | 16×16  | Taskbar / Dock (small)           |
| `chat-icon-32.png`     | 32×32  | Taskbar / System tray            |
| `chat-icon-64.png`     | 64×64  | Windows taskbar / Linux          |
| `chat-icon-128.png`    | 128×128| macOS & high-DPI                 |
| `chat-icon-256.png`    | 256×256| macOS Retina & modern desktops   |

## Design Recommendations

- Use a **clean, modern chat icon** (speech bubble, two overlapping bubbles, or a stylized "message" symbol).
- Match the current **dark modern theme** of the app:
  - Primary accent: `#6366f1` (Indigo)
  - Background: Dark (#0f0f12 or transparent)
- Use rounded corners and a subtle gradient or shadow for a premium 2026 look.
- Export as **PNG with transparency**.
- Keep the icon simple and recognizable at small sizes.

## How to Generate Icons

You can use tools like:
- **Figma** + "Export as PNG"
- **Iconify** or **Feather Icons** (chat / message icons)
- **RealFaviconGenerator** or **AppIcon.co** for multi-size export
- **Inkscape** or **Adobe Illustrator** for vector design

## Example Icon Style Suggestion

A modern chat icon could be:
- Two rounded speech bubbles overlapping
- One bubble in indigo (`#6366f1`), the other slightly lighter or white with stroke
- Clean line style or flat design with subtle depth

---

Once the PNG files are placed here, the application will automatically load them on startup (see `ChatClientApplication.setAppIcons()`).
