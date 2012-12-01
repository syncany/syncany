/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.watch.remote;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;

import org.syncany.watch.remote.files.UpdateFile;
import org.syncany.db.CloneClient;
import org.syncany.db.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import org.syncany.config.Repository;
import org.syncany.config.Settings;
import org.syncany.db.CloneFile.Status;
import org.syncany.util.StringUtil;

/**
 * Encapsules the updates grouped by clients.
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UpdateQueue {
    private static final Logger logger = Logger.getLogger(UpdateQueue.class.getSimpleName());
    private static final boolean DEBUG = true;
    
    private Map<CloneClient, UpdateFile> remoteUpdateFiles;
    
    private Map<Long, FileHistory> generatedResultMap;
    private FileHistoryComparator fileHistoryComparator;

    public UpdateQueue() {
        this.remoteUpdateFiles = new HashMap<CloneClient, UpdateFile>();
        this.fileHistoryComparator = new FileHistoryComparator();
    }

    public void setLocalUpdateFile(UpdateFile updateFile) {
    }
    
    public void addRemoteUpdateFile(CloneClient client, UpdateFile updateFile) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Adding update file to temp. DB [{0}, {1}] ...", new Object[]{client.getMachineName(), updateFile.getName()});
        }

        // Add to list (for the record)
        remoteUpdateFiles.put(client, updateFile);
    }

    /**
     * Looks at all the available remote update files and generates
     * a single update list. The generated list has correct dependencies (causally).
     * @return
     */
    public PriorityQueue<FileHistory> getQueue() {
        if (generatedResultMap != null) {
            return createQueue(generatedResultMap);
        }
        
        Map<Long, FileHistory> resultHistories = new HashMap<Long, FileHistory>();
        
        // STEP 1: create file histories for all file IDs
        //         - detects file ID conflicts and resolves them
        //         - filename conflicts are resovled in step 2
        
        // Add the local update file first
        for (FileHistory history : Database.getInstance().getFileHistories()) {
            System.err.println("set last local for "+ history.getFileId() + " : " + history.getLastUpdate());
            history.setLastLocalUpdate(history.getLastUpdate());
            resultHistories.put(history.getFileId(), history);
        }        
        
        // Make file histories
        // - loop* means client-specific update and client specific histories
        // - result* means update/history in the resultHistories map (= final queue)
        for (Map.Entry<CloneClient, UpdateFile> e : remoteUpdateFiles.entrySet()) {        
            UpdateFile thisUpdateFile = e.getValue();
            
            files:
            for (Long fileId : thisUpdateFile.getFileIds()) {
                FileHistory loopFileHistory = thisUpdateFile.getFileUpdates(fileId); // a file history in the update file of this client
                FileHistory resultFileHistory = resultHistories.get(fileId); // file history of the same file (if already in list) 
                
                // No history for this file ID exists so far; just add it
                if (resultFileHistory == null) {
                    resultHistories.put(fileId, loopFileHistory);
                    continue files;
                }
                
                // History already exists; check for conflicts
                else {
                    // Check every update in the history
                	versions2:
                	for (FileUpdate loopUpdate : loopFileHistory.getHistory().values()) {
                		FileUpdate resultUpdate = resultFileHistory.get(loopUpdate.getVersion());
                		if (DEBUG) System.err.println("resultUpdate: "+resultUpdate+"\nloopUpdate: "+loopUpdate);
                        
                	
                	}
                	
                	versions:
                    for (FileUpdate resultUpdate : resultFileHistory.getHistory().values()) {
                        FileUpdate loopUpdate = loopFileHistory.get(resultUpdate.getVersion());
                        if (DEBUG) System.err.println("resultUpdate: "+resultUpdate+"\nloopUpdate: "+loopUpdate);
                        
                        // "loop"-history does not include the "result"-update
                        // Seems that the result history is newer than this client's one
                        if (loopUpdate == null) {
                            if (DEBUG) System.err.println("--> loopUpd null");
                            continue files;
                        }     
                                
                        // Result update exists; now check if they are identical
                        else {
                            // Updates are identical -> skip
                            if (resultUpdate.equals(loopUpdate)) {
                                if (DEBUG) System.err.println("--> identical");                                
                                continue versions;
                            }   
                            
                            // They are NOT identical; conflict!
                            else {
                                if (DEBUG) System.err.println("--> NOT identical");
                                // The "result"-history wins (= could be my history or one from 
                                // another client); now check if the "loop"-history is mine
                                // - if it is, this is a local CONFLICT -> attach "loop"-history to "result".
                                // - if not, just ignore the result history
                                if (resultUpdate.getUpdated().before(loopUpdate.getUpdated())) {                                   
                                    if (DEBUG) System.err.println("--> resultUpd wins");
                                    
                                    if (loopFileHistory.getMachineName().equals(Settings.getInstance().getMachineName())) {                                        
                                        if (DEBUG) System.err.println("--> loopHistory is LOCAL -> branching new file");
                                        
                                        // Add a branch as new file
                                        FileHistory branchedLoopFileHistory = loopFileHistory.branch();
                                        resultHistories.put(branchedLoopFileHistory.getFileId(), branchedLoopFileHistory);                                           
                                        
                                        // Add prune history and link branch history
                                        resultFileHistory.setPruneHistory(loopFileHistory);
                                        resultFileHistory.setBranchHistory(branchedLoopFileHistory);                                        
                                    }
                                    
                                    continue files;
                                }
                                
                                // The "loop"-history wins; we need to make the "loop"-history to the
                                // new "result"-history; but before we do that, let's check if the "result"-history is mine
                                // - if it is, this is a local CONFLICT --> attach my history ("result") to the 
                                //   "loop"-history and then make "loop" the new "result" (wow, that sounds confusing!)
                                // - if not, transfer the "conflicting" version from the current "result" history to the
                                //   current "loop" (might be null, might be the local history) first
                                else {  
                                    if (DEBUG) System.err.println("--> loopUpd wins");
                                    
                                    if (resultFileHistory.getMachineName().equals(Settings.getInstance().getMachineName())) {
                                        if (DEBUG) System.err.println("--> resultHistory is LOCAL -> branching new file");
                                        // Add a branch as new file
                                        FileHistory branchedResultFileHistory = resultFileHistory.branch();
                                        resultHistories.put(branchedResultFileHistory.getFileId(), branchedResultFileHistory);                                                                                   
                                        
                                        // Add prune history and link branch history
                                        loopFileHistory.setPruneHistory(resultFileHistory);
                                        loopFileHistory.setBranchHistory(branchedResultFileHistory);                                                                                
                                    }
                                    
                                    else {
                                        // This happens if the local client lost two times over different clients.
                                        // We must now transfer the conflict histories between the objects
                                        
                                        if (DEBUG) System.err.println("--> resultHistory is __NOT__ LOCAL -> transfer conflict history to loop");
                                        
                                        loopFileHistory.setPruneHistory(resultFileHistory.getPruneHistory());
                                        loopFileHistory.setBranchHistory(resultFileHistory.getBranchHistory());
                                    }
                                    
                                    // Update last local version
                                    if (DEBUG) System.err.println("--> Current last local result history upd: "+resultFileHistory.getLastLocalUpdate());
                                    
                                    if (resultFileHistory.getLastLocalUpdate() != null &&
                                            resultFileHistory.getLastLocalUpdate().getVersion() >= loopUpdate.getVersion()) {
                                        
                                        loopFileHistory.setLastLocalUpdate(resultFileHistory.get(loopUpdate.getVersion()-1));    
                                        resultFileHistory.setLastLocalUpdate(resultFileHistory.get(loopUpdate.getVersion()-1));    
                                        if (DEBUG) System.err.println("--> Adjusted last local to: "+loopFileHistory.getLastLocalUpdate());
                                    }
                                    else {
                                        loopFileHistory.setLastLocalUpdate(resultFileHistory.getLastLocalUpdate());    
                                    }
                                    
                                    // Add to result list
                                    resultHistories.put(fileId, loopFileHistory);
                                    continue files;
                                }
                            }
                        }                        
                    }
                    
                    // If we reached this point, that means that the histories have
                    // been identical so far. The only thing left to check is whether
                    // the "loop"-history is longer.
                    // - if it is, it has NEW entries -> make it the new "result"
                    // - if not, they are of the same length (or the "result" one is longer) -> do nothing
                    
                    if (loopFileHistory.getHistory().size() > resultFileHistory.getHistory().size()) {
                        // Copy last local
                        loopFileHistory.setLastLocalUpdate(resultFileHistory.getLastLocalUpdate());
                        
                        resultHistories.put(fileId, loopFileHistory);
                        continue files;
                    }                
                    
                    // Done!
                    continue files;
                }                
            }
        }
        
        
        // STEP 2: detect filename conflicts and resolve them        
        // TODO This has O(n^2) runtime; can this be done more efficiently?
        if (DEBUG) System.err.println("Now do filename clash check ...");
        
        for (FileHistory outerHistory : resultHistories.values()) {
            long outerFileId = outerHistory.getFileId();
            FileUpdate outerLastUpdate = outerHistory.getLastUpdate();
            
            inner:
            for (FileHistory innerHistory : resultHistories.values()) {                
                long innerFileId = innerHistory.getFileId();
                FileUpdate innerLastUpdate = innerHistory.getLastUpdate();
             
                // Same file ID; skip!
                if (innerFileId == outerFileId) {
                    continue inner;
                }
                
                // One of them is MERGED or DELETED; skip!
                if (innerLastUpdate.getStatus() == Status.MERGED || innerLastUpdate.getStatus() == Status.DELETED
                        || outerLastUpdate.getStatus() == Status.MERGED || outerLastUpdate.getStatus() == Status.DELETED) {
                    
                    continue inner;
                }
                
                // Not the same filename; skip!
                if (!innerLastUpdate.getName().equals(outerLastUpdate.getName())
                        || !innerLastUpdate.getPath().equals(outerLastUpdate.getPath())) {
                    
                    continue inner;
                }                
                
                // Filename conflict between innerFileId and outerFileId
                // Now detect:
                // - if it's gonna be merged (same checksum)
                // - who wins (= the one with the older last update)
                if (DEBUG) System.err.println("- CLASH between "+innerLastUpdate.getFileId()+" v"+innerLastUpdate.getVersion()+" and "+outerLastUpdate.getFileId()+" v"+outerLastUpdate.getVersion()+"; Filename: "+innerLastUpdate.getPath()+"/"+innerLastUpdate.getName());
                        
                boolean innerWins = innerLastUpdate.getUpdated().before(outerLastUpdate.getUpdated());
                boolean sameChecksum = Arrays.equals(innerLastUpdate.getChecksum(), outerLastUpdate.getChecksum());
                
                // Files have same checksum: merge them
                if (sameChecksum) {
                    if (innerWins) {
                        if (DEBUG) System.err.println("--> Same checksum; Winner: "+innerLastUpdate.getFileId()+" v"+innerLastUpdate.getVersion()+"; Will be merged: "+outerLastUpdate.getFileId()+" v"+outerLastUpdate.getVersion());
                        
                        FileUpdate outerNewMergeUpdate = (FileUpdate) outerLastUpdate.clone();
                        
                        outerNewMergeUpdate.setVersion(outerLastUpdate.getVersion()+1);
                        outerNewMergeUpdate.setStatus(Status.MERGED);
                        outerNewMergeUpdate.setMergedRootId(innerLastUpdate.getMergedRootId());
                        outerNewMergeUpdate.setMergedFileId(innerLastUpdate.getFileId());
                        outerNewMergeUpdate.setMergedFileVersion(innerLastUpdate.getVersion());
                        
                        outerHistory.add(outerNewMergeUpdate);
                    }
                    else {
                        if (DEBUG) System.err.println("--> Same checksum; Winner: "+outerLastUpdate.getFileId()+" v"+outerLastUpdate.getVersion()+"; Will be merged: "+innerLastUpdate.getFileId()+" v"+innerLastUpdate.getVersion());

                        FileUpdate innerNewMergeUpdate = (FileUpdate) innerLastUpdate.clone();
                        
                        innerNewMergeUpdate.setVersion(innerLastUpdate.getVersion()+1);
                        innerNewMergeUpdate.setStatus(Status.MERGED);
                        innerNewMergeUpdate.setMergedRootId(outerLastUpdate.getMergedRootId());
                        innerNewMergeUpdate.setMergedFileId(outerLastUpdate.getFileId());
                        innerNewMergeUpdate.setMergedFileVersion(outerLastUpdate.getVersion());
                        
                        innerHistory.add(innerNewMergeUpdate);                        
                    }
                }
                
                // Files have different checksums: conflict!
                else {
                    if (innerWins) {
                        if (DEBUG) System.err.println("--> CONFLICT: NOT the same checksum; Winner: "+innerLastUpdate.getFileId()+" v"+innerLastUpdate.getVersion()+"; Renaming to conflicted copy: "+outerLastUpdate.getFileId()+" v"+outerLastUpdate.getVersion());
                        
                        FileUpdate outerNewConflictUpdate = (FileUpdate) outerLastUpdate.clone();
                        
                        outerNewConflictUpdate.setName(outerLastUpdate.getConflictedCopyName());
                        outerNewConflictUpdate.setVersion(outerLastUpdate.getVersion()+1);
                        outerNewConflictUpdate.setStatus(Status.RENAMED);
                        // TODO setBranchedFrom(..)
                        
                        outerHistory.add(outerNewConflictUpdate);
                    }
                    
                    else {
                        if (DEBUG) System.err.println("--> CONFLICT: NOT the same checksum; Winner: "+outerLastUpdate.getFileId()+" v"+outerLastUpdate.getVersion()+"; Renaming to conflicted copy: "+innerLastUpdate.getFileId()+" v"+innerLastUpdate.getVersion());

                        FileUpdate innerNewConflictUpdate = (FileUpdate) innerLastUpdate.clone();
                        
                        innerNewConflictUpdate.setName(innerLastUpdate.getConflictedCopyName());
                        innerNewConflictUpdate.setVersion(innerLastUpdate.getVersion()+1);
                        innerNewConflictUpdate.setStatus(Status.RENAMED);
                        // TODO setBranchedFrom(..)
                        
                        innerHistory.add(innerNewConflictUpdate);                        
                    }
                }
            }                        
        }
        
        /// STEP 3: Remove the branched histories.        
        List<Long> removeIds = new ArrayList<Long>();
        
        for (FileHistory history : resultHistories.values()) {
            if (history.getBranchHistory() != null) {
                removeIds.add(history.getBranchHistory().getFileId());
            }
        }
        
        for (Long removeFileId : removeIds) {
            resultHistories.remove(removeFileId);
        }

        generatedResultMap = resultHistories;
        return createQueue(generatedResultMap);
    }
    
    private PriorityQueue<FileHistory> createQueue(Map<Long, FileHistory> resultMap) {
        PriorityQueue<FileHistory> resultQueue = new PriorityQueue<FileHistory>(11, fileHistoryComparator);
        
        for (FileHistory history : resultMap.values()) {
           resultQueue.add(history); 
        }
        
        return resultQueue;
    }    

    public Map<CloneClient, UpdateFile> getRemoteUpdateFiles() {
        return remoteUpdateFiles;
    }

    private static UpdateFile makeUpdateFileLocal(Repository repo) {
        UpdateFile uf = new UpdateFile(repo, Settings.getInstance().getMachineName(), new Date());                
        FileUpdate u;
        
        u = new FileUpdate(); 
        u.setFolder(true);
        u.setClientName("platop");
        u.setUpdated(new Date(10));
        u.setChecksum(new byte[] {1,1,1,2,2,2});
        u.setFileId(1);
        u.setVersion(1);    
        u.setPath("some/path");
        u.setName("file11.txt");           
        uf.add(u);        

        u = new FileUpdate(); 
        u.setFolder(true);
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,1,1,2,2,2});
        u.setUpdated(new Date(20));
        u.setFileId(1);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file12.txt");           
        uf.add(u);        

        u = new FileUpdate(); 
        u.setClientName("platop");
        u.setChecksum(new byte[] {3,3,3,4,4,4});        
        u.setUpdated(new Date(30));
        u.setFileId(9);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file91.txt");              
        uf.add(u);        
        
        u = new FileUpdate(); 
        u.setClientName("platop");
        u.setChecksum(new byte[] {3,3,3,4,4,4});        
        u.setUpdated(new Date(40));
        u.setFileId(9);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file92.txt");           
        uf.add(u);        

        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,2,3,4,5});        
        u.setUpdated(new Date(50));
        u.setFileId(3);
        u.setVersion(5);
        u.setPath("some/path");
        u.setName("file35.txt");         
        uf.add(u);        

        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,1,1,1,1,1,1});        
        u.setUpdated(new Date(60));
        u.setFileId(4);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file41.txt");                 
        uf.add(u);        

        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {2,2,2,2,2});        
        u.setUpdated(new Date(70));
        u.setFileId(4);
        u.setVersion(2);
        u.setPath("some/path");   // << conflicts with client2: file 7.2
        u.setName("CONFLICT.txt");         
        uf.add(u);        

        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,2,3,3,2,4});        
        u.setUpdated(new Date(80));
        u.setFileId(5);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file51.txt");                 
        uf.add(u);
        
        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,2,3,3,2,4});        
        u.setUpdated(new Date(90));
        u.setFileId(5);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file52.txt");                 
        uf.add(u);
        
        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {111,43});        
        u.setUpdated(new Date(90));
        u.setFileId(5);
        u.setVersion(3);
        u.setPath("some/path");
        u.setName("file53.txt");                 
        uf.add(u);   
        
        u = new FileUpdate();
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,1,1,1,1,1,1,1,1,1,1});        
        u.setUpdated(new Date(190));
        u.setFileId(10);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("CONFLICT-10.1-11.1");       // <<< filename conflict with 11.1            
        uf.add(u);           

        return uf;
    }
    
    private static UpdateFile makeUpdateFile1(Repository repo) {
        UpdateFile uf = new UpdateFile(repo, "client1", new Date());        
        FileUpdate u;
        
        u = new FileUpdate(); // Identical to local
        u.setFolder(true);
        u.setClientName("platop");
        u.setUpdated(new Date(10));
        u.setChecksum(new byte[] {1,1,1,2,2,2});
        u.setFileId(1);
        u.setVersion(1);    
        u.setPath("some/path");
        u.setName("file11.txt");           
        uf.add(u);        

        u = new FileUpdate(); // Conflicts to local+client2 --> wins over local, loses over client2
        u.setFolder(true);
        u.setClientName("client1");
        u.setChecksum(new byte[] {4,5,3,5,6,4,3,45});
        u.setUpdated(new Date(19)); // <<--- less than local, but more than client2
        u.setFileId(1);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file12.txt");          
        uf.add(u);        

        u = new FileUpdate(); // Identical to local
        u.setClientName("platop");
        u.setChecksum(new byte[] {3,3,3,4,4,4});        
        u.setUpdated(new Date(30));
        u.setFileId(9);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file91.txt");          
        uf.add(u);        
        
        u = new FileUpdate(); // Differs from local (-> loses)
        u.setClientName("client1");
        u.setChecksum(new byte[] {1,3,5,3,65,7,7,5,3,3}); // <<<---
        u.setUpdated(new Date(999999)); // <<---
        u.setFileId(9);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file92.txt");          
        uf.add(u);        

        u = new FileUpdate(); // Differs from local (-> wins)
        u.setClientName("client1");
        u.setChecksum(new byte[] {1,3,4,4,6,3,2,6});     // <<<---    
        u.setUpdated(new Date(000001)); // <<<---
        u.setFileId(3);
        u.setVersion(5);
        u.setPath("some/path");
        u.setName("file35.txt");          
        uf.add(u);        
        
        u = new FileUpdate(); // Does not exist in local version
        u.setClientName("client1");
        u.setChecksum(new byte[] {1,3,4,4,6,3,2,6});     // <<<---    
        u.setUpdated(new Date(000002)); // <<<---
        u.setFileId(3);
        u.setVersion(6);
        u.setPath("some/path");
        u.setName("file36.txt");                  
        uf.add(u);                

        u = new FileUpdate();// Identical to local
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,1,1,1,1,1,1});        
        u.setUpdated(new Date(60));
        u.setFileId(4);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file41.txt");                  
        uf.add(u);        

        u = new FileUpdate();// Identical to local
        u.setClientName("platop");
        u.setChecksum(new byte[] {2,2,2,2,2});        
        u.setUpdated(new Date(70));
        u.setFileId(4);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file42.txt");                  
        uf.add(u);     
        
        u = new FileUpdate(); /// NEW (compared to local)
        u.setClientName("client1");
        u.setChecksum(new byte[] {2,2,2,2,2});        
        u.setUpdated(new Date(70));
        u.setFileId(4);
        u.setVersion(3);
        u.setPath("some/path");
        u.setName("file43.txt");                  
        uf.add(u);         

        u = new FileUpdate();// Identical to local
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,2,3,3,2,4});        
        u.setUpdated(new Date(80));
        u.setFileId(5);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file51.txt");                  
        uf.add(u);
        
        u = new FileUpdate();// Identical to local
        u.setClientName("platop");
        u.setChecksum(new byte[] {1,2,3,3,2,4});        
        u.setUpdated(new Date(90));
        u.setFileId(5);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file52.txt");                  
        uf.add(u);
        
        //////////////////////////// Misses v53 (compared to local)        
        
        u = new FileUpdate();
        u.setClientName("client1");
        u.setChecksum(new byte[] {9,9,9,9,9,96,5,4,3,3,2});        
        u.setUpdated(new Date(180));
        u.setFileId(11);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("CONFLICT-10.1-11.1");       // <<< filename conflict with 10.1           
        uf.add(u);           
        
        return uf;
        
    }
    
    private static UpdateFile makeUpdateFile2(Repository repo) {
        UpdateFile uf = new UpdateFile(repo, "client2", new Date());               
        FileUpdate u;
        
        u = new FileUpdate(); // Identical to local
        u.setFolder(true);
        u.setClientName("platop");
        u.setUpdated(new Date(10));
        u.setChecksum(new byte[] {1,1,1,2,2,2});
        u.setFileId(1);
        u.setVersion(1);      
        u.setPath("some/path");
        u.setName("file11.txt");           
        uf.add(u);              
        
        u = new FileUpdate(); // Conflict with local (-> wins)
        u.setFolder(true);
        u.setClientName("client2");
        u.setChecksum(new byte[] {1,3,5,3,65,7,7,5,3,3}); // <<<<<<<------ changed
        u.setUpdated(new Date(00001));  // <<<<<<<------ changed
        u.setFileId(1);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("file12.txt");         
        uf.add(u);        
                
        u = new FileUpdate(); // NEW file
        u.setFolder(true);
        u.setClientName("client2");
        u.setUpdated(new Date(100));
        u.setChecksum(new byte[] {1,3,3,2,2,2,2,2,2});
        u.setFileId(8);
        u.setVersion(1);    
        u.setPath("short/path");
        u.setName("FOLDER");           
        uf.add(u);     
        
        u = new FileUpdate(); // NEW file
        u.setFolder(true);
        u.setClientName("client2");
        u.setUpdated(new Date(101));
        u.setChecksum(new byte[] {1,3,3,2,2,2,2,2,2});
        u.setFileId(8);
        u.setVersion(2);     
        u.setPath("some/very/very/very/long/path");
        u.setName("FOLDER");                
        uf.add(u);               
        
        u = new FileUpdate();// NEW for local+client1
        u.setClientName("client2");
        u.setChecksum(new byte[] {3,2,2,3,23,2,2,4,5,6,6});        
        u.setUpdated(new Date(110));
        u.setFileId(7);
        u.setVersion(1);
        u.setPath("some/path");
        u.setName("file71.txt");
        uf.add(u);    
        
        u = new FileUpdate();// file path conflict with v1.2
        u.setClientName("client2");
        u.setChecksum(new byte[] {3,2,2,3,23,2,2,4,5,6,6});        
        u.setUpdated(new Date(110));
        u.setFileId(7);
        u.setVersion(2);
        u.setPath("some/path");
        u.setName("CONFLICT.txt");        
        uf.add(u);          
        
        return uf;
        
    }    
    
    public void dump() {
        System.err.println("OUTPUT:");
        FileHistory history;
        
        PriorityQueue<FileHistory> q = getQueue();
        
        while (null != (history = q.poll())) {
            System.err.println("");
            System.err.println("");
            System.err.println("History of file id "+history.getFileId()+ " ("+history.getMachineName()+")");
            System.err.println("---------------------------------------");
            System.err.println("LAST LOCAL: "+history.getLastLocalUpdate());
            for (FileUpdate u : history.getHistory().values()) {
                System.err.println(u);    
            }     
            
            if (history.getPruneHistory() != null) {
                System.err.println("");
                System.err.println("  Prune history (by "+history.getPruneHistory().getMachineName()+"; to be deleted from DB):");
                System.err.println("  ---------------------------------------");
                System.err.println("  LAST LOCAL: "+history.getPruneHistory().getLastLocalUpdate());
                for (FileUpdate u : history.getPruneHistory().getHistory().values()) {
                    System.err.println("  "+u);       
                }                                 
            }
            else {
                System.err.println("  No prune history.");
            }
            
            if (history.getBranchHistory() != null) {
                System.err.println("");
                System.err.println("  Branch history (by "+history.getBranchHistory().getMachineName()+"; conflicting branch):");
                System.err.println("  ---------------------------------------");
                System.err.println("  LAST LOCAL: "+history.getBranchHistory().getLastLocalUpdate());
                for (FileUpdate u : history.getBranchHistory().getHistory().values()) {
                    System.err.println("  "+u);    
                }                                 
            }
            else {
                System.err.println("  No branch history.");
            }            
        }        
    }
    
    /**
     * Here are the rules:
     * 
     * Folders:
     *   * Folder histories to the end, because first we move all files,
     *     then the folders. This is important for DELETE requests.
     *   * For folders: Longer paths at the end. Also, this is important
     *     for DELETEs.
     * 
     * Files:
     *   * No rules.
     * 
     * Both:
     *   * Files with branches first. This is because 
     */
    private class FileHistoryComparator implements Comparator<FileHistory> {
        @Override
        public int compare(FileHistory h1, FileHistory h2) {
            // -1 = h1 to the front; 1 = h1 to the back
            FileUpdate u1 = h1.getLastUpdate();
            FileUpdate u2 = h2.getLastUpdate();
            
            // Both files
            if (!u1.isFolder() && !u2.isFolder()) {
                // TODO small files first? or alphabetically?
                return 0;
            }
            
            // Both folders
            if (u1.isFolder() && u2.isFolder()) {
                FileUpdate last1 = h1.getLastLocalUpdate();
                FileUpdate last2 = h2.getLastLocalUpdate();
                
                // If possible, compare the last known local updates
                if (last1 != null) {
                    u1 = last1;
                }
                
                if (last2 != null) {
                    u2 = last2;
                }
                
                // The one with the shortest parent comes to the BACK
                // because we don't want the long ones left behind when we
                // delete them!                
                if (StringUtil.count(u1.getPath(), File.separator) <
                        StringUtil.count(u2.getPath(), File.separator)) {
                    
                    return 1;
                }
                else {
                    return -1;
                }
            }
            
            // Folder and file
            if (u1.isFolder() && !u2.isFolder()) {
                return 1;
            }
            
            else {
                return -1;
            }
        }        
    }    
    
    public static UpdateFile readMaritop(Repository repo) throws IOException, ClassNotFoundException {
        UpdateFile uf = new UpdateFile(repo, "maritop", new Date());               
        uf.read(new File("/home/pheckel/Syncany/Repository/update-maritop-1308400932711"), false);               
        
        return uf;
    }
    
    public static UpdateFile readPlatop(Repository repo) throws IOException, ClassNotFoundException {
        UpdateFile uf = new UpdateFile(repo, "platop", new Date());               
        uf.read(new File("/home/pheckel/Syncany/Repository/update-platop-1308400908028"), false);               
        
        return uf;
    }

    public static void main(String[] a) throws IOException {
    	Settings.getInstance().setMachineName("platop");        
        
        Repository repo = new Repository();

        UpdateFile uf0 = makeUpdateFileLocal(repo);
        UpdateFile uf1 = makeUpdateFile1(repo);
        UpdateFile uf2 = makeUpdateFile2(repo);

        UpdateQueue ul = new UpdateQueue();

        ul.setLocalUpdateFile(uf0);
        ul.addRemoteUpdateFile(new CloneClient(uf1.getMachineName(), 1), uf1);
        ul.addRemoteUpdateFile(new CloneClient(uf2.getMachineName(), 1), uf2);

        ul.dump();
    }



   
}
