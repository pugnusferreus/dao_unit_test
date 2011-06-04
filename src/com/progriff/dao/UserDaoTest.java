package com.progriff.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.progriff.model.User;

public class UserDaoTest
{

	Logger log = Logger.getLogger(UserDaoTest.class);

	private static UserDao userDao;

	@BeforeClass
	public static void setup() throws Exception
	{
		userDao = new UserDaoImpl();
	}

	@Test
	public void testCreate()
	{
		try
		{
			User user = new User();
			user.setFirstName("Foo");
			user.setLastName("Bar");
			user.setUsername("foobar");
			user.setPassword("somepassword");

			userDao.create(user);
			assertNotNull(user.getId());
			log.info("user.id is : " + user.getId());
		}
		catch(Exception e)
		{
			fail("Exception occurred [" + e.toString() + "]");
		}
	}

	@Test
	public void testUpdate()
	{
		try
		{
			// create a new user
			User user = new User();
			user.setFirstName("Foo");
			user.setLastName("Bar");
			user.setUsername("foobar");
			user.setPassword("somepassword");

			userDao.create(user);
			assertNotNull(user.getId());
			Long id = user.getId();

			// retrieves the user again and update the values
			user = userDao.getById(id);
			assertNotNull(user);

			user.setFirstName("Foo2");
			user.setLastName("Bar2");
			user.setPassword("somepassword2");
			user.setUsername("foobar2");
			userDao.update(user);

			// retrieves again to make sure that the values are from HSQL
			user = userDao.getById(id);

			// verify the data
			assertNotNull(user);
			assertEquals("Foo2", user.getFirstName());
			assertEquals("Bar2", user.getLastName());
			assertEquals("somepassword2", user.getPassword());
			assertEquals("foobar2", user.getUsername());

		}
		catch(Exception e)
		{
			fail("Exception occurred [" + e.toString() + "]");
		}
	}

	@Test
	public void testDelete()
	{
		try
		{
			// create a new user
			User user = new User();
			user.setFirstName("Foo");
			user.setLastName("Bar");
			user.setUsername("foobar");
			user.setPassword("somepassword");

			userDao.create(user);
			assertNotNull(user.getId());
			Long id = user.getId();

			// retrieves the user again and update the values
			user = userDao.getById(id);
			assertNotNull(user);

			userDao.delete(user);

			// make sure that the user is no more
			user = userDao.getById(id);
			assertNull(user);

		}
		catch(Exception e)
		{
			fail("Exception occurred [" + e.toString() + "]");
		}
	}
}
