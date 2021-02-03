package havis.middleware.ale.core.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implement the web trigger service
 */
public class HttpTriggerService {

	private static HttpTriggerService instance;
	static {
		reset();
	}

	public static void reset() {
		instance = new HttpTriggerService();
	}

	protected Map<String, List<HttpTrigger>> triggers = new HashMap<String, List<HttpTrigger>>();

	public static HttpTriggerService getInstance() {
		return instance;
	}

	/**
	 * Creates a new instance
	 */
	HttpTriggerService() {
	}

	/**
	 * Adds a new web trigger
	 * 
	 * @param trigger
	 *            The trigger
	 */
	void add(HttpTrigger trigger) {
		List<HttpTrigger> list = this.triggers.get(trigger.getName());
		if (list != null) {
			list.add(trigger);
		} else {
			list = new ArrayList<HttpTrigger>();
			list.add(trigger);
			this.triggers.put(trigger.getName(), list);
		}
	}

	/**
	 * Removes the web trigger
	 * 
	 * @param trigger
	 *            The trigger
	 */
	void remove(HttpTrigger trigger) {
		List<HttpTrigger> list = this.triggers.get(trigger.getName());
		if (list != null) {
			Iterator<HttpTrigger> iterator = list.iterator();
			while (iterator.hasNext()) {
				// remove the exact instance only
				if (iterator.next() == trigger) {
					iterator.remove();
					break;
				}
			}
			if (list.size() == 0)
				this.triggers.remove(trigger.getName());
		}
	}

	/**
	 * Handle a trigger request
	 * 
	 * @param name
	 *            the trigger name
	 * @return true if a trigger was found, false otherwise
	 */
	public boolean handle(String name) {
		List<HttpTrigger> list = this.triggers.get(name);
		if (list != null) {
			Set<HttpTrigger> invoked = new HashSet<>();
			for (HttpTrigger t : list) {
				// Make sure we trigger equal instances only once, we try
				// triggering all triggers with the same URI that are
				// not equal (with a different creator ID).
				if (!invoked.contains(t)) {
					if (t.invoke()) {
						invoked.add(t);
					}
				}
			}
			return true;
		}
		return false;
	}
}