package yk.layout_inspector;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.layoutinspector.LayoutInspectorBridge;
import com.android.layoutinspector.LayoutInspectorCaptureOptions;
import com.android.layoutinspector.LayoutInspectorResult;
import com.android.layoutinspector.ProtocolVersion;
import com.android.layoutinspector.model.ClientWindow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String ADB_PATH = "C:\\Users\\luoshuangxi\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe";
    private static final String APP_NAME = "org.mokee.lawnchair";

    private static final boolean LAYOUT_INSPECTOR_V2_PROTOCOL_ENABLED = false;

    public static void exitWithError() {
        System.exit(-1);
    }

    public static IDevice getDevice(String adbPath) {
        AndroidDebugBridge.init(true);
        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(adbPath, false);
        if (!waitForDevice(bridge, 5)) {
            System.out.println("no devices connected.");
            return null;
        }
        IDevice[] devices = bridge.getDevices();
        if (devices == null || devices.length != 1) {
            throw new IllegalStateException("no device or no only one device");
        }
        return devices[0];
    }

    static ProtocolVersion determineProtocolVersion(int apiVersion, boolean v2Enabled) {
        return apiVersion >= LayoutInspectorBridge.getV2_MIN_API() && v2Enabled ? ProtocolVersion.Version2 : ProtocolVersion.Version1;
    }

    public static void main(String[] args) {
        File captures = new File("captures");
        if (!captures.exists() || !captures.isDirectory()) {
            System.out.println("mkdir captures ...");
            captures.mkdir();
        }

        IDevice device = getDevice(ADB_PATH);
        System.out.println(device.getName());
        Client client = device.getClient(APP_NAME);
        if (client == null) {
            System.out.println("程序名错误");
            exitWithError();
            return;
        }
        try {
            List<ClientWindow> windows = ClientWindow.getAll(client, 20, TimeUnit.SECONDS);
            if (windows.size() == 0) {
                System.out.println("没有window");
                exitWithError();
                return;
            }
            for (int i = 0; i < windows.size(); i++) {
                ClientWindow clientWindow = windows.get(i);
                System.out.println(i + ":" + clientWindow.getDisplayName());
            }
            Scanner scanner = new Scanner(System.in);
            int i = scanner.nextInt();
            if (i < 0 || i > windows.size() - 1) {
                System.out.println("选择范围错误");
                exitWithError();
                return;
            }
            ClientWindow window = windows.get(i);
            LayoutInspectorCaptureOptions options = new LayoutInspectorCaptureOptions();
            options.setTitle(window.getDisplayName());
            ProtocolVersion version =
                    determineProtocolVersion(client.getDevice().getVersion().getApiLevel(), LAYOUT_INSPECTOR_V2_PROTOCOL_ENABLED);
            options.setVersion(
                    version);
            LayoutInspectorResult result = LayoutInspectorBridge.captureView(window, options);
            byte[] bytes = result.getData();
            if (bytes != null) {
                IOUtil.saveBytes("captures/" + APP_NAME + "_" + System.currentTimeMillis() + ".li", bytes);
                System.out.println("save ok");
                System.exit(0);
            } else {
                System.out.println("dump view error");
                exitWithError();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean waitForDevice(AndroidDebugBridge adb, int timeSeconds) {
        int i = 0;
        while (!adb.isConnected() && i++ < timeSeconds) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("wait for device");
        }
        return adb.isConnected();
    }

}
