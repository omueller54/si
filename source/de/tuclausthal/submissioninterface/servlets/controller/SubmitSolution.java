/*
 * Copyright 2009 - 2014 Sven Strickroth <email@cs-ware.de>
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

package de.tuclausthal.submissioninterface.servlets.controller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.DiskFileUpload;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.tuclausthal.submissioninterface.persistence.dao.DAOFactory;
import de.tuclausthal.submissioninterface.persistence.dao.ParticipationDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.SubmissionDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.TaskDAOIf;
import de.tuclausthal.submissioninterface.persistence.dao.impl.LogDAO;
import de.tuclausthal.submissioninterface.persistence.datamodel.LogEntry.LogAction;
import de.tuclausthal.submissioninterface.persistence.datamodel.Participation;
import de.tuclausthal.submissioninterface.persistence.datamodel.ParticipationRole;
import de.tuclausthal.submissioninterface.persistence.datamodel.Submission;
import de.tuclausthal.submissioninterface.persistence.datamodel.Task;
import de.tuclausthal.submissioninterface.persistence.datamodel.Test;
import de.tuclausthal.submissioninterface.persistence.datamodel.UMLConstraintTest;
import de.tuclausthal.submissioninterface.servlets.RequestAdapter;
import de.tuclausthal.submissioninterface.template.Template;
import de.tuclausthal.submissioninterface.template.TemplateFactory;
import de.tuclausthal.submissioninterface.util.ContextAdapter;
import de.tuclausthal.submissioninterface.util.Util;

/**
 * Controller-Servlet for the submission of files
 * @author Sven Strickroth
 */
public class SubmitSolution extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Session session = RequestAdapter.getSession(request);
		TaskDAOIf taskDAO = DAOFactory.TaskDAOIf(session);
		Task task = taskDAO.getTask(Util.parseInteger(request.getParameter("taskid"), 0));
		if (task == null) {
			request.setAttribute("title", "Aufgabe nicht gefunden");
			request.getRequestDispatcher("MessageView").forward(request, response);
			return;
		}

		// check Lecture Participation
		ParticipationDAOIf participationDAO = DAOFactory.ParticipationDAOIf(session);
		Participation participation = participationDAO.getParticipation(RequestAdapter.getUser(request), task.getTaskGroup().getLecture());
		if (participation == null) {
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "insufficient rights");
			return;
		}

		boolean canUploadForStudents = participation.getRoleType() == ParticipationRole.ADVISOR || (task.isTutorsCanUploadFiles() && participation.getRoleType() == ParticipationRole.TUTOR);

		// if session-user is not a tutor (with rights to upload for students) or advisor: check dates
		if (!canUploadForStudents) {
			if (participation.getRoleType() == ParticipationRole.TUTOR) {
				request.setAttribute("title", "Tutoren k�nnen keine eigenen L�sungen einsenden.");
				request.getRequestDispatcher("MessageView").forward(request, response);
				return;
			}
			if (task.getStart().after(Util.correctTimezone(new Date()))) {
				request.setAttribute("title", "Abgabe nicht gefunden");
				request.getRequestDispatcher("MessageView").forward(request, response);
				return;
			}
			if (task.getDeadline().before(Util.correctTimezone(new Date()))) {
				request.setAttribute("title", "Abgabe nicht mehr m�glich");
				request.getRequestDispatcher("MessageView").forward(request, response);
				return;
			}
		}

		if (task.isShowTextArea() == false && "-".equals(task.getFilenameRegexp())) {
			request.setAttribute("title", "Das Einsenden von L�sungen ist f�r diese Aufgabe deaktiviert.");
			request.getRequestDispatcher("MessageView").forward(request, response);
			return;
		}

		request.setAttribute("task", task);

		if (canUploadForStudents) {
			request.getRequestDispatcher("SubmitSolutionAdvisorFormView").forward(request, response);
		} else {
			request.setAttribute("participation", participation);

			if (request.getParameter("onlypartners") == null) {
				if (task.isShowTextArea()) {
					String textsolution = "";
					Submission submission = DAOFactory.SubmissionDAOIf(session).getSubmission(task, RequestAdapter.getUser(request));
					if (submission != null) {
						ContextAdapter contextAdapter = new ContextAdapter(getServletContext());
						File textSolutionFile = new File(contextAdapter.getDataPath().getAbsolutePath() + System.getProperty("file.separator") + task.getTaskGroup().getLecture().getId() + System.getProperty("file.separator") + task.getTaskid() + System.getProperty("file.separator") + submission.getSubmissionid() + System.getProperty("file.separator") + "textloesung.txt");
						if (textSolutionFile.exists()) {
							BufferedReader bufferedReader = new BufferedReader(new FileReader(textSolutionFile));
							StringBuffer sb = new StringBuffer();
							String line;
							while ((line = bufferedReader.readLine()) != null) {
								sb.append(line);
								sb.append(System.getProperty("line.separator"));
							}
							textsolution = sb.toString();
							bufferedReader.close();
						}
					}
					request.setAttribute("textsolution", textsolution);
				}
				request.getRequestDispatcher("SubmitSolutionFormView").forward(request, response);
			} else {
				request.getRequestDispatcher("SubmitSolutionPossiblePartnersView").forward(request, response);
			}
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		Session session = RequestAdapter.getSession(request);
		Template template = TemplateFactory.getTemplate(request, response);

		TaskDAOIf taskDAO = DAOFactory.TaskDAOIf(session);
		Task task = taskDAO.getTask(Util.parseInteger(request.getParameter("taskid"), 0));
		if (task == null) {
			template.printTemplateHeader("Aufgabe nicht gefunden");
			PrintWriter out = response.getWriter();
			out.println("<div class=mid><a href=\"" + response.encodeURL("?") + "\">zur �bersicht</a></div>");
			template.printTemplateFooter();
			return;
		}

		// check Lecture Participation
		ParticipationDAOIf participationDAO = DAOFactory.ParticipationDAOIf(session);

		Participation studentParticipation = participationDAO.getParticipation(RequestAdapter.getUser(request), task.getTaskGroup().getLecture());
		if (studentParticipation == null) {
			template.printTemplateHeader("Ung�ltige Anfrage");
			PrintWriter out = response.getWriter();
			out.println("<div class=mid>Sie sind kein Teilnehmer dieser Veranstaltung.</div>");
			out.println("<div class=mid><a href=\"" + response.encodeURL("Overview") + "\">zur �bersicht</a></div>");
			template.printTemplateFooter();
			return;
		}

		//http://commons.apache.org/fileupload/using.html

		// Check that we have a file upload request
		boolean isMultipart = FileUpload.isMultipartContent(request);

		List<Integer> partnerIDs = new LinkedList<Integer>();
		int uploadFor = 0;
		List<FileItem> items = null;
		if (!isMultipart) {
			if (request.getParameterValues("partnerid") != null) {
				for (String partnerIdParameter : request.getParameterValues("partnerid")) {
					int partnerID = Util.parseInteger(partnerIdParameter, 0);
					if (partnerID > 0) {
						partnerIDs.add(partnerID);
					}
				}
			}
		} else {
			// Create a new file upload handler
			FileUploadBase upload = new DiskFileUpload();
			upload.setSizeMax(task.getMaxsize());

			// Parse the request
			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;
			}

			// Process the uploaded items
			Iterator<FileItem> iter = items.iterator();
			while (iter.hasNext()) {
				FileItem item = iter.next();
				if (item.isFormField() && "partnerid".equals(item.getFieldName())) {
					int partnerID = Util.parseInteger(item.getString(), 0);
					if (partnerID > 0) {
						partnerIDs.add(partnerID);
					}
				} else if (item.isFormField() && "uploadFor".equals(item.getFieldName())) {
					uploadFor = Util.parseInteger(item.getString(), 0);
				}
			}
		}

		if (uploadFor > 0) {
			// Uploader ist wahrscheinlich Betreuer -> keine zeitlichen Pr�fungen
			// check if uploader is allowed to upload for students
			if (!(studentParticipation.getRoleType() == ParticipationRole.ADVISOR || (task.isTutorsCanUploadFiles() && studentParticipation.getRoleType() == ParticipationRole.TUTOR))) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Sie sind nicht berechtigt bei dieser Veranstaltung Dateien f�r Studenten hochzuladen.</div>");
				out.println("<div class=mid><a href=\"" + response.encodeURL("Overview") + "\">zur �bersicht</a></div>");
				template.printTemplateFooter();
				return;
			}
			studentParticipation = participationDAO.getParticipation(uploadFor);
			if (studentParticipation == null || studentParticipation.getLecture().getId() != task.getTaskGroup().getLecture().getId()) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Der gew�hlte Student ist kein Teilnehmer dieser Veranstaltung.</div>");
				out.println("<div class=mid><a href=\"" + response.encodeURL("Overview") + "\">zur �bersicht</a></div>");
				template.printTemplateFooter();
				return;
			}
			if (task.isShowTextArea() == false && "-".equals(task.getFilenameRegexp())) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Das Einsenden von L�sungen ist f�r diese Aufgabe deaktiviert.</div>");
				template.printTemplateFooter();
				return;
			}
		} else {
			if (studentParticipation.getRoleType() == ParticipationRole.ADVISOR || studentParticipation.getRoleType() == ParticipationRole.TUTOR) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Betreuer und Tutoren k�nnen keine eigenen L�sungen einsenden.</div>");
				template.printTemplateFooter();
				return;
			}
			// Uploader is Student, -> hard date checks
			if (task.getStart().after(Util.correctTimezone(new Date()))) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Abgabe nicht gefunden.</div>");
				template.printTemplateFooter();
				return;
			}
			if (task.getDeadline().before(Util.correctTimezone(new Date()))) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Abgabe nicht mehr m�glich.</div>");
				template.printTemplateFooter();
				return;
			}
			if (isMultipart && "-".equals(task.getFilenameRegexp())) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Dateiupload ist f�r diese Aufgabe deaktiviert.</div>");
				template.printTemplateFooter();
				return;
			} else if (!isMultipart && !task.isShowTextArea()) {
				template.printTemplateHeader("Ung�ltige Anfrage");
				PrintWriter out = response.getWriter();
				out.println("<div class=mid>Textl�sungen sind f�r diese Aufgabe deaktiviert.</div>");
				template.printTemplateFooter();
				return;
			}
		}

		SubmissionDAOIf submissionDAO = DAOFactory.SubmissionDAOIf(session);

		Transaction tx = session.beginTransaction();
		Submission submission = submissionDAO.createSubmission(task, studentParticipation);

		if (studentParticipation.getGroup() != null && task.getMaxSubmitters() > 1) {
			if (studentParticipation.getGroup().isSubmissionGroup()) {
				for (Participation partnerParticipation : studentParticipation.getGroup().getMembers()) {
					Submission partnerSubmission = submissionDAO.getSubmissionLocked(task, partnerParticipation.getUser());
					if (partnerSubmission == null) {
						submission.getSubmitters().add(partnerParticipation);
						session.update(submission);
					} else if (partnerSubmission.getSubmissionid() != submission.getSubmissionid()) {
						tx.rollback();
						template.printTemplateHeader("Ung�ltige Anfrage");
						PrintWriter out = response.getWriter();
						out.println("<div class=mid>Ein Gruppenpartner hat bereits eine Gruppen-Abgabe initiiert.</div>");
						template.printTemplateFooter();
						return;
					}
				}
			} else {
				for (int partnerID : partnerIDs) {
					Participation partnerParticipation = participationDAO.getParticipation(partnerID);
					if (submission.getSubmitters().size() < task.getMaxSubmitters() && partnerParticipation != null && partnerParticipation.getLecture().getId() == task.getTaskGroup().getLecture().getId() && (task.isAllowSubmittersAcrossGroups() || (partnerParticipation.getGroup() != null && partnerParticipation.getGroup().getGid() == studentParticipation.getGroup().getGid())) && submissionDAO.getSubmissionLocked(task, partnerParticipation.getUser()) == null) {
						submission.getSubmitters().add(partnerParticipation);
						session.update(submission);
					} else {
						tx.rollback();
						template.printTemplateHeader("Ung�ltige Anfrage");
						PrintWriter out = response.getWriter();
						out.println("<div class=mid>Ein ausgew�hlter Partner hat bereits eine eigene Abgabe initiiert oder Sie haben bereits die maximale Anzahl von Partnern ausgew�hlt.</div>");
						template.printTemplateFooter();
						return;
					}
				}
			}
		}

		ContextAdapter contextAdapter = new ContextAdapter(getServletContext());

		File path = new File(contextAdapter.getDataPath().getAbsolutePath() + System.getProperty("file.separator") + task.getTaskGroup().getLecture().getId() + System.getProperty("file.separator") + task.getTaskid() + System.getProperty("file.separator") + submission.getSubmissionid() + System.getProperty("file.separator"));
		if (path.exists() == false) {
			path.mkdirs();
		}

		if (isMultipart) {
			// Process the uploaded items
			Iterator<FileItem> iter = items.iterator();
			while (iter.hasNext()) {
				FileItem item = iter.next();

				// Process a file upload
				if (!item.isFormField()) {
					Pattern pattern;
					if (task.getFilenameRegexp() == null || task.getFilenameRegexp().isEmpty() || uploadFor > 0) {
						pattern = Pattern.compile("^(?:.*?[\\\\/])?([a-zA-Z0-9_. -]+)$");
					} else {
						pattern = Pattern.compile("^(?:.*?[\\\\/])?(" + task.getFilenameRegexp() + ")$");
					}
					StringBuffer submittedFileName = new StringBuffer(item.getName());
					if (submittedFileName.lastIndexOf(".") > 0) {
						int lastDot = submittedFileName.lastIndexOf(".");
						submittedFileName.replace(lastDot, submittedFileName.length(), submittedFileName.subSequence(lastDot, submittedFileName.length()).toString().toLowerCase());
					}
					Matcher m = pattern.matcher(submittedFileName);
					if (!m.matches()) {
						if (!submissionDAO.deleteIfNoFiles(submission, path)) {
							submission.setLastModified(new Date());
							submissionDAO.saveSubmission(submission);
						}
						System.err.println("SubmitSolutionProblem2: " + item.getName() + ";" + submittedFileName + ";" + pattern.pattern());
						tx.commit();
						template.printTemplateHeader("Ung�ltige Anfrage");
						PrintWriter out = response.getWriter();
						out.println("Dateiname ung�ltig bzw. entspricht nicht der Vorgabe (ist ein Klassenname vorgegeben, so muss die Datei genauso hei�en).<br>Tipp: Nur A-Z, a-z, 0-9, ., - und _ sind erlaubt. Evtl. muss der Dateiname mit einem Gro�buchstaben beginnen und darf keine Leerzeichen enthalten.");
						if (uploadFor > 0) {
							out.println("<br>F�r Experten: Der Dateiname muss dem folgenden regul�ren Ausdruck gen�gen: " + Util.escapeHTML(pattern.pattern()));
						}
						template.printTemplateFooter();
						return;
					}
					String fileName = m.group(1);
					if (!"-".equals(task.getArchiveFilenameRegexp()) && (fileName.endsWith(".zip") || fileName.endsWith(".jar"))) {
						ZipInputStream zipFile;
						Pattern archivePattern;
						if (task.getArchiveFilenameRegexp() == null || task.getArchiveFilenameRegexp().isEmpty()) {
							archivePattern = Pattern.compile("^(([/a-zA-Z0-9_ .-]*?/)?([a-zA-Z0-9_ .-]+))$");
						} else if (task.getArchiveFilenameRegexp().startsWith("^")) {
							archivePattern = Pattern.compile("^(" + task.getArchiveFilenameRegexp().substring(1) + ")$");
						} else {
							archivePattern = Pattern.compile("^(([/a-zA-Z0-9_ .-]*?/)?(" + task.getArchiveFilenameRegexp() + "))$");
						}
						try {
							zipFile = new ZipInputStream(item.getInputStream());
							ZipEntry entry = null;
							while ((entry = zipFile.getNextEntry()) != null) {
								if (entry.getName().contains("..") || entry.isDirectory()) {
									System.err.println("Ignored entry: " + entry.getName() + "; contains \"..\" or is directory");
									continue;
								}
								StringBuffer archivedFileName = new StringBuffer(entry.getName().replace("\\", "/"));
								if (!archivePattern.matcher(archivedFileName).matches()) {
									System.err.println("Ignored entry: " + archivedFileName + ";" + archivePattern.pattern());
									continue;
								}
								if (!entry.getName().toLowerCase().endsWith(".class")) {
									if (archivedFileName.lastIndexOf(".") > 0) {
										int lastDot = archivedFileName.lastIndexOf(".");
										archivedFileName.replace(lastDot, archivedFileName.length(), archivedFileName.subSequence(lastDot, archivedFileName.length()).toString().toLowerCase());
									}
									// TODO: relocate java-files from jar/zip archives?
									File fileToCreate = new File(path, archivedFileName.toString());
									if (!fileToCreate.getParentFile().exists()) {
										fileToCreate.getParentFile().mkdirs();
									}
									Util.copyInputStreamAndClose(zipFile, new BufferedOutputStream(new FileOutputStream(fileToCreate)));
								}
							}
							zipFile.close();
						} catch (IOException e) {
							if (!submissionDAO.deleteIfNoFiles(submission, path)) {
								submission.setLastModified(new Date());
								submissionDAO.saveSubmission(submission);
							}
							System.err.println("SubmitSolutionProblem1");
							tx.commit();
							System.err.println(e.getMessage());
							e.printStackTrace();
							template.printTemplateHeader("Ung�ltige Anfrage");
							PrintWriter out = response.getWriter();
							out.println("Problem beim Entpacken des Archives.");
							template.printTemplateFooter();
							return;
						}
					} else {
						Util.saveAndRelocateJavaFile(item, path, fileName);
					}
					if (!submissionDAO.deleteIfNoFiles(submission, path)) {
						submission.setLastModified(new Date());
						submissionDAO.saveSubmission(submission);
					}
					tx.commit();
					new LogDAO(session).createLogEntry(studentParticipation.getUser(), null, task, LogAction.UPLOAD, null, null);
					response.addIntHeader("SID", submission.getSubmissionid());

					for (Test test : task.getTests()) {
						if (test instanceof UMLConstraintTest && test.getTimesRunnableByStudents() > 0) {
							response.addIntHeader("TID", test.getId());
							break;
						}
					}

					response.sendRedirect(response.encodeRedirectURL("ShowTask?taskid=" + task.getTaskid()));
					return;
				}
			}
			if (!submissionDAO.deleteIfNoFiles(submission, path)) {
				submission.setLastModified(new Date());
				submissionDAO.saveSubmission(submission);
			}
			System.err.println("SubmitSolutionProblem3");
			System.err.println("Problem: Keine Abgabedaten gefunden.");
			tx.commit();
			PrintWriter out = response.getWriter();
			out.println("Problem: Keine Abgabedaten gefunden.");
		} else if (request.getParameter("textsolution") != null) {
			if (task.isADynamicTask()) {
				int numberOfFields = task.getDynamicTaskStrategie(session).getNumberOfResultFields();
				List<String> results = new LinkedList<String>();
				for (int i = 0; i < numberOfFields; i++) {
					String result = request.getParameter("dynamicresult" + i);
					if (result == null) {
						result = "";
					}
					results.add(result);
				}
				DAOFactory.ResultDAOIf(session).createResults(submission, results);
				DAOFactory.TaskNumberDAOIf(session).assignTaskNumbersToSubmission(submission, studentParticipation);
			}

			File uploadedFile = new File(path, "textloesung.txt");
			FileWriter fileWriter = new FileWriter(uploadedFile);
			if (request.getParameter("textsolution") != null && request.getParameter("textsolution").length() <= task.getMaxsize()) {
				fileWriter.write(request.getParameter("textsolution"));
			}
			fileWriter.close();

			submission.setLastModified(new Date());
			submissionDAO.saveSubmission(submission);
			tx.commit();

			response.sendRedirect(response.encodeRedirectURL("ShowTask?taskid=" + task.getTaskid()));
		} else {
			if (!submissionDAO.deleteIfNoFiles(submission, path)) {
				submission.setLastModified(new Date());
				submissionDAO.saveSubmission(submission);
			}
			System.out.println("SubmitSolutionProblem4");
			tx.commit();
			PrintWriter out = response.getWriter();
			out.println("Problem: Keine Abgabedaten gefunden.");
		}
	}
}
