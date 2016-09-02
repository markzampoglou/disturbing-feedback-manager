package gr.iti.mklab.reveal.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoClient;
import gr.iti.mklab.reveal.dnn.api.FeedbackObject;
import gr.iti.mklab.reveal.dnn.api.ObjectManagement;

import gr.iti.mklab.reveal.util.Configuration;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PreDestroy;
import java.security.MessageDigest;


@Controller
@RequestMapping("/disturbing_feedback")
public class DisturbingFeedbackCollector {
    private final String USER_AGENT = "Mozilla/5.0";

    public DisturbingFeedbackCollector() throws Exception {
        Configuration.load(getClass().getResourceAsStream("/remote.properties"));
    }

    // Suppress MongoDB logging
    static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);
    static {
        root.setLevel(Level.WARN);
    }

    @PreDestroy
    public void cleanUp() throws Exception {
        System.out.println("Spring Container destroy");
        //  MorphiaManager.tearDown();
    }

    ///////////////////////////////////////////////////////
    /////////// DOWNLOAD IMAGE     ////////////////////////
    ///////////////////////////////////////////////////////

    @RequestMapping(value = "/get_feedback", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String disturbing_feedback(@RequestParam(value = "url", required = true) String url, @RequestParam(value = "score", required = true) Float score, @RequestParam(value = "desired_score", required = true) Float desired_score){
        System.out.println("Received feedback on erroneous classification");
        MongoClient mongoclient = new MongoClient(Configuration.MONGO_HOST, 27017);
        Morphia morphia = new Morphia();
        morphia.map(FeedbackObject.class);
        Datastore ds = new Morphia().createDatastore(mongoclient, "DisturbingFeedback");
        ds.ensureCaps();
        try {
            String imgHash;
            byte[] urlBytes = url.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(urlBytes);
            byte[] digest=md.digest();
            imgHash = String.format("%032x", new java.math.BigInteger(1, digest));
            System.out.println("Hash : " + imgHash);

            FeedbackObject feedbackItem = ds.get(FeedbackObject.class, imgHash);
            if (feedbackItem == null) {
                feedbackItem = new FeedbackObject();
                ObjectManagement.downloadURL(url, Configuration.QUEUE_IMAGE_PATH , imgHash);
                feedbackItem.id=imgHash;
                feedbackItem.sourceURL=url;
                feedbackItem.score=score;
                feedbackItem.desired_score=desired_score;
                ds.save(feedbackItem);
                mongoclient.close();
                return "ok";
            } else
            {
                mongoclient.close();
                return "exists";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mongoclient.close();
            return "failed";
        }

    }

    ////////////////////////////////////////////////////////
    ///////// EXCEPTION HANDLING ///////////////////////////
    ///////////////////////////////////////////////////////

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RevealException.class)
    @ResponseBody
    public RevealException handleCustomException(RevealException ex) {
        return ex;
    }


    public static void main(String[] args) throws Exception {
    }
}