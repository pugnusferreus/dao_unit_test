package com.progriff.util;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * This class manages Hibernate sessions correctly.
 * <p>
 * Note that currentSession and closeSession MUST ALWAYS be used within a try/finally clause to ensure sessions are closed correctly.
 * <p>
 * e.g.<br>
 * Session session = HibernateUtils.currentSession();<br>
 * try {<br>
 * // do work here<br>
 * }<br>
 * finally {<br>
 * HibernateUtils.closeSession();<br>
 * }
 * <p>
 * The benefit of HibernateUtils is that if we want a session to be re-used across multiple DAO operations we can do so without having do change any
 * of the DAO code (i.e. using the recommended session-per-request strategy rather than the non-recommended session-per-operation strategy).
 * <p>
 * This works by keeping track of how many times a nested currentSession and closeSession has been called, and using a depth counter to keep track of
 * the level of nesting.
 * <p>
 * A good example is for a web application we can define a Filter to open and close a hibernate session. That way the session is open for the entire
 * view-rendering process, which allows us to lazy-load our domain model throughout the JSP rendering.
 * <p>
 * If we remove the filter, each DAO operation will simply open and close a session for each operation which is fine, just less efficient and not
 * recommended.
 * <p>
 * e.g.<br>
 * FILTER<br>
 * doFilter(...){<br>
 * HibernateUtils.currentSession();<br>
 * try {<br>
 * // do work for this request, this may involve many<br>
 * // DAO operations, each calling current/close session.<br>
 * // These nested current/close operations will actually<br>
 * // just be given the session we opened in this filter.<br>
 * filterChain.doFilter(request,response);<br>
 * }<br>
 * finally {<br>
 * HibernateUtils.closeSession();<br>
 * }<br>
 * }<br>
 */

public class HibernateUtils
{
	private static Logger log = Logger.getLogger(HibernateUtils.class);

	/**
	 * 
	 */
	protected static SessionFactory sessionFactory;

	@SuppressWarnings("unchecked")
	public static final ThreadLocal session = new ThreadLocal();

	@SuppressWarnings("unchecked")
	public static final ThreadLocal depth = new ThreadLocal();

	private static Statistics stats;

	static
	{
		try
		{
			// Create the SessionFactory
			// below is now depreciated, all goes via configuration
			// sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();
			sessionFactory = new Configuration().configure().buildSessionFactory();
			stats = sessionFactory.getStatistics();
		}
		catch(Throwable ex)
		{
			// Make sure you log the exception, as it might be swallowed
			log.error("Initial SessionFactory creation failed.", ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static double getQueryHitRatio()
	{
		double queryCacheHitCount = stats.getQueryCacheHitCount();
		double queryCacheMissCount = stats.getQueryCacheMissCount();
		double queryCacheHitRatio = queryCacheHitCount / (queryCacheHitCount + queryCacheMissCount);
		return queryCacheHitRatio;
	}

	public static double getSecondLevelQueryHitRatio()
	{
		double queryCacheHitCount = stats.getSecondLevelCacheHitCount();
		double queryCacheMissCount = stats.getSecondLevelCacheMissCount();
		double queryCacheHitRatio = queryCacheHitCount / (queryCacheHitCount + queryCacheMissCount);
		return queryCacheHitRatio;
	}

	public static double getSecondLevelQueryHitRatio(String region)
	{
		SecondLevelCacheStatistics stats2 = sessionFactory.getStatistics().getSecondLevelCacheStatistics(region);
		double queryCacheHitCount = stats2.getHitCount();
		double queryCacheMissCount = stats2.getMissCount();
		double queryCacheHitRatio = queryCacheHitCount / (queryCacheHitCount + queryCacheMissCount);
		return queryCacheHitRatio;
	}

	public static Statistics getStatistics()
	{
		return sessionFactory.getStatistics();
	}

	public static SecondLevelCacheStatistics getSecondLevelCacheStatistics(String regionName)
	{
		Statistics stats = sessionFactory.getStatistics();
		return stats.getSecondLevelCacheStatistics(regionName);
	}

	public static void enableJMX(String name) throws Exception
	{

		sessionFactory.getStatistics().setStatisticsEnabled(true);

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName on = new ObjectName("Hibernate:type=statistics,application=" + name);
		StatisticsService mBean = new StatisticsService();
		mBean.setSessionFactory(sessionFactory);
		server.registerMBean(mBean, on);
	}

	public static void disableJMX(String name) throws Exception
	{
		sessionFactory.getStatistics().setStatisticsEnabled(false);

		ObjectName on = new ObjectName("Hibernate:type=statistics,application=" + name);
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		server.unregisterMBean(on);
	}

	/**
	 * Returns the current session.
	 * <p>
	 * If a session has not already been opened it will create a new session.
	 * <p>
	 * Note that currentSession and closeSession MUST ALWAYS be used within a try/finally clause to ensure sessions are closed correctly.<br>
	 * e.g. <br>
	 * Session session = HibernateUtils.currentSession(); <br>
	 * try { // do work here } <br>
	 * finally { <br>
	 * HibernateUtils.closeSession(); <br>
	 * }
	 */

	@SuppressWarnings("unchecked")
	public static Session currentSession()
	{
		Session s = (Session) session.get();
		if(s == null)
		{
			// open a new session if this thread has none yet
			// also flag the nesting depth as 0
			log.debug("HibernateUtils.currentSession(): Opening Hibernate session : if this is a search slave app, if the master share cant be found, it will hang the app with no logging - this is now the logging");
			s = sessionFactory.openSession();
			session.set(s);
			depth.set(new Integer(0));
		}
		else
		{
			// if a session already exists, simply increment
			// the depth flag and return the current session
			int d = ((Integer) depth.get()).intValue();
			depth.set(new Integer(d + 1));
		}
		return s;
	}

	/**
	 * Closes the current session.
	 * <p>
	 * Note that this method should always be within a finally clause to ensure ALL sessions are closed correctly.
	 */
	@SuppressWarnings("unchecked")
	public static void closeSession(boolean forceClose)
	{
		if(depth.get() != null)
		{
			int d = ((Integer) depth.get()).intValue();

			if(d <= 0 || forceClose)
			{
				// once the depth is back to zero, we know
				// we can close the session. if forceClose
				// is true we should also close the session
				// no matter what the nesting depth is.
				log.debug("Closing Hibernate session");
				((Session) session.get()).close();
				depth.set(null);
				session.set(null);

				// if force close is true, we should be on
				// on a depth of zero. If this is not the
				// case we have a coding problem and should
				// log the error
				if(forceClose && d != 0)
				{
					StackTraceElement[] ste = Thread.currentThread().getStackTrace();

					StringBuilder sb = new StringBuilder();

					for(StackTraceElement steObj : ste)
					{
						sb.append("[ " + steObj.getClassName() + "-" + steObj.getMethodName() + "-" + steObj.getLineNumber() + " ]");
					}

					log.error("Force close flag is set to true but our nesting depth is not zero (" + d + "). This indicates a coding error in one of the DAO operations for this request. We will force the session to close but please ensure try/finally clause is correctly in place for the calling DAO operations. Stack Trace Elements [" + sb.toString() + "]");
				}
			}
			else
			{
				// if the nesting depth is greater than
				// greater than zero, we should leave the session
				// open but decrement the nesting depth
				depth.set(new Integer(d - 1));
			}
		}
		else
		{
			// if the depth is null this indicates the session
			// has already been closed and we have a problem with
			// our try/finally coding somewhere in the operations
			// calling this method.
			log.error("CloseSession called for an already closed session. Please ensure try/finally clause is correctly in place for the calling DAO operations");
		}
	}

	/**
	 * Overloaded method to by default not force a close of the hibernate session.
	 */
	public static void closeSession()
	{
		closeSession(false);
	}

	/**
	 * 
	 */
	public static void closeSessionFactory()
	{
		sessionFactory.close();
	}

	public static Integer getDepthValue()
	{
		if(depth.get() != null)
		{
			return ((Integer) depth.get()).intValue();
		}

		return 0;
	}

}
