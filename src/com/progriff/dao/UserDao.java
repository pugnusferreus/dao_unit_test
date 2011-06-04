package com.progriff.dao;

import com.progriff.model.User;

public interface UserDao
{
	/**
	 * Inserts a row into the user table
	 * 
	 * @param o
	 * @throws Exception
	 */
	public void create(User o) throws Exception;

	/**
	 * Updates a row in the user table
	 * 
	 * @param o
	 * @throws Exception
	 */
	public void update(User o) throws Exception;

	/**
	 * Removes a row from the user table
	 * 
	 * @param o
	 * @throws Exception
	 */
	public void delete(User o) throws Exception;

	/**
	 * Get a row from user table
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public User getById(long id) throws Exception;
}
