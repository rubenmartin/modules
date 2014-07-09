package org.motechproject.batch.mds;

// Generated Apr 11, 2014 10:49:43 AM by Hibernate Tools 3.4.0.CR1

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

/**
 * Class containing batch job configuration
 */
@Entity
public class BatchJobConfigurationHistory implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // THis should be int
    @Field(required = true)
    private String batchJobId;

    public String getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(String batchJobId) {
        this.batchJobId = batchJobId;
    }

    public String getJobConfiguration() {
        return jobConfiguration;
    }

    public void setJobConfiguration(String jobConfiguration) {
        this.jobConfiguration = jobConfiguration;
    }

    @Field(required = true)
    private String jobConfiguration;

    public BatchJobConfigurationHistory(String batchJobId,
            String jobConfiguration) {
        this.batchJobId = batchJobId;
        this.jobConfiguration = jobConfiguration;
    }

}
