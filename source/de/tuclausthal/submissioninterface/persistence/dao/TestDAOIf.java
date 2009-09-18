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

package de.tuclausthal.submissioninterface.persistence.dao;

import de.tuclausthal.submissioninterface.persistence.datamodel.JUnitTest;
import de.tuclausthal.submissioninterface.persistence.datamodel.RegExpTest;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.Test;

/**
 * Data Access Object Interface for the Test-class(es)
 * @author Sven Strickroth
 */
public interface TestDAOIf {
	/**
	 * Create and store a JUnitTest to a specific task
	 * @param task the task to associtate the test to
	 * @return the created test
	 */
	public JUnitTest createJUnitTest(Task task);

	/**
	 * Create and store a JUnitTest to a specific task
	 * @param task the task to associtate the test to
	 * @return the created test
	 */
	public RegExpTest createRegExpTest(Task task);

	/**
	 * Update/save a test
	 * @param test the test to update
	 */
	public void saveTest(Test test);

	/**
	 * Remove a specific test from the DB
	 * @param test the test to remvoe
	 */
	public void deleteTest(Test test);

	public Test getTest(int testId);
}
