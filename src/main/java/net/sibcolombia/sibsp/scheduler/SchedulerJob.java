package net.sibcolombia.sibsp.scheduler;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import net.sibcolombia.sibsp.configuration.DataDir;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class SchedulerJob implements Job {

  private static Logger log = Logger.getLogger(SchedulerJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      DataDir dataDirectory = (DataDir) context.getJobDetail().getJobDataMap().get("setupFolder");
      File resourceFolder = new File(dataDirectory.getDataDir(), DataDir.getResourcesDir());
      if (resourceFolder.isDirectory()) {
        File[] children = resourceFolder.listFiles();
        if (children != null) {
          long currentMilliseconds = new Date().getTime();
          Date fileDate;
          long diffDays;
          for (File child : children) {
            fileDate = new Date(child.lastModified());
            diffDays = (currentMilliseconds - fileDate.getTime()) / (24 * 60 * 60 * 1000);
            if (diffDays >= 7) {
              FileUtils.deleteDirectory(child);
            }
          }
        }
      }
      log.info("Resource folder has been cleaned.");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error("Folder cleaning error deleting folder: " + e);
    }
  }
}
