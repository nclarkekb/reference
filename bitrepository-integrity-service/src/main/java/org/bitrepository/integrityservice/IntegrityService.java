/*
 * #%L
 * Bitrepository Integrity Service
 * %%
 * Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.bitrepository.integrityservice;

import java.util.Collection;

import org.bitrepository.service.LifeCycledService;
import org.bitrepository.service.scheduler.Workflow;
import org.bitrepository.service.scheduler.WorkflowTask;

public interface IntegrityService extends LifeCycledService {
    /**
     * Retrieves all the scheduled tasks in the system, which are running.
     * @return The names of the tasks, which are scheduled by the system.
     */
    Collection<WorkflowTask> getScheduledWorkflows();
    
    /**
     * Retrieves all the available workflows, even those which have not been scheduled.
     * @return All the available workflows. 
     */
    Collection<Workflow> getAllWorkflows();
    
    /**
     * @param pillarId The pillar which has the files.
     * @return The number of files on the given pillar.
     */
    long getNumberOfFiles(String pillarId);
    
    /**
     * @param pillarId The pillar which might be missing some files.
     * @return The number of files missing for the given pillar.
     */
    long getNumberOfMissingFiles(String pillarId);
    
    /**
     * @param pillarId The pillar which might contain files with checksum error.
     * @return The number of files with checksum error at the given pillar.
     */
    long getNumberOfChecksumErrors(String pillarId);

    /**
     * Initiates the scheduling of a workflow.
     * @param workflow The workflow to schedule.
     * @param intervalBetweenRuns The time between running the workflow.
     */
    void scheduleWorkflow(Workflow workflow, long timeBetweenRuns);
    
    /**
     * Shut down the integrity service.
     */
    void shutdown();
}
