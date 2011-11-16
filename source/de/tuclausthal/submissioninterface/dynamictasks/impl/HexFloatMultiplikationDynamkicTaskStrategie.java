/*
 * Copyright 2011 Sven Strickroth <email@cs-ware.de>
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

package de.tuclausthal.submissioninterface.dynamictasks.impl;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Session;

import de.tuclausthal.submissioninterface.dynamictasks.AbstractDynamicTaskStrategie;
import de.tuclausthal.submissioninterface.dynamictasks.DynamicTaskStrategieIf;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.TaskNumber;
import de.tuclausthal.submissioninterface.util.RandomNumber;

/**
 * @author Sven Strickroth
 */
public class HexFloatMultiplikationDynamkicTaskStrategie extends AbstractDynamicTaskStrategie implements DynamicTaskStrategieIf {
	private static final String[] RESULT_FIELDS = { "L�sung der Berechnung (bin�r)", "L�sung der Berechnung (hex)", "Fehler der L�sung" };
	private static final String[] RESULT_FIELDS_WITH_PARTIAL = { "-L�sung der Berechnung (decimal)", "L�sung der Berechnung (bin�r)", "L�sung der Berechnung (hex)", "-Float-Ergebnis", "Fehler der L�sung" };
	private static final String[] VARIABLES = { "Wert 1", "Wert 2" };

	public HexFloatMultiplikationDynamkicTaskStrategie(Session session, Task task) {
		super(session, task);
	}

	@Override
	public String[] getResultFields(boolean includePartialSolutions) {
		if (includePartialSolutions) {
			return RESULT_FIELDS_WITH_PARTIAL;
		} else {
			return RESULT_FIELDS;
		}
	}

	@Override
	public boolean isCorrect(Submission submission) {
		List<String> correctResults = getCorrectResults(submission, false);
		List<String> studentSolution = getUserResults(submission);
		if (!studentSolution.get(0).equals(correctResults.get(0))) {
			return false;
		}
		if (!RandomNumber.trimLeadingZeros(studentSolution.get(1)).equals(RandomNumber.trimLeadingZeros(correctResults.get(1)))) {
			return false;
		}
		try {
			if (Math.abs(Double.parseDouble(correctResults.get(2)) - Double.parseDouble(studentSolution.get(2))) > 0.0000000001d) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public List<String> getCorrectResults(Submission submission, boolean includePartialSolutions) {
		List<TaskNumber> numbers = getVariables(submission);
		double result = Double.parseDouble(numbers.get(0).getOrigNumber()) * Double.parseDouble(numbers.get(1).getOrigNumber());
		List<String> results = new LinkedList<String>();
		if (includePartialSolutions) {
			results.add(String.valueOf(result));
		}
		results.add(RandomNumber.getFloatBits((float) result));
		results.add(RandomNumber.binStringToHex(results.get(results.size() - 1)));
		if (includePartialSolutions) {
			results.add(String.valueOf((float) result));
		}
		double diff = result - Double.valueOf(Float.toString((float) result));
		results.add(String.valueOf((float) diff));
		return results;
	}

	@Override
	public String[] getVariableNames() {
		return VARIABLES;
	}

	@Override
	protected List<TaskNumber> createTaskNumbers(Participation participation) {
		List<TaskNumber> numbers = new LinkedList<TaskNumber>();
		String[] floatNumbers = RandomNumber.getFloatBitsTruncated(RandomNumber.getFloat(true));
		while (floatNumbers[0].endsWith(".0")) {
			floatNumbers = RandomNumber.getFloatBitsTruncated(RandomNumber.getFloat(true));
		}
		numbers.add(new TaskNumber(getTask(), participation, RandomNumber.binStringToHex(floatNumbers[1]), floatNumbers[0]));
		floatNumbers = RandomNumber.getFloatBitsTruncated(RandomNumber.getFloat(true));
		while (floatNumbers[0].endsWith(".0")) {
			floatNumbers = RandomNumber.getFloatBitsTruncated(RandomNumber.getFloat(true));
		}
		numbers.add(new TaskNumber(getTask(), participation, RandomNumber.binStringToHex(floatNumbers[1]), floatNumbers[0]));
		return numbers;
	}
}