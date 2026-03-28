package com.github.lizhanyin.tfs.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.jetbrains.annotations.NotNull;

/**
 * A concrete multi-status implementation, suitable either for instantiating or
 * subclassing.
 * <p>
 * This class can be used without OSGi running.
 * </p>
 */
public class MultiStatus extends Status {

    /**
     * List of child statuses.
     */
    private final List<IStatus> children = new ArrayList<>();

    /**
     * Creates and returns a new multi-status object with the given children.
     *
     * @param caller      the relevant class to build unique identifier from
     * @param code        the caller-specific status code
     * @param newChildren the list of children status objects
     * @param message     a human-readable message, localized to the current locale
     * @param exception   a low-level exception, or <code>null</code> if not
     *                    applicable
     *
     * @since 3.12
     */
    public MultiStatus(Class<?> caller, int code, @NotNull IStatus[] newChildren, String message, Throwable exception) {
        this(caller, code, message, exception);
        addAllInternal(newChildren);
    }

    /**
     * Creates and returns a new multi-status object with the given children.
     *
     * @param pluginId    the unique identifier of the relevant plug-in
     * @param code        the plug-in-specific status code
     * @param newChildren the list of children status objects
     * @param message     a human-readable message, localized to the current locale
     * @param exception   a low-level exception, or <code>null</code> if not
     *                    applicable
     */
    public MultiStatus(String pluginId, int code, @NotNull IStatus[] newChildren, String message, Throwable exception) {
        this(pluginId, code, message, exception);
        addAllInternal(newChildren);
    }

    /**
     * Creates and returns a new multi-status object with no children.
     *
     * @param caller    the relevant class to build unique identifier from
     * @param code      the caller-specific status code
     * @param message   a human-readable message, localized to the current locale
     * @param exception a low-level exception, or <code>null</code> if not
     *                  applicable
     *
     * @since 3.12
     */
    public MultiStatus(Class<?> caller, int code, String message, Throwable exception) {
        super(OK, caller, code, message, exception);
    }

    /**
     * Creates and returns a new multi-status object with no children.
     *
     * @param pluginId  the unique identifier of the relevant plug-in
     * @param code      the plug-in-specific status code
     * @param message   a human-readable message, localized to the current locale
     * @param exception a low-level exception, or <code>null</code> if not
     *                  applicable
     */
    public MultiStatus(String pluginId, int code, String message, Throwable exception) {
        super(OK, pluginId, code, message, exception);
    }

    /**
     * Creates and returns a new multi-status object with no children.
     *
     * @param caller  the relevant class to build unique identifier from
     * @param code    the caller-specific status code
     * @param message a human-readable message, localized to the current locale
     *
     * @since 3.12
     */
    public MultiStatus(Class<?> caller, int code, String message) {
        super(OK, caller, code, message, null);
    }

    /**
     * Creates and returns a new multi-status object with no children.
     *
     * @param pluginId the unique identifier of the relevant plug-in
     * @param code     the plug-in-specific status code
     * @param message  a human-readable message, localized to the current locale
     * @since 3.11
     */
    public MultiStatus(String pluginId, int code, String message) {
        super(OK, pluginId, code, message, null);
    }

    /**
     * Adds the given status to this multi-status.
     *
     * @param status the new child status
     */
    public void add(@NotNull IStatus status) {
        children.add(status);
        int newSev = status.getSeverity();
        if (newSev > getSeverity()) {
            setSeverity(newSev);
        }
    }

    /**
     * Adds all of the children of the given status to this multi-status. Does
     * nothing if the given status has no children (which includes the case where it
     * is not a multi-status).
     *
     * @param status the status whose children are to be added to this one
     */
    public void addAll(@NotNull IStatus status) {
        addAllInternal(status.getChildren());
    }

    private void addAllInternal(@NotNull IStatus[] newChildren) {
        int maxSeverity = getSeverity();
        for (IStatus child : newChildren) {
            int severity = child.getSeverity();
            if (severity > maxSeverity) {
                maxSeverity = severity;
            }
        }
        this.children.addAll(Arrays.asList(newChildren));
        setSeverity(maxSeverity);
    }

    @Override
    public IStatus[] getChildren() {
        return children.toArray(new IStatus[0]);
    }

    @Override
    public boolean isMultiStatus() {
        return true;
    }

    /**
     * Merges the given status into this multi-status. Equivalent to
     * <code>add(status)</code> if the given status is not a multi-status.
     * Equivalent to <code>addAll(status)</code> if the given status is a
     * multi-status.
     *
     * @param status the status to merge into this one
     * @see #add(IStatus)
     * @see #addAll(IStatus)
     */
    public void merge(@NotNull IStatus status) {
        if (!status.isMultiStatus()) {
            add(status);
        } else {
            addAll(status);
        }
    }

    /**
     * Returns a string representation of the status, suitable for debugging
     * purposes only.
     */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(" ", super.toString() + " children=[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (IStatus child : children) {
            joiner.add(child.toString());
        }
        return joiner.toString();
    }
}