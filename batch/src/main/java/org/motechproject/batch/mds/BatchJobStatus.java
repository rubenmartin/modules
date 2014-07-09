package org.motechproject.batch.mds;

// Generated Apr 11, 2014 10:49:43 AM by Hibernate Tools 3.4.0.CR1

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * Class to specify the status of job
 */
@Entity
public class BatchJobStatus implements java.io.Serializable {

    private static final long serialVersionUID = -6254679543385251579L;

    @Field(required = true)
    private Integer jobStatusId;

    @Field(required = true)
    private String jobStatusCode;

    public BatchJobStatus() {
    }

    public BatchJobStatus(Integer jobStatusId, String jobStatusCode) {
        this.jobStatusId = jobStatusId;
        this.jobStatusCode = jobStatusCode;
    }

    public Integer getJobStatusId() {
        return jobStatusId;
    }

    public void setJobStatusId(Integer jobStatusId) {
        this.jobStatusId = jobStatusId;
    }

    public String getJobStatusCode() {
        return jobStatusCode;
    }

    public void setJobStatusCode(String jobStatusCode) {
        this.jobStatusCode = jobStatusCode;
    }

}
