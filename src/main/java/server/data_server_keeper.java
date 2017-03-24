package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Created by shelfee on 2016/12/12.
 */
public interface data_server_keeper extends Remote {

    public boolean alive() throws RemoteException;
    public boolean addFile(String fileName, byte[]data, long time) throws RemoteException;
    public boolean removeFile(String fileName) throws RemoteException;
    public HashMap<String, Long> reportFiles() throws RemoteException;
    public boolean closeNode() throws RemoteException;
    public int serverLoad() throws RemoteException;
}