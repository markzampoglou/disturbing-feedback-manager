package gr.iti.mklab.reveal.dnn.api;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by marzampoglou on 6/17/16.
 */

@Entity
public class FeedbackObject {
    public @Id String id;
    public String sourceURL;
    public float score;
    public float desired_score;
}
