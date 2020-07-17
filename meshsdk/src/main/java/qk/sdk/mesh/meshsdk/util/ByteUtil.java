package qk.sdk.mesh.meshsdk.util;

import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ByteUtil {
    static final String TAG = "ByteUtil";

    /**
     * int到字节数组的转换！
     *
     * @param number
     * @return
     */
    public static byte[] intToByte(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();//将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    /**
     * short到字节数组的转换
     *
     * @param
     * @return
     */
    public static byte[] shortToByte(short number) {
        byte[] targets = new byte[2];
        for (int i = 0; i < 2; i++) {
            int offset = (targets.length - 1 - i) * 8;
            targets[targets.length-1 - i] = (byte) ((number >>> offset) & 0xff);
        }
        return targets;
    }

    public static short byteToShort(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);// 最低位
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }

    public static String bytesToHexString(byte[] src) {
        if (src == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv.toUpperCase());
        }
        return stringBuilder.toString();
    }

    public static byte[] byteTobitArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    public static String byteTobitString(byte b) {
        return Integer.toBinaryString(b);
    }

    public static byte[] hexStringToBytes(String hex) {
        int m = 0, n = 0;
        int byteLen = hex.length() / 2; // 每两个字符描述一个字节
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
            ret[i] = Byte.valueOf((byte) intVal);
        }
        return ret;
    }

    public static String rgbtoHex(int r) {
        return toBrowserHexValue(r);
    }

    private static String toBrowserHexValue(int number) {
        StringBuilder builder = new StringBuilder(
                Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

    public static int getPIdFromUUID(byte[] uuid) {
        if (uuid.length < 5) {
            return 0;
        }
        byte[] pid = new byte[4];
        System.arraycopy(uuid, 1, pid, 0, 4);
        return byteArrayToInt(pid);
    }

    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        // 由高位到低位
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;// 往高位游
        }
        return value;
    }

    public static byte[] parseFromBytes(@Nullable final byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        int txPowerLevel = Integer.MIN_VALUE;
        String localName = null;
        List<ParcelUuid> serviceUuids = null;
        SparseArray<byte[]> manufacturerData = null;
        Map<ParcelUuid, byte[]> serviceData = null;

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                final int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                final int dataLength = length - 1;
                // fieldType is unsigned int.
                final int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case 0x16:
                        byte[] uuid = new byte[dataLength - 1];
                        uuid[0] = 0x00;
                        uuid[1] = scanRecord[currentPos + 2];
                        System.arraycopy(scanRecord, currentPos + 2, uuid, 2, uuid.length);
                        Utils.INSTANCE.printLog(TAG, "currentPos:" + currentPos + "uuid:" + ByteUtil.bytesToHexString(uuid) + " ,scanRecord:" + ByteUtil.bytesToHexString(scanRecord));
                        return uuid;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

        } catch (final Exception e) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
        }
        return null;
    }
}
