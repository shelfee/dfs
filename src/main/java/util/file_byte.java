package util;

import java.io.*;

/**
 * Created by shelfee on 2016/12/12.
 */
public class file_byte {
    public static byte[] File2ByteArray(String filename) {
        File file = new File(filename);
        FileInputStream fis = null;
        if(!file.isFile())
            return null;
        try {
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[]b = new byte[1024 > fis.available() ? fis.available() :1024];
            while(fis.available() > 0) {
                fis.read(b);
                bos.write(b);
                b = new byte[1024 > fis.available() ? fis.available() :1024];
            }
            fis.close();
            bos.close();
            byte[] data = bos.toByteArray();
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static boolean ByteArray2File(byte[] data, String fileName) throws IOException {
        if(data == null)
            return false;

        File file = new File(fileName);
        if(!file.isFile()) {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return true;
        }
        else{
            return false;
        }
    }

}
