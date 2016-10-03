package uk.ac.lancs.ucrel.ops;

import uk.ac.lancs.ucrel.ds.Collocate;

import java.rmi.RemoteException;
import java.util.List;

public interface CollocateOperation extends Operation {
    void search(String[] searchTerms, int contextLeft, int contextRight, int pageLength, boolean reverseOrder) throws RemoteException;

    List<Collocate> it() throws RemoteException;

    int getLength() throws RemoteException;

    long getTime() throws RemoteException;
}
