package server;

import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * Created by shelfee on 2016/12/11.
 */
public interface center_service extends Remote {

    public int processLogin(String serverHost) throws RemoteException;
    public boolean addNode(String hostip, String keep, String service) throws RemoteException;
    public long addFile(String fileName, byte[]data) throws RemoteException;
    public boolean removeFile(String fileName) throws RemoteException;
    public long checkFile(String fileName) throws RemoteException;
    public byte[] getFile(String fileName) throws RemoteException;
    public String allocate() throws RemoteException;
    public boolean synchronizeNode(data_server_keeper keeper) throws RemoteException;
    public void openNode() throws RemoteException;
    public boolean closeNode(String ip) throws RemoteException;
    public boolean alive(String hostip, String keep, String service) throws RemoteException;
}
