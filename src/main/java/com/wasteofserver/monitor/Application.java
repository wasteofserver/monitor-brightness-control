package com.wasteofserver.monitor;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Application extends JFrame {
    List<WinUser.HMONITOR> monitors = new ArrayList<>();
    int currentBrightnessValue = 20;
    NativeKeyListener nativeKeyListener = new NativeKeyListener() {
        public void nativeKeyPressed(NativeKeyEvent e) {
            // Check if Ctrl+Alt+5 is pressed
            if (e.getID() == NativeKeyEvent.NATIVE_KEY_PRESSED && (e.getModifiers() & NativeInputEvent.CTRL_MASK) != 0 && (e.getModifiers() & NativeInputEvent.ALT_MASK) != 0 && e.getKeyCode() == NativeKeyEvent.VC_5) {
                currentBrightnessValue = changeBrightnessValue(currentBrightnessValue, -5);
                changeBrightness(currentBrightnessValue);
                consumeEvent(e);
            }

            // Check if Ctrl+Alt+6 is pressed
            if (e.getID() == NativeKeyEvent.NATIVE_KEY_PRESSED && (e.getModifiers() & NativeInputEvent.CTRL_MASK) != 0 && (e.getModifiers() & NativeInputEvent.ALT_MASK) != 0 && e.getKeyCode() == NativeKeyEvent.VC_6) {
                currentBrightnessValue = changeBrightnessValue(currentBrightnessValue, 5);
                changeBrightness(currentBrightnessValue);
                consumeEvent(e);
            }
        }

        public void nativeKeyReleased(NativeKeyEvent e) {
            // Nothing here
        }

        public void nativeKeyTyped(NativeKeyEvent e) {
            // Nothing here
        }
    };

    public static void main(String[] args) {
        new Application();
    }

    Application() {
        super("Application"); // Set the title of the JFrame
        unregisterGlobalShortCutsShutdownHook();
        registerGlobalShortCuts();

        initMonitors();
        setGlobalBrightnessToCurrentValue();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set the default close operation
        setSize(400, 300); // Set the size of the JFrame


        startOnSystemTray();
        setVisible(false);
    }

    static int changeBrightnessValue(int currentBrightness, int change) {
        int newBrightness = currentBrightness + change;
        if (newBrightness > 100) {
            newBrightness = 100;
        } else if (newBrightness < 0) {
            newBrightness = 0;
        }
        System.out.println("New brightness: " + newBrightness + " (old brightness: " + currentBrightness + " proposed change: " + change + ")");
        return newBrightness;
    }

    private static void consumeEvent(NativeKeyEvent e) {
        try {
            Field f = NativeInputEvent.class.getDeclaredField("reserved");
            f.setAccessible(true);
            f.setShort(e, (short) 0x01);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void startOnSystemTray() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon_orange.png"));

            ActionListener exitListener = e -> System.exit(0);

            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem("Exit monitor-brightness-control");
            defaultItem.addActionListener(exitListener);
            popup.add(defaultItem);

            TrayIcon trayIcon = setupTrayBehaviour(image, popup);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        } else {
            setExtendedState(JFrame.ICONIFIED);
        }
    }

    private TrayIcon setupTrayBehaviour(Image image, PopupMenu popup) {
        TrayIcon trayIcon = new TrayIcon(image, "Application", popup);
        trayIcon.setImageAutoSize(true);

        // Add a mouse listener to handle double-click open events
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setVisible(true);
                }
            }
        });
        return trayIcon;
    }

    void changeBrightness(int brightness) {
        if (brightness > 100 || brightness < 0) {
            System.out.println("Brightness should be between 0 and 100");
            return;
        }

        monitors.parallelStream().forEach(hmonitor -> {
            PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR[] physMons = new PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR[1];
            if (Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(hmonitor, 1, physMons).booleanValue()) {
                WinNT.HANDLE monitor = physMons[0].hPhysicalMonitor;
                Dxva2.INSTANCE.SetMonitorBrightness(monitor, brightness);
                Dxva2.INSTANCE.DestroyPhysicalMonitor(monitor);
            }
        });
    }

    void setGlobalBrightnessToCurrentValue() {
        for (WinUser.HMONITOR hmonitor : monitors) {
            PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR[] physMons = new PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR[1];
            if (Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(hmonitor, 1, physMons).booleanValue()) {
                WinNT.HANDLE monitor = physMons[0].hPhysicalMonitor;

                DWORDByReference pdwMinimumBrightness = new DWORDByReference();
                DWORDByReference pdwCurrentBrightness = new DWORDByReference();
                DWORDByReference pdwMaximumBrightness = new DWORDByReference();

                if (Dxva2.INSTANCE.GetMonitorBrightness(monitor, pdwMinimumBrightness, pdwCurrentBrightness, pdwMaximumBrightness).booleanValue()) {
                    currentBrightnessValue = pdwCurrentBrightness.getValue().intValue();
//                    System.out.println("Monitor " + hmonitor + " currentBrightness: " + pdwCurrentBrightness.getValue());
//                    System.out.println("Monitor " + hmonitor + " minBrightness: " + pdwMinimumBrightness.getValue());
//                    System.out.println("Monitor " + hmonitor + " maxBrightness: " + pdwMaximumBrightness.getValue());
                    System.out.println("Set global current brightness to " + currentBrightnessValue);
                    currentBrightnessValue = pdwCurrentBrightness.getValue().intValue();
                } else {
                    System.out.println("Monitor " + hmonitor + " failed to get brightness");
                }
                Dxva2.INSTANCE.DestroyPhysicalMonitor(monitor);
            }
        }
    }

    void initMonitors() {
        User32.INSTANCE.EnumDisplayMonitors(null, null, new WinUser.MONITORENUMPROC() {
            int count = 0;

            @Override
            public int apply(WinUser.HMONITOR hmonitor, WinDef.HDC hdc, WinDef.RECT rect, WinDef.LPARAM lparam) {
                WinUser.MONITORINFOEX monitorInfo = new WinUser.MONITORINFOEX();
                User32.INSTANCE.GetMonitorInfo(hmonitor, monitorInfo);
                String monitorName = new String(monitorInfo.szDevice).trim();
                System.out.println("Monitor " + count++ + ": " + hmonitor + ", Name: " + monitorName + ", Primary: " + (monitorInfo.dwFlags == 1 ? "Yes" : "No"));
                monitors.add(hmonitor);
                return 1;
            }
        }, null);
    }

    void unregisterGlobalShortCutsShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                GlobalScreen.removeNativeKeyListener(nativeKeyListener);
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex) {
                System.err.println("There was a problem unregistering the native hook.");
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        }));
    }

    void registerGlobalShortCuts() {
        try {
            GlobalScreen.setEventDispatcher(new VoidDispatchService());
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }


        GlobalScreen.addNativeKeyListener(nativeKeyListener);
    }


}
