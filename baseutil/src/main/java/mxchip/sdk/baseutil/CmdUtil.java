package mxchip.sdk.baseutil;

import android.text.TextUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class CmdUtil {

    private static final String TAG = "CmdUtil";

    /**
     * 重启设备
     */
    public static void restartDevice() {
        try {
            String cmd = "su -c reboot";
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 执行命令并且输出结果
    public static String execRootCmd(String filePath, String sh) {
        String cmd = "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm install -r " + filePath;
        String result = "";
        DataOutputStream dos = null;
        DataInputStream dis = null;

        try {
            Process p = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());

            dos.writeBytes("chmod 777 " + filePath + "\n");
            dos.writeBytes(cmd + "\n");
            dos.flush();
            //可以写多条命令
            if (!TextUtils.isEmpty(sh)) {
                dos.writeBytes(sh);
                dos.flush();
            }
            String line = null;
            while ((line = dis.readLine()) != null) {
                result += line;
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public static void executer(final String command) {
        if (command.isEmpty())
            return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Process process = null;
                DataOutputStream os = null;
                try {
                    process = Runtime.getRuntime().exec("su");

                    os = new DataOutputStream(process.getOutputStream());
                    os.writeBytes(command + "\n");
                    os.flush();
                    process.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (os != null)
                            os.close();
                        if (process != null)
                            process.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
