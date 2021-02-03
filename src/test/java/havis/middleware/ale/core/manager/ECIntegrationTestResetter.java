package havis.middleware.ale.core.manager;

import havis.middleware.ale.core.depot.service.ec.EventCycleResetter;

/**
 * Class to reset the EC manager
 */
public class ECIntegrationTestResetter {
    public static void reset() {
        EventCycleResetter.reset();
        EC.getInstance().dispose();
        LR.reset();
        LRIntegrationTestHelper.manager = havis.middleware.ale.core.manager.LR.getInstance();
    }
}
