package hdf.view;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThemeManager provides centralized theme and color management for HDFView.
 *
 * Supports automatic dark mode detection and manual theme override via user preferences.
 * All UI components should use ThemeManager for consistent theming across the application.
 *
 * @author The HDF Group
 */
public class ThemeManager {
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    /** Singleton instance */
    private static ThemeManager instance;

    /** Current theme */
    private Theme currentTheme;

    /** Display instance */
    private final Display display;

    /** Thread-safe list of theme change listeners */
    private final List<ThemeChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Theme enumeration
     */
    public enum Theme {
        /** Automatic theme detection based on OS preference */
        AUTO("Auto (System)"),
        /** Force light theme */
        LIGHT("Light"),
        /** Force dark theme */
        DARK("Dark");

        private final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Theme fromString(String value) {
            if (value == null) return AUTO;
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid theme value: {}, defaulting to AUTO", value);
                return AUTO;
            }
        }
    }

    /**
     * Color scheme definition.
     *
     * IMPORTANT: ColorScheme creates custom Color objects that are OS resources.
     * Always call close() when the ColorScheme is no longer needed to prevent
     * resource leaks.
     *
     * Implements AutoCloseable to support try-with-resources and signal that
     * this class manages disposable resources.
     */
    public static class ColorScheme implements AutoCloseable {
        // Background colors
        public final Color background;
        public final Color infoBackground;
        public final Color widgetBackground;
        public final Color listBackground;

        // Foreground colors
        public final Color foreground;
        public final Color infoForeground;
        public final Color widgetForeground;
        public final Color listForeground;

        // UI element colors
        public final Color selectionBackground;
        public final Color selectionForeground;
        public final Color borderColor;
        public final Color shadowColor;

        // Chart and data visualization colors
        public final Color chartBackground;
        public final Color chartForeground;
        public final Color chartGridColor;
        public final Color histogramBarColor;
        public final Color[] chartLineColors;

        // Track custom colors that need disposal (system colors are NOT disposed)
        private final java.util.List<Color> customColors = new java.util.ArrayList<>();

        ColorScheme(Display display, boolean isDark) {
            if (isDark) {
                // Dark theme colors
                this.background = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
                this.infoBackground = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
                this.widgetBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
                this.listBackground = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);

                this.foreground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
                this.infoForeground = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
                this.widgetForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
                this.listForeground = display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);

                this.selectionBackground = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
                this.selectionForeground = display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
                this.borderColor = display.getSystemColor(SWT.COLOR_WIDGET_BORDER);
                this.shadowColor = display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

                // Dark theme chart colors
                this.chartBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
                this.chartForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
                this.chartGridColor = display.getSystemColor(SWT.COLOR_GRAY);
                this.histogramBarColor = display.getSystemColor(SWT.COLOR_LIST_SELECTION);

                // Chart line colors optimized for dark backgrounds
                // Create custom colors and track them for disposal
                Color orange = new Color(display, new RGB(255, 128, 0));
                Color lightBlue = new Color(display, new RGB(128, 128, 255));
                Color pink = new Color(display, new RGB(255, 192, 203));

                customColors.add(orange);
                customColors.add(lightBlue);
                customColors.add(pink);

                this.chartLineColors = new Color[] {
                    display.getSystemColor(SWT.COLOR_CYAN),
                    display.getSystemColor(SWT.COLOR_YELLOW),
                    display.getSystemColor(SWT.COLOR_GREEN),
                    display.getSystemColor(SWT.COLOR_MAGENTA),
                    orange,
                    display.getSystemColor(SWT.COLOR_RED),
                    lightBlue,
                    pink
                };
            } else {
                // Light theme colors
                this.background = display.getSystemColor(SWT.COLOR_WHITE);
                this.infoBackground = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
                this.widgetBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
                this.listBackground = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);

                this.foreground = display.getSystemColor(SWT.COLOR_BLACK);
                this.infoForeground = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
                this.widgetForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
                this.listForeground = display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);

                this.selectionBackground = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
                this.selectionForeground = display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
                this.borderColor = display.getSystemColor(SWT.COLOR_WIDGET_BORDER);
                this.shadowColor = display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

                // Light theme chart colors
                this.chartBackground = display.getSystemColor(SWT.COLOR_WHITE);
                this.chartForeground = display.getSystemColor(SWT.COLOR_BLACK);
                this.chartGridColor = display.getSystemColor(SWT.COLOR_GRAY);
                this.histogramBarColor = display.getSystemColor(SWT.COLOR_BLUE);

                // Chart line colors optimized for light backgrounds
                // Light theme uses only system colors (no disposal needed)
                this.chartLineColors = new Color[] {
                    display.getSystemColor(SWT.COLOR_BLACK),
                    display.getSystemColor(SWT.COLOR_RED),
                    display.getSystemColor(SWT.COLOR_DARK_GREEN),
                    display.getSystemColor(SWT.COLOR_BLUE),
                    display.getSystemColor(SWT.COLOR_MAGENTA),
                    display.getSystemColor(SWT.COLOR_YELLOW),
                    display.getSystemColor(SWT.COLOR_GRAY),
                    display.getSystemColor(SWT.COLOR_CYAN)
                };
            }
        }

        /**
         * Close and dispose custom Color objects to prevent OS resource leaks.
         * System colors are managed by SWT and must NOT be disposed.
         *
         * This method is safe to call multiple times and satisfies the
         * AutoCloseable contract for standardized resource management.
         */
        @Override
        public void close() {
            for (Color color : customColors) {
                if (color != null && !color.isDisposed()) {
                    color.dispose();
                }
            }
            customColors.clear();
        }
    }

    /** Current color scheme */
    private ColorScheme colorScheme;

    /**
     * Private constructor for singleton pattern
     */
    private ThemeManager(Display display) {
        this.display = display;

        // Load theme preference from ViewProperties
        String themePreference = ViewProperties.getThemePreference();
        this.currentTheme = Theme.fromString(themePreference);

        // Initialize color scheme
        updateColorScheme();

        log.info("ThemeManager initialized with theme: {} (actual: {})",
                 currentTheme, isDarkMode() ? "DARK" : "LIGHT");
    }

    /**
     * Get singleton instance.
     *
     * IMPORTANT: This method must be called from the SWT UI thread.
     * Calling from a non-UI thread may cause "Invalid thread access" exceptions.
     *
     * @return the ThemeManager instance
     * @throws IllegalStateException if Display is not available or called from wrong thread
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            // Verify we're on the UI thread
            Display display = Display.getCurrent();
            if (display == null) {
                // Not on UI thread - try to get default, but this may fail
                display = Display.getDefault();
                if (display == null) {
                    throw new IllegalStateException(
                        "Display must be created before ThemeManager. " +
                        "Ensure getInstance() is called from the SWT UI thread.");
                }
                log.warn("ThemeManager.getInstance() called from non-UI thread. " +
                        "This may cause threading issues. Call from UI thread instead.");
            }
            instance = new ThemeManager(display);
        }
        return instance;
    }

    /**
     * Initialize ThemeManager explicitly with a Display instance.
     * This method should be called during application startup from the UI thread.
     *
     * @param display the SWT Display instance
     * @return the ThemeManager instance
     */
    public static synchronized ThemeManager initialize(Display display) {
        if (display == null) {
            throw new IllegalArgumentException("Display cannot be null");
        }
        if (instance == null) {
            instance = new ThemeManager(display);
        }
        return instance;
    }

    /**
     * Detect if the OS is in dark mode
     * Uses SWT's native dark mode detection (available in SWT 3.119+)
     * which queries the OS directly for more reliable detection on macOS and Windows 10/11
     */
    private boolean detectSystemDarkMode() {
        boolean isDark = display.isSystemDarkTheme();
        log.debug("System dark mode detection: isDark={}", isDark);
        return isDark;
    }

    /**
     * Check if current effective theme is dark mode
     */
    public boolean isDarkMode() {
        switch (currentTheme) {
            case LIGHT:
                return false;
            case DARK:
                return true;
            case AUTO:
            default:
                return detectSystemDarkMode();
        }
    }

    /**
     * Get current theme preference
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Set theme preference and update colors.
     * Notifies all registered listeners of the theme change.
     *
     * @param theme the new theme to apply
     */
    public void setTheme(Theme theme) {
        if (theme == null) theme = Theme.AUTO;

        if (this.currentTheme != theme) {
            Theme oldTheme = this.currentTheme;
            log.info("Changing theme from {} to {}", oldTheme, theme);
            this.currentTheme = theme;

            // Save to preferences
            ViewProperties.setThemePreference(theme.name());

            // Update color scheme
            updateColorScheme();

            // Notify listeners
            notifyThemeChanged(oldTheme, theme);
        }
    }

    /**
     * Update color scheme based on current theme.
     * Closes the previous color scheme to prevent resource leaks.
     */
    private void updateColorScheme() {
        // Close old color scheme to prevent resource leaks
        if (this.colorScheme != null) {
            this.colorScheme.close();
        }

        boolean isDark = isDarkMode();
        this.colorScheme = new ColorScheme(display, isDark);
        log.debug("Color scheme updated for {} mode", isDark ? "dark" : "light");
    }

    /**
     * Get current color scheme
     */
    public ColorScheme getColors() {
        return colorScheme;
    }

    /**
     * Refresh theme (call when system theme might have changed)
     */
    public void refresh() {
        log.info("Refreshing theme");
        updateColorScheme();
    }

    /**
     * Shutdown ThemeManager and dispose all resources.
     * This method should be called during application shutdown.
     *
     * After calling this method, ThemeManager should not be used.
     */
    public void shutdown() {
        log.info("Shutting down ThemeManager");

        // Close current color scheme
        if (this.colorScheme != null) {
            this.colorScheme.close();
            this.colorScheme = null;
        }

        // Clear listeners
        listeners.clear();

        // Note: We don't set instance to null because it's a singleton
        // The instance will be cleaned up by JVM shutdown
    }

    /**
     * Register a theme change listener.
     * The listener will be notified whenever the theme changes.
     *
     * @param listener the listener to register (must not be null)
     */
    public void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            log.debug("Registered theme listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a theme change listener.
     *
     * @param listener the listener to remove
     */
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        if (listeners.remove(listener)) {
            log.debug("Unregistered theme listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all registered listeners of a theme change.
     * This method ensures notifications happen on the SWT UI thread.
     *
     * @param oldTheme the previous theme
     * @param newTheme the new theme
     */
    private void notifyThemeChanged(Theme oldTheme, Theme newTheme) {
        final ColorScheme colors = this.colorScheme;

        // Ensure we're on the UI thread
        if (Display.getCurrent() == null) {
            display.asyncExec(() -> fireThemeChangeEvent(oldTheme, newTheme, colors));
        } else {
            fireThemeChangeEvent(oldTheme, newTheme, colors);
        }
    }

    /**
     * Fire the theme change event to all listeners.
     * Must be called on the SWT UI thread.
     *
     * @param oldTheme the previous theme
     * @param newTheme the new theme
     * @param colors the new color scheme
     */
    private void fireThemeChangeEvent(Theme oldTheme, Theme newTheme, ColorScheme colors) {
        log.info("Notifying {} listeners of theme change: {} -> {}",
                 listeners.size(), oldTheme, newTheme);

        for (ThemeChangeListener listener : listeners) {
            try {
                listener.onThemeChanged(oldTheme, newTheme, colors);
            } catch (Exception ex) {
                log.error("Error notifying theme listener: {}",
                         listener.getClass().getSimpleName(), ex);
            }
        }
    }
}
