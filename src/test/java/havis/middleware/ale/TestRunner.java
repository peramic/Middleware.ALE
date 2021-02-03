package havis.middleware.ale;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Test runner to execute all JUnit4 tests in the class path
 * 
 * JUnit4 provides no dynamic way of executing all tests from ant, hence this
 * class was created.
 */
public final class TestRunner {

	public static void main(String[] args) throws Exception {
		JUnitCore junit = new JUnitCore();
		junit.addListener(new RunListener() {

			private long start = 0;
			private List<String> ignoredTests = new ArrayList<>();
			private Class<?> lastTestClass = null;
			private boolean lastFailed = false;
			private Class<?> lastSkippedTestClass = null;

			@Override
			public void testStarted(Description description) throws Exception {
				if (lastTestClass != description.getTestClass()) {
					printTestClassSummary();
					lastTestClass = description.getTestClass();
					lastFailed = false;
					start = System.currentTimeMillis();
					System.out.println("JUnit: " + description.getTestClass().getName() + " ...");
				}
			}

			private void printTestClassSummary() {
				if (lastTestClass != null) {
					System.out.println("JUnit: " + lastTestClass.getName() + " " + (lastFailed ? "failed" : "passed") + " ("
							+ ((System.currentTimeMillis() - start) / 1000.0) + "s)");
				}
			}

			@Override
			public void testAssumptionFailure(Failure failure) {
				lastFailed = true;

				System.err.println("JUnit: " + failure.getDescription().getTestClass().getName() + "." + failure.getDescription().getMethodName() + "() failed"
						+ (failure.getMessage() != null ? ": " + failure.getMessage() : ""));
				if (failure.getException() != null) {
					failure.getException().printStackTrace(System.err);
				}
			}

			@Override
			public void testFailure(Failure failure) throws Exception {
				testAssumptionFailure(failure);
			}

			@Override
			public void testIgnored(Description description) throws Exception {
				if (lastSkippedTestClass != description.getTestClass()) {
					lastSkippedTestClass = description.getTestClass();
					ignoredTests.add(description.getTestClass().getName());
				}
			}

			@Override
			public void testRunFinished(Result result) throws Exception {
				// print the summary for the last test class
				printTestClassSummary();

				// now print the test run summary
				System.out.println(" ");
				System.out.println("JUnit: Test cases run: " + result.getRunCount() + ", Skipped: " + result.getIgnoreCount() + ", Failed: "
						+ result.getFailureCount());
				if (result.getFailureCount() > 0) {
					System.out.print("JUnit: Failed test classes: ");
					String lastFailure = null;
					for (Failure failure : result.getFailures()) {
						if (!failure.getDescription().getTestClass().getName().equals(lastFailure)) {
							if (lastFailure != null) {
								System.out.print(", ");
							}
							lastFailure = failure.getDescription().getTestClass().getName();
							System.out.print(lastFailure);
						}
					}
					System.out.println();
				}
				if (ignoredTests.size() > 0) {
					System.out.print("JUnit: Skipped test classes: ");
					boolean first = true;
					for (String ignoredTest : ignoredTests) {
						if (first) {
							first = false;
						} else {
							System.out.print(", ");
						}
						System.out.print(ignoredTest);
					}
					System.out.println();
				}

				System.out.println("JUnit: Total run time: " + result.getRunTime() / 1000.0 + "s");
				System.out.println(" ");
			}

		});
		Result result = junit.run(findTestClasses().toArray(new Class[0]));

		// exit with the failure count; 0 = normal exit
		System.exit(result.getFailureCount());
	}

	private static List<Class<?>> findTestClasses() throws Exception {
		List<Class<?>> testClasses = new ArrayList<>();
		Enumeration<URL> resources = TestRunner.class.getClassLoader().getResources("");
		while (resources != null && resources.hasMoreElements()) {
			testClasses.addAll(findTestClasses(new File(resources.nextElement().getFile())));
		}
		return testClasses;
	}

	private static List<Class<?>> findTestClasses(File directory) throws Exception {
		List<Class<?>> testClasses = new ArrayList<>();
		if (directory.exists()) {
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					testClasses.addAll(findTestClasses(file));
				} else if (file.getName().endsWith("Test.class")) {
					Class<?> testClass = Class.forName(getFullyQualifiedClassName(file));
					if (containsTests(testClass)) {
						testClasses.add(testClass);
					}
				}
			}
		}
		return testClasses;
	}

	private static boolean containsTests(Class<?> testClass) {
		for (Method method : testClass.getMethods()) {
			if (method.isAnnotationPresent(Test.class)) {
				return true;
			}
		}
		return false;
	}

	private static String getFullyQualifiedClassName(File file) {
		String packageName = TestRunner.class.getPackage().getName().replace('.', File.separatorChar);
		String fq = file.getAbsolutePath();
		// strip file system path
		fq = fq.substring(fq.lastIndexOf(packageName));
		// strip extension
		fq = fq.substring(0, fq.length() - 6);
		// change to dot notation
		fq = fq.replace(File.separatorChar, '.');
		return fq;
	}
}
