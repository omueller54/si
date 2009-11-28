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

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.Group;
import de.tuclausthal.submissioninterface.persistence.datamodel.Lecture;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.ParticipationRole;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;

/**
 * Data Access Object implementation for the ParticipationDAOIf
 * @author Sven Strickroth
 */
public class ParticipationDAO extends AbstractDAO implements ParticipationDAOIf {
	public ParticipationDAO(Session session) {
		super(session);
	}

	@Override
	public Participation createParticipation(User user, Lecture lecture, ParticipationRole type) {
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		Participation participation = null;

		// try to load an existing participation and lock it (or lock it in advance, so that nobody can create it in another thread)
		participation = (Participation) session.createCriteria(Participation.class).add(Restrictions.eq("lecture", lecture)).add(Restrictions.eq("user", user)).setLockMode(LockMode.UPGRADE).uniqueResult();
		if (participation == null) {
			participation = new Participation();
			participation.setUser(user);
			participation.setLecture(lecture);
		}
		participation.setRoleType(type);
		session.saveOrUpdate(participation);
		tx.commit();

		return participation;
	}

	@Override
	public void deleteParticipation(Participation participation) {
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		session.update(participation);
		session.delete(participation);
		tx.commit();
	}

	@Override
	public Participation getParticipation(User user, Lecture lecture) {
		return (Participation) getSession().createCriteria(Participation.class).add(Restrictions.eq("lecture", lecture)).add(Restrictions.eq("user", user)).uniqueResult();
	}

	@Override
	public void deleteParticipation(User user, Lecture lecture) {
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		Participation participation = (Participation) session.createCriteria(Participation.class).add(Restrictions.eq("lecture", lecture)).add(Restrictions.eq("user", user)).setLockMode(LockMode.UPGRADE).uniqueResult();
		if (participation != null) {
			session.delete(participation);
		}
		tx.commit();
	}

	@Override
	public List<Participation> getParticipationsWithoutGroup(Lecture lecture) {
		return (List<Participation>) getSession().createCriteria(Participation.class).add(Restrictions.eq("lecture", lecture)).add(Restrictions.isNull("group")).createCriteria("user").addOrder(Order.asc("lastName")).addOrder(Order.asc("firstName")).list();
	}

	@Override
	public List<Participation> getParticipationsOfGroup(Group group) {
		return (List<Participation>) getSession().createCriteria(Participation.class).add(Restrictions.eq("group", group)).createCriteria("user").addOrder(Order.asc("lastName")).addOrder(Order.asc("firstName")).list();
	}

	@Override
	public Participation getParticipation(int participationid) {
		return (Participation) getSession().get(Participation.class, participationid);
	}

	@Override
	public void saveParticipation(Participation participation) {
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		session.save(participation);
		tx.commit();
	}

	@Override
	public List<Participation> getParticipationsOfLectureOrdered(Lecture lecture) {
		return (List<Participation>) getSession().createCriteria(Participation.class).add(Restrictions.eq("lecture", lecture)).createCriteria("user").addOrder(Order.asc("lastName")).addOrder(Order.asc("firstName")).list();
	}
}
