/*
 * Copyright 2009 Sven Strickroth <email@cs-ware.de>
 * 
 * This file is part of the SubmissionInterface.
 * 
 * SubmissionInterface is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 * 
 * SubmissionInterface is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SubmissionInterface. If not, see <http://www.gnu.org/licenses/>.
 */

package de.tuclausthal.submissioninterface.persistence.dao.impl;

import org.hibernate.Session;
import org.hibernate.Transaction;

import de.tuclausthal.submissioninterface.persistence.dao.GroupDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.Group;
import de.tuclausthal.submissioninterface.persistence.datamodel.Lecture;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.util.HibernateSessionHelper;

/**
 * Data Access Object implementation for the GroupDAOIf
 * @author Sven Strickroth
 */
public class GroupDAO implements GroupDAOIf {
	@Override
	public Group createGroup(Lecture lecture, String name) {
		Session session = HibernateSessionHelper.getSession();
		Transaction tx = session.beginTransaction();
		Group group = new Group();
		group.setName(name);
		group.setLecture(lecture);
		session.save(group);
		tx.commit();
		return group;
	}

	@Override
	public void deleteGroup(Group group) {
		Session session = HibernateSessionHelper.getSession();
		Transaction tx = session.beginTransaction();
		for (Participation participation : group.getMembers()) {
			participation.setGroup(null);
			session.update(participation);
		}
		session.update(group);
		session.delete(group);
		tx.commit();
	}

	@Override
	public Group getGroup(int groupid) {
		return (Group) HibernateSessionHelper.getSession().get(Group.class, groupid);
	}

	@Override
	public void saveGroup(Group group) {
		Session session = HibernateSessionHelper.getSession();
		Transaction tx = session.beginTransaction();
		session.update(group);
		tx.commit();
	}
}