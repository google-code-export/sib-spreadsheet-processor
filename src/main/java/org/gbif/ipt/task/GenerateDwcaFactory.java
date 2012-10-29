package org.gbif.ipt.task;

import net.sibcolombia.sibsp.model.Resource;


public interface GenerateDwcaFactory {

  GenerateDwca create(Resource resource, ReportHandler handler);
}