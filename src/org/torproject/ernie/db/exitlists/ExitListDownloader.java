/* Copyright 2010--2012 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.ernie.db.exitlists;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.torproject.ernie.db.main.Configuration;

public class ExitListDownloader extends Thread {

  public ExitListDownloader(Configuration config) {
  }

  public void run() {

    if (((System.currentTimeMillis() / 60000L) % 60L) > 30L) {
      /* Don't start in second half of an hour, when we only want to
       * process other data. */
      return;
    }

    Logger logger = Logger.getLogger(ExitListDownloader.class.getName());
    try {
      logger.fine("Downloading exit list...");
      String exitAddressesUrl =
          "http://exitlist.torproject.org/exit-addresses";
      URL u = new URL(exitAddressesUrl);
      HttpURLConnection huc = (HttpURLConnection) u.openConnection();
      huc.setRequestMethod("GET");
      huc.connect();
      int response = huc.getResponseCode();
      if (response != 200) {
        logger.warning("Could not download exit list. Response code " + 
            response);
        return;
      }
      BufferedInputStream in = new BufferedInputStream(
          huc.getInputStream());
      SimpleDateFormat printFormat =
          new SimpleDateFormat("yyyy/MM/dd/yyyy-MM-dd-HH-mm-ss");
      printFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date downloadedDate = new Date();
      File tarballFile = new File("exitlist/" + printFormat.format(
          downloadedDate));
      tarballFile.getParentFile().mkdirs();
      File rsyncFile = new File("rsync/exit-lists/"
          + tarballFile.getName());
      rsyncFile.getParentFile().mkdirs();
      SimpleDateFormat dateTimeFormat =
          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      BufferedWriter bwT = new BufferedWriter(new FileWriter(
          tarballFile));
      BufferedWriter bwR = new BufferedWriter(new FileWriter(
          rsyncFile));
      bwT.write("@type tordnsel 1.0\n");
      bwT.write("Downloaded " + dateTimeFormat.format(downloadedDate)
          + "\n");
      bwR.write("@type tordnsel 1.0\n");
      bwR.write("Downloaded " + dateTimeFormat.format(downloadedDate)
          + "\n");
      int len;
      byte[] data = new byte[1024];
      while ((len = in.read(data, 0, 1024)) >= 0) {
        bwT.write(new String(data, 0, len));
        bwR.write(new String(data, 0, len));
      }   
      in.close();
      bwT.close();
      bwR.close();
      logger.fine("Finished downloading exit list.");
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed downloading exit list", e);
      return;
    }

    /* Write stats. */
    StringBuilder dumpStats = new StringBuilder("Finished downloading "
        + "exit list.\nLast three exit lists are:");
    Stack<File> filesInInputDir = new Stack<File>();
    filesInInputDir.add(new File("exitlist"));
    SortedSet<File> lastThreeExitLists = new TreeSet<File>();
    while (!filesInInputDir.isEmpty()) {
      File pop = filesInInputDir.pop();
      if (pop.isDirectory()) {
        SortedSet<File> lastThreeElements = new TreeSet<File>();
        for (File f : pop.listFiles()) {
          lastThreeElements.add(f);
        }
        while (lastThreeElements.size() > 3) {
          lastThreeElements.remove(lastThreeElements.first());
        }
        for (File f : lastThreeElements) {
          filesInInputDir.add(f);
        }
      } else {
        lastThreeExitLists.add(pop);
        while (lastThreeExitLists.size() > 3) {
          lastThreeExitLists.remove(lastThreeExitLists.first());
        }
      }
    }
    for (File f : lastThreeExitLists) {
      dumpStats.append("\n" + f.getName());
    }
    logger.info(dumpStats.toString());

    this.cleanUpRsyncDirectory();
  }

  /* Delete all files from the rsync directory that have not been modified
   * in the last three days. */
  public void cleanUpRsyncDirectory() {
    long cutOffMillis = System.currentTimeMillis()
        - 3L * 24L * 60L * 60L * 1000L;
    Stack<File> allFiles = new Stack<File>();
    allFiles.add(new File("rsync/exit-lists"));
    while (!allFiles.isEmpty()) {
      File file = allFiles.pop();
      if (file.isDirectory()) {
        allFiles.addAll(Arrays.asList(file.listFiles()));
      } else if (file.lastModified() < cutOffMillis) {
        file.delete();
      }
    }
  }
}
