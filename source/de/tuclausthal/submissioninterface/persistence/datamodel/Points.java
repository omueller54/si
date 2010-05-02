/*
 * Copyright 2009 - 2010 Sven Strickroth <email@cs-ware.de>
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

package de.tuclausthal.submissioninterface.persistence.datamodel;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

@Embeddable
public class Points implements Serializable {
	private Integer points;
	private Boolean pointsOk;
	private Participation issuedBy;
	private String publicComment;
	private String internalComment;

	/**
	 * @return the points
	 */
	public Integer getPoints() {
		return points;
	}

	/**
	 * @param points the points to set
	 */
	public void setPoints(Integer points) {
		this.points = points;
	}

	/**
	 * @return the issuedBy
	 */
	@ManyToOne
	@ForeignKey(name = "issuedby")
	public Participation getIssuedBy() {
		return issuedBy;
	}

	/**
	 * @param issuedBy the issuedBy to set
	 */
	public void setIssuedBy(Participation issuedBy) {
		this.issuedBy = issuedBy;
	}

	/**
	 * @return the comment
	 */
	@Lob
	public String getPublicComment() {
		return publicComment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setPublicComment(String comment) {
		this.publicComment = comment;
	}

	/**
	 * @return the pointsOk
	 */
	public Boolean getPointsOk() {
		return pointsOk;
	}

	/**
	 * @param pointsOk the pointsOk to set
	 */
	public void setPointsOk(Boolean pointsOk) {
		this.pointsOk = pointsOk;
	}

	/**
	 * @return the internalComment
	 */
	@Lob
	public String getInternalComment() {
		return internalComment;
	}

	/**
	 * @param internalComment the internalComment to set
	 */
	public void setInternalComment(String internalComment) {
		this.internalComment = internalComment;
	}
}
