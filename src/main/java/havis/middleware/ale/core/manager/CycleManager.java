package havis.middleware.ale.core.manager;

import havis.middleware.ale.core.cycle.CommonCycle;
import havis.middleware.ale.exit.Exits;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base cycle manager
 * 
 * @param <T>
 *            the type of cycle to manage
 */
public class CycleManager<T extends CommonCycle<?, ?, ?, ?, ?>> {

	protected Lock lock = new ReentrantLock();
	protected Map<String, T> cycles;

	private Lock volatileCyclesLock = new ReentrantLock();
	private List<T> volatileCycles;

	protected CycleManager() {
		super();
		cycles = new LinkedHashMap<>();
		volatileCycles = new ArrayList<>();
	}

	protected void addVolatileCycle(T cycle) {
		volatileCyclesLock.lock();
		try {
			volatileCycles.add(cycle);
		} finally {
			volatileCyclesLock.unlock();
		}
	}

	protected void removeVolatileCycle(T cycle) {
		volatileCyclesLock.lock();
		try {
			volatileCycles.remove(cycle);
		} finally {
			volatileCyclesLock.unlock();
		}
	}

	protected void disposeVolatileCycles() {
		volatileCyclesLock.lock();
		try {
			Iterator<T> it = volatileCycles.iterator();
			while (it.hasNext()) {
				try {
					it.next().dispose();
				} catch (Exception e) {
					Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Failed to dispose cycle: " + e.getMessage(), e);
				}
				it.remove();
			}
		} finally {
			volatileCyclesLock.unlock();
		}
	}

	/**
	 * Disposes instance
	 */
	public void dispose() {
		disposeVolatileCycles();
		lock.lock();
		try {
			for (Entry<String, T> pair : cycles.entrySet()) {
				try {
					pair.getValue().dispose();
				} catch (Exception e) {
					Exits.Log.logp(Exits.Level.Error, Exits.Common.Name, Exits.Common.Error, "Failed to dispose cycle: " + e.getMessage(), e);
				}
			}
			cycles.clear();
		} finally {
			lock.unlock();
		}
	}
}