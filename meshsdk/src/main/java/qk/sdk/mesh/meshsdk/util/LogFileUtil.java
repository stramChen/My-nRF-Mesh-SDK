package qk.sdk.mesh.meshsdk.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import qk.sdk.mesh.meshsdk.bean.LogMsg;

public class LogFileUtil {
    private static final String kTAG = "LogFileUtil";

    /**
     * 判断是否存在外部存储
     */
    public static boolean isExternalStorageMounted() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * 创建应用文件夹
     */
    public static void createFolderForApp() {
        if (isExternalStorageMounted()) {
            try {
                //下载目录
                File download = new File(Constants.INSTANCE.getFOLDER_DOWNLOAD());
                if (download != null) {
                    if (!download.exists()) {
                        download.mkdirs();
                    }
                }

                //缓存目录
                File cache = new File(Constants.INSTANCE.getFOLDER_CACHE());
                if (cache != null) {
                    if (!cache.exists()) {
                        cache.mkdirs();
                    }
                }

                //日志目录
//                File photo = new File(Constants.INSTANCE.getFOLDER_LOG());
//                if (photo != null) {
//                    if (!photo.exists()) {
//                        photo.mkdirs();
//                    }
//                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查之前是否有未上传的日志
     *
     * @param context
     */
    public static void deleteLog(Context context) {
        File file = new File(context.getFilesDir().getAbsolutePath() + File.separator);
        File[] files = file.listFiles();
        if (file != null && files != null && files.length > 0) {
            for (File logFile : files) {
                String fileName = logFile.getName();
                String[] names = fileName.split("_");
                if (names != null && names.length >= 3) {
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        Date date = formatter.parse(names[2] + " 00:00:00");
                        Date nowDate = new Date();
                        long diff = nowDate.getTime() - date.getTime();
                        if (diff / 1000 / 60 / 60 / 24 <= 1) {//本地保留当天的日志，其他日志删除
                            //todo mxchip 此处处理逻辑待与产品确定
                            logFile.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param filePath 保存路径
     * @param bm       要保存的bitmap
     * @return boolean 是否保存成功
     * @Description:将bitmap保存到本地文件夹中
     */
    public static boolean saveBitmapIntoFile(String filePath, Bitmap bm) {
        File file = new File(filePath);
        boolean isSaved = false;
        FileOutputStream fops = null;
        if (!file.exists() || !file.isDirectory()) {
            try {
                file.createNewFile();

                fops = new FileOutputStream(file);
                isSaved = bm.compress(Bitmap.CompressFormat.JPEG, 100, fops);
                fops.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (isSaved && bm != null && !bm.isRecycled()) {
                    bm.recycle();
                }
                if (fops != null) {
                    try {
                        fops.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return isSaved;
    }

    /**
     * @param b    要存储的字节数组
     * @param path 存储路径
     * @return 是否存储成功
     */
    public static boolean saveByteToFile(byte[] b, String path) {
        File file = new File(path);
        boolean isSaved = false;
        FileOutputStream fops = null;
        if (!file.exists() || !file.isDirectory()) {
            try {
                file.createNewFile();

                fops = new FileOutputStream(file);
                fops.write(b);
                fops.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fops != null) {
                    try {
                        fops.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return isSaved;
    }

    public static ArrayList<String> listAlldir(Context context) {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Uri uri = intent.getData();
        ArrayList<String> list = new ArrayList<String>();
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null,
                null, MediaStore.Images.Media.DATE_ADDED + " DESC");// managedQuery(uri,
        // proj,
        // null,
        // null,
        // null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(0);
                    list.add(new File(path).getAbsolutePath());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return list;
    }

    public static String getfileinfo(String data) {
        String filename[] = data.split("/");
        if (filename != null) {
            return filename[filename.length - 2];
        }
        return null;
    }

    /**
     * @param path 路径名称（不含文件名称）
     * @return 完整的路径（含文件名称）
     * @Description 获取Camera的临时存储路径
     */
    public static File getTemporaryFileName(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        String name = path + System.currentTimeMillis()
                + ".jpg";
        File filePath = new File(name);
        if (!filePath.exists()) {
            filePath.createNewFile();
        }

        return filePath;
    }

    private static DateFormat mLogFormate = new SimpleDateFormat("yyyy-MM-dd");

    public static String getInnerFileName(String fileName) {
        String time = mLogFormate.format(new Date());
        return fileName + "_" + time;
    }

    /**
     * Writes a byte array to a file creating the file if it does not exist.
     *
     * @param file the file to write to
     * @param data the content to write to the file
     *             end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since IO 2.1
     */
    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file, false);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canWrite()) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    /**
     * Reads the contents of a file into a byte array.
     * The file is always closed.
     *
     * @param file the file to read, must not be {@code null}
     * @return the file contents, never {@code null}
     * @throws IOException in case of an I/O error
     */
    public static byte[] readFileToByteArray(File file) {
        InputStream in = null;
        try {
            in = openInputStream(file);
            byte[] data = toByteArray(in, (int) file.length());
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                // ignore
                ioe.printStackTrace();
            }
        }
        return null;
    }

    public static byte[] toByteArray(InputStream input, int size) throws IOException {

        if (size < 0) {
            throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);
        }

        if (size == 0) {
            return new byte[0];
        }

        byte[] data = new byte[size];
        int offset = 0;
        int readed;

        while (offset < size && (readed = input.read(data, offset, size - offset)) != -1) {
            offset += readed;
        }

        if (offset != size) {
            throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + size);
        }

        return data;
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canRead()) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    /**
     * 从本地资源中读取图片
     */
    public static Bitmap readBitMap(Context context, int resId) {
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opt.inPurgeable = true;
            opt.inInputShareable = true;
            opt.inSampleSize = 2;
            // 获取资源图片
            InputStream is = context.getResources().openRawResource(resId);
            return BitmapFactory.decodeStream(is, null, opt);
        } catch (Exception e) {

        }
        return null;
    }

    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static long downLoadFile(Context context, String url, String fileName) {
        String path = Constants.INSTANCE.getBASE_PATH() + "/download/" + fileName;
        File file = new File(path);
        if (file.exists())
            return 0;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir("/download/", fileName);
        //获取下载管理器
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        long taskId = downloadManager.enqueue(request);
        return taskId;
    }

    public static void copyFromAssetsToSdcard(Context context, boolean isCover, String source, String dest) {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = context.getResources().getAssets().open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {

        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT+08:00"));
    }

//    public synchronized static void writeFileToSDCard(final String buffer, final String folder,
//                                                      final String fileName, final boolean append, final boolean autoLine) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (fileName.isEmpty())
//                    return;
//
//                boolean sdCardExist = Environment.getExternalStorageState().equals(
//                        Environment.MEDIA_MOUNTED);
//                String folderPath;
//                if (sdCardExist) {
//                    if (TextUtils.isEmpty(folder)) {
//                        //如果folder为空，则直接保存在sd卡的根目录
//                        folderPath = Environment.getExternalStorageDirectory()
//                                + File.separator;
//                    } else {
//                        folderPath = folder;
//                    }
//                } else {
//                    return;
//                }
//
//                File fileDir = new File(folderPath);
//                if (!fileDir.exists()) {
//                    if (!fileDir.mkdirs()) {
//                        return;
//                    }
//                }
//                File file;
//                //判断文件名是否为空
//                if (TextUtils.isEmpty(fileName)) {
//                    file = new File(folderPath + "app_log.txt");
//                } else {
//                    file = new File(folderPath + fileName);
//                }
//                RandomAccessFile raf = null;
//                FileOutputStream out = null;
//                try {
//                    if (append) {
//                        //如果为追加则在原来的基础上继续写文件
//                        raf = new RandomAccessFile(file, "rw");
//                        raf.seek(file.length());
//                        raf.write((new LogMsg(dateFormat.format(new Date()), buffer)).toString().getBytes());
//                        if (autoLine) {
//                            raf.write("\n".getBytes());
//                        }
//                    } else {
//                        //重写文件，覆盖掉原来的数据
//                        out = new FileOutputStream(file);
//                        out.write((new LogMsg(dateFormat.format(new Date()), buffer)).toString().getBytes());
//                        out.flush();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    String fileName = CommonUtil.INSTANCE.getMacAddress().replace(":", "-");
//                    CmdUtil.executer("chmod 777 " + Constants.INSTANCE.getFOLDER_STYD());
//                    CmdUtil.executer("adb logcat -v time > /data/data/styd.hardware.gate.sec/files/" + fileName + "_android.txt");
//                    CmdUtil.executer("adb shell dmesg -n 8 > /data/data/styd.hardware.gate.sec/files/" + fileName + "_kernel.txt");
//                    CmdUtil.executer("adb shell mount > /data/data/styd.hardware.gate.sec/files/" + fileName + "_mount.txt");
//
//                    CmdUtil.executer("chmod 777 /data/data/styd.hardware.gate.sec/files/" + fileName + "_android.txt");
//                    CmdUtil.executer("chmod 777 /data/data/styd.hardware.gate.sec/files/" + fileName + "_kernel.txt");
//                    CmdUtil.executer("chmod 777 /data/data/styd.hardware.gate.sec/files/" + fileName + "_mount.txt");
//
//                    saveCrashInfo2File(e);
//                } finally {
//                    try {
//                        if (raf != null) {
//                            raf.close();
//                        }
//                        if (out != null) {
//                            out.close();
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
//    }

    /**
     * 向内部存储写日志，默认folder为filsDir，且append、autoLine 为true，
     *
     * @param context
     * @param buffer
     * @param fileName
     */
    public static void writeLogToInnerFile(final Context context, final String buffer,
                                           final String fileName) {
        String folder = context.getFilesDir().getAbsolutePath() + File.separator;
        writeLogToInnerFile(context, buffer, folder, fileName, true, true);
    }

    public static void writeLogToInnerFile(final Context context, final String buffer, final String folder,
                                           final String fileName, final boolean append, final boolean autoLine) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (fileName.isEmpty() || context == null)
                    return;

                RandomAccessFile raf = null;
                FileOutputStream out = null;
                try {
                    File folderPath = new File(folder);
                    if (!folderPath.exists()) {
                        if (!folderPath.mkdirs()) {
                            return;
                        }
                    }

                    File logPath = new File(folder + fileName);
                    if (!logPath.exists()) {
                        if (!logPath.createNewFile())
                            return;
                        CmdUtil.executer("chmod 777 " + folder);
                    }

                    if (append) {
                        //如果为追加则在原来的基础上继续写文件
                        raf = new RandomAccessFile(logPath, "rw");
                        raf.seek(logPath.length());
                        raf.write((new LogMsg(dateFormat.format(new Date()), buffer)).toString().getBytes());
                        if (autoLine) {
                            raf.write("\n".getBytes());
                        }
                    } else {
                        //重写文件，覆盖掉原来的数据
                        out = new FileOutputStream(logPath);
                        out.write((new LogMsg(dateFormat.format(new Date()), buffer)).toString().getBytes());
                        out.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (raf != null) {
                            raf.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static String readLog(Context context, String filename) {
        try {
            //打开文件输入流
//            FileInputStream input = context.openFileInput(filename);
            File file = new File(filename);
            FileInputStream input = new FileInputStream(file);
            byte[] temp = new byte[1024 * 4];
            //定义字符串变量
            StringBuilder sb = new StringBuilder("");
            int len = 0;
            //读取文件内容，当文件内容长度大于0时，
            while ((len = input.read(temp)) > 0) {
                //把字条串连接到尾部
                sb.append(new String(temp, 0, len));
            }
            //关闭输入流
            input.close();
            //返回字符串
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            Throwable cause = e.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.close();
            String result = writer.toString();
            return result;
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return 返回文件名称, 便于将文件传送到服务器
     */
//    public static String saveCrashInfo2File(Context context, Exception ex) {
//        StringBuffer sb = new StringBuffer();
//
//        Writer writer = new StringWriter();
//        PrintWriter printWriter = new PrintWriter(writer);
//        ex.printStackTrace(printWriter);
//        Throwable cause = ex.getCause();
//        while (cause != null) {
//            cause.printStackTrace(printWriter);
//            cause = cause.getCause();
//        }
//        printWriter.close();
//        String result = writer.toString();
//        sb.append(result);
//        String path = context.getFilesDir().getAbsolutePath() + File.separator;
//        try {
//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                File dir = new File(path);
//                if (!dir.exists()) {
//                    dir.mkdirs();
//                }
//                LogFileUtil.writeLogToInnerFile(context, sb.toString(), path, CommonUtil.INSTANCE.getSocketFileName(1), true, true);
//            }
//            return CommonUtil.INSTANCE.getSocketFileName(1);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
