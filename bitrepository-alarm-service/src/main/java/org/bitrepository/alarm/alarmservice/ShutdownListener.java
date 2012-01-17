package org.bitrepository.alarm.alarmservice;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.bitrepository.alarm.AlarmStore;
import org.bitrepository.alarm.AlarmStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Listener has two intentions
 * 1) Acquire necessary information at startup to locate configuration files and create the first instance 
 * 		of the basic client, so everything is setup before the first users start using the webservice. 
 * 2) In time shut the service down in a proper manner, so no threads will be orphaned.   
 */
public class ShutdownListener implements ServletContextListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Do initialization work  
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("Initializing servlet context");
        String confDir = sce.getServletContext().getInitParameter("alarmServiceConfDir");
        if(confDir == null) {
        	throw new RuntimeException("No configuration directory specified!");
        }
        log.debug("Configuration dir = " + confDir);
        AlarmStoreFactory.init(confDir);
        AlarmStore alarmStore = AlarmStoreFactory.getInstance();
        log.debug("Servlet context initialized");
    }

    /**
     * Do teardown work. 
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        AlarmStore alarmStore = AlarmStoreFactory.getInstance();
        alarmStore.shutdown(); 
        log.debug("Servlet context destroyed");
    }

}