package server;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by shelfee on 2016/12/12.
 */
public class data_server_keeperImpl  extends UnicastRemoteObject implements data_server_keeper {
    public static data_server_keeper instance;
    public static final Object lock = new Object();
    protected data_server_keeperImpl() throws RemoteException {
        super();
    }

    public boolean alive() throws RemoteException {
        return true;
    }

    public boolean addFile(String fileName, byte[]data, long timestamp) throws RemoteException {
        try {
            util.file_byte.ByteArray2File(data, data_server.rootpath + fileName);
            File file = new File(data_server.rootpath + fileName);
            //System.out.println(file.getName());
            file.setLastModified(timestamp);
            //data_server.FileSign.put(fileName, timestamp);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeFile(String fileName) throws RemoteException {
        File file = new File( data_server.rootpath + fileName);
        if(file.isFile()) {
            file.delete();
            return true;
        }
        return false;
    }

    public HashMap<String, Long> reportFiles() throws RemoteException {
        File root = new File(data_server.rootpath);
        File[]files = root.listFiles();
        HashMap<String, Long>report = new HashMap<String, Long>();
        for(File file: files){
            report.put(file.getName(), file.lastModified());
        }
        return report;
    }

    public boolean closeNode() throws RemoteException {
        synchronized (lock){
            if(data_server.clients.isEmpty()) {
                data_server.close = true;
                return true;
            }
        }
        return false;
    }

    public int serverLoad() throws RemoteException {
        return data_server.clients.entrySet().size();
    }

    public static data_server_keeper getInstance()throws RemoteException{
        if (null == instance){
            synchronized (lock){
                instance = new data_server_keeperImpl();
            }
        }
        return instance;
    }
}
