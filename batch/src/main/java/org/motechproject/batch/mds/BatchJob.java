package org.motechproject.batch.mds;

// Generated Apr 7, 2014 9:39:14 PM by Hibernate Tools 3.4.0.CR1

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * Class containing all the fields related to job
 * @author naveen
 * 
 */
@Entity
public class BatchJob implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Field(required = true)
    private Integer batchJobStatusId; // TODO int

    public Integer getBatchJobStatusId() {
        return batchJobStatusId;
    }

    public void setBatchJobStatusId(Integer batchJobStatusId) {
        this.batchJobStatusId = batchJobStatusId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Field(required = true)
    private String jobName;

    private String cronExpression;

    public BatchJob() {
    }

    public BatchJob(Integer batchJobStatusId, String jobName) {
        this.batchJobStatusId = batchJobStatusId;
        this.jobName = jobName;
    }

    public BatchJob(Integer batchJobStatusId, String jobName,
            String cronExpression) {
        this.batchJobStatusId = batchJobStatusId;
        this.jobName = jobName;
        this.cronExpression = cronExpression;
    }

}
