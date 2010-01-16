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

package de.tuclausthal.submissioninterface.authfilter;

import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.datamodel.User;
import de.tuclausthal.submissioninterface.testframework.executor.TestExecutorTestResult;

/**
 * Adapter for HTTP-session to have a better interface for our stored variables
 * @author Sven Strickroth
 */
public class SessionAdapter {
	private HttpSession session = null;

	/*	private String ipAddress;
		private Boolean linkedToIP;*/

	public SessionAdapter(HttpServletRequest request) {
		session = request.getSession(true);
	}

	/**
	 * Binds the user to the session
	 * @param user
	 */
	public void setUser(User user) {
		session.setAttribute("userID", user.getUid());
	}

	/**
	 * Reads the user from the session
	 * @return the user or null if no user was stored to the session
	 */
	public User getUser(Session hsession) {
		if (session.getAttribute("userID") != null) {
			return DAOFactory.UserDAOIf(hsession).getUser((Integer) session.getAttribute("userID"));
		} else {
			return null;
		}
	}

	public Future<TestExecutorTestResult> getQueuedTest() {
		return (Future<TestExecutorTestResult>) session.getAttribute("queuedTest");
	}

	public void setQueuedTest(Future<TestExecutorTestResult> futureTestResult) {
		session.setAttribute("queuedTest", futureTestResult);
	}
}
