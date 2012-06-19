package org.bitrepository.integrityservice.scheduler;

import java.util.Date;
import java.util.TimerTask;

import org.bitrepository.integrityservice.scheduler.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntervalBasedWorkflowTask extends TimerTask implements WorkflowTask {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());
    /** The date for the next run of the workflow.*/
    private Date nextRun;
    /** The name of the workflow.*/
    private final String name;
    /** The interval between triggers. */
    private final long interval;
    
    private final Workflow workflow;

    /**
     * Initialise trigger.
     * @param interval The interval between triggering events in milliseconds.
     * @param name The name of this workflow.
     */
    public IntervalBasedWorkflowTask(long interval, String name, Workflow workflow) {
        this.interval = interval;
        this.name = name;
        this.workflow = workflow;
        nextRun = new Date();
    }
    
    @Override
    public Date getNextRun() {
        return new Date(nextRun.getTime());
    }
    
    @Override
    public long getTimeBetweenRuns() {
        return interval;
    }
    
    @Override
    public void trigger() {
        log.info("Starting the workflow: " + getName());
        nextRun = new Date(System.currentTimeMillis() + interval);
        workflow.start();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String currentState() {
        return workflow.currentState();
    }

    @Override
    public void run() {
        if(getNextRun().getTime() <= System.currentTimeMillis()) {
           trigger();
        }
    }
}