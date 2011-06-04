package com.progriff.dao;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.progriff.util.HibernateUtils;

public abstract class BaseDao<T>
{
	public static Logger log = Logger.getLogger(BaseDao.class);

	private boolean filtersEnabled = false;
	private boolean cacheEnabled = true;

	public abstract Class<T> getModelClass();

	public BaseDao()
	{
		super();
	}

	public static String addKeywordWildcards(String keyword)
	{
		StringBuffer keywordBuff = new StringBuffer();
		keywordBuff.append("%");
		keywordBuff.append(keyword);
		keywordBuff.append("%");

		return keywordBuff.toString();
	}

	@SuppressWarnings("unchecked")
	public T getById(long id) throws Exception
	{
		Session s = HibernateUtils.currentSession();
		try
		{
			Object o = s.get(getModelClass(), id);

			if(o != null)
			{
				return (T) o;
			}
			else
			{
				return null;
			}
		}
		catch(RuntimeException e)
		{
			throw e;
		}
		finally
		{
			HibernateUtils.closeSession();
		}
	}

	public void create(T o) throws Exception
	{
		Session s = HibernateUtils.currentSession();
		Transaction t = null;
		try
		{
			t = s.beginTransaction();
			s.save(o);
			t.commit();
		}
		catch(RuntimeException e)
		{
			if(t != null)
			{
				t.rollback();
			}
			throw e;
		}
		finally
		{
			HibernateUtils.closeSession();
		}
	}

	public void update(T o) throws Exception
	{
		Session s = HibernateUtils.currentSession();
		Transaction t = null;
		try
		{
			t = s.beginTransaction();
			s.update(o);
			t.commit();
			s.flush();
		}
		catch(RuntimeException e)
		{
			if(t != null)
			{
				t.rollback();
			}
			throw e;
		}
		finally
		{
			HibernateUtils.closeSession();
		}
	}

	public void delete(T o) throws Exception
	{
		Session s = HibernateUtils.currentSession();
		Transaction t = null;

		try
		{
			t = s.beginTransaction();
			s.delete(o);
			t.commit();
			s.flush();
		}
		catch(RuntimeException e)
		{
			if(t != null)
			{
				t.rollback();
			}
			throw e;
		}
		finally
		{
			HibernateUtils.closeSession();
		}
	}

	public void evict(T o)
	{
		Session s = HibernateUtils.currentSession();

		try
		{
			s.evict(o);
		}
		finally
		{
			HibernateUtils.closeSession();
		}
	}

	public void attach(T o)
	{
		Session s = HibernateUtils.currentSession();

		try
		{
			s.refresh(o);
		}
		finally
		{
			HibernateUtils.closeSession();
		}
	}

	public boolean isFiltersEnabled()
	{
		return filtersEnabled;
	}

	public void setFiltersEnabled(boolean filtersEnabled)
	{
		this.filtersEnabled = filtersEnabled;
	}

	public boolean isCacheEnabled()
	{
		return cacheEnabled;
	}

	public void setCacheEnabled(boolean cacheEnabled)
	{
		this.cacheEnabled = cacheEnabled;
	}

}
