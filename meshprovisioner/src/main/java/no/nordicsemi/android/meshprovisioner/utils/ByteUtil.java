package no.nordicsemi.android.meshprovisioner.utils;

public class ByteUtil {
    /**
     * 注释：int到字节数组的转换！
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
     * 注释：short到字节数组的转换！
     *
     * @param
     * @return
     */
    public static byte[] shortToByte(short number) {
        byte[] targets = new byte[2];
        for (int i = 0; i < 2; i++) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte) ((number >>> offset) & 0xff);
        }
        return targets;
    }

    /**
     * 加密数据
     *
     * @param bytes
     * @return
     */
    public static byte[] encryptByte(byte[] bytes) {
        byte[] reverseByte = new byte[bytes.length];

        //逆序
        int index = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            reverseByte[index] = bytes[i];
            index++;
        }

        //取反
        byte[] complementBytes = new byte[reverseByte.length];
        for (int i = 0; i < reverseByte.length; i++) {
            byte complement = (byte) ~(int) reverseByte[i];
            complementBytes[i] = complement;
        }

//        //加随机数
//        byte[] addBytes = new byte[complementBytes.length];
//        for (int i = 0; i < complementBytes.length; i++) {
//            byte add = (byte) (complementBytes[i] + randomNum);
//            addBytes[i] = add;
//        }
//        LockUtil.byte2hex(addBytes);

        return complementBytes;
    }

    /**
     * 解密数据
     *
     * @param
     * @return
     */
    public static byte[] decryptByte(byte[] bytes) {
//        //减随机数
//        byte[] deleteBytes = new byte[bytes.length];
//        for (int i = 0; i < deleteBytes.length; i++) {
//            deleteBytes[i] = (byte) (bytes[i] - randomNum);
//        }

        //取反
        byte[] complementBytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte complement = (byte) ~bytes[i];
            complementBytes[i] = complement;
        }

        //逆序
        byte[] reverseBytes = new byte[complementBytes.length];
        int index = 0;
        for (int i = complementBytes.length - 1; i >= 0; i--) {
            reverseBytes[index] = complementBytes[i];
            index++;
        }

        return reverseBytes;
    }

    public static int calcCRC16(byte[] pArray, int length) {
        int wCRC = 0xFFFF;
        int CRC_Count = length;
        int i;
        int num = 0;
        while (CRC_Count > 0) {
            CRC_Count--;
            wCRC = wCRC ^ (0xFF & pArray[num++]);
            for (i = 0; i < 8; i++) {
                if ((wCRC & 0x0001) == 1) {
                    wCRC = wCRC >> 1 ^ 0xA001;
                } else {
                    wCRC = wCRC >> 1;
                }
            }
        }
        return wCRC;
    }

    /**
     * CRC16 modbus
     *
     * @param bytes 需校验的byte数组
     * @return 校验码数组（length为2）
     */
    public static byte[] getCRC(byte[] bytes) {
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }

        byte[] crcBytes = new byte[2];
        crcBytes[0] = (byte) (CRC >> 8);
        crcBytes[1] = (byte) (CRC & 0x00ff);
        return crcBytes;
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
}
