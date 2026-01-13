package hdf.view;

/**
 * Listener interface for components that need to respond to theme changes.
 *
 * Components implementing this interface can register with ThemeManager to receive
 * notifications when the application theme changes, allowing for dynamic UI updates
 * without requiring an application restart.
 *
 * @author The HDF Group
 */
public interface ThemeChangeListener {
    /**
     * Called when the application theme changes.
     *
     * Implementations should update their UI components to reflect the new theme.
     * This method is always called on the SWT UI thread, so direct widget updates
     * are safe.
     *
     * @param oldTheme the previous theme setting
     * @param newTheme the new theme setting
     * @param colors the new color scheme to apply
     */
    void onThemeChanged(ThemeManager.Theme oldTheme,
                       ThemeManager.Theme newTheme,
                       ThemeManager.ColorScheme colors);
}
