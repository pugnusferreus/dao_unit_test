package com.progriff.dao;

import com.progriff.model.User;

public class UserDaoImpl extends BaseDao<User> implements UserDao
{

	@Override
	public Class<User> getModelClass()
	{
		return User.class;
	}

}
