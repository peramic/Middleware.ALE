package havis.middleware.ale.core.report;

/**
 * Provides a interface for enqueuing
 * 
 * @author abrams
 * 
 * @param <Reports>
 *            Reports type
 * @param <Datas>
 *            Datas type
 */
public interface IReports<Reports, Datas extends IDatas> {

	/**
	 * Enqueues a reports info object
	 * 
	 * @param reports
	 *            The reports info object
	 */
	void enqueue(ReportsInfo<Reports, Datas> reports);

	/**
	 * Disposes all reports
	 */
	void dispose();
}
